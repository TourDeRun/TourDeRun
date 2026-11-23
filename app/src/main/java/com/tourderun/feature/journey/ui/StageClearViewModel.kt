package com.tourderun.feature.journey.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.card.CardRepository
import com.tourderun.core.data.card.OwnedCard
import com.tourderun.core.data.journey.ArrivalRepository
import com.tourderun.core.data.journey.PendingSeen
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.uid
import com.tourderun.domain.card.newCardCityIds
import com.tourderun.domain.route.routeFor
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 연출에 보일 못 본 도착과, 확인할 때 같이 줄 지역 카드 (JRN-05, EXT-01).
 *
 * @property newCardCityIds 확인 배치에 같이 쓸 카드 도시 ID(도착 순서, 이미 가진 도시 제외).
 *   가진 카드를 읽지 못했으면(Failed) null: 이때는 확인을 쓰지 않는다([StageClearController.confirm])
 */
data class StageClearShown(
    val seen: PendingSeen,
    val newCardCityIds: List<String>?,
)

/**
 * 못 본 도착 + 가진 카드 → [StageClearShown]. 카드는 `:domain`의 [newCardCityIds]로 계산한다
 * (본 도착 수 → 지금 도착 수 사이의 새 도시 중 가진 것 제외). 서울 카드(처음 팀 소속)는 여기서 주지 않는다.
 */
internal fun stageClearShown(seen: PendingSeen, owned: Loaded<List<OwnedCard>>): StageClearShown = when (owned) {
    is Loaded.Failed -> StageClearShown(seen, newCardCityIds = null)
    is Loaded.Ready -> StageClearShown(
        seen = seen,
        newCardCityIds = newCardCityIds(
            seenArrivals = seen.pending.seen,
            currentArrivals = seen.pending.current,
            ownedCityIds = owned.value.mapTo(mutableSetOf()) { it.cityId },
            isFirstTeamJoin = false,
            route = routeFor(seen.season),
        ),
    )
}

/**
 * 스테이지 클리어 연출에 보일 못 본 도착과 확인 처리 (JRN-05, EXT-01). 화면 수명과 떨어져 있어 단위 테스트한다.
 *
 * - 로그인 uid로 [ArrivalRepository.observePending]을 구독한다. uid가 바뀌면 다시 구독하고, null이면 구독하지 않는다.
 * - 못 본 도착이 있을 때만 가진 카드([CardRepository.observeOwned])를 구독한다. **가진 카드의 첫 값이 올 때까지는
 *   연출을 띄우지 않는다**(users 문서는 세션 리스너가 이미 캐시에 두므로 보통 바로 온다).
 *   가진 카드를 못 읽었으면(Failed) 카드 없이 연출을 띄우고, 확인은 쓰지 않는다([confirm]).
 * - [confirm]한 도착은 저장소가 아직 같은 값을 내도 다시 보이지 않는다(쓰기가 실패해도 연출은 닫는다).
 *   다른 도착이 새로 오면 보인다.
 * - 저장소는 uid가 있을 때 처음 꺼낸다([Lazy]). 로그인 전에는 Firestore를 만들지 않는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StageClearController(
    session: SessionRepository,
    private val arrivals: Lazy<ArrivalRepository>,
    private val cards: Lazy<CardRepository>,
) {
    private val confirmed = MutableStateFlow<PendingSeen?>(null)

    /** 지금 연출로 보일 못 본 도착과 줄 카드. 없으면 null. */
    val visible: Flow<StageClearShown?> = combine(
        session.state.map { it.uid }.distinctUntilChanged().flatMapLatest { uid ->
            if (uid == null) flowOf(null) else shownFor(uid)
        },
        confirmed,
    ) { shown, done -> shown?.takeIf { it.seen != done } }
        .distinctUntilChanged()

    private fun shownFor(uid: String): Flow<StageClearShown?> =
        arrivals.get().observePending(uid).flatMapLatest { seen ->
            if (seen == null) flowOf(null)
            else cards.get().observeOwned(uid).map { owned -> stageClearShown(seen, owned) }
        }

    /**
     * [shown]을 봤다고 쓴다: `seenArrivals`와 새 카드를 같은 배치로. 같은 도착으로 두 번 부르면 두 번째는 쓰지 않는다.
     *
     * 가진 카드를 못 읽었으면([StageClearShown.newCardCityIds]가 null) 연출만 닫고 **쓰지 않는다**.
     * 카드 없이 `seenArrivals`만 올리면 이번 도시 카드를 영영 못 받고, 카드를 다 주면 이미 가진 카드의 첫 기록을
     * 덮어쓸 수 있어서다. 못 본 도착이 그대로 남으므로 다음에 가진 카드를 읽을 수 있을 때 연출이 다시 뜨고 그때 쓴다.
     *
     * @return 쓰기를 시작한 결과. [shown]이 null이거나, 이미 확인한 도착이거나, 가진 카드를 못 읽었으면 null(쓰지 않음)
     */
    fun confirm(shown: StageClearShown?): Result<Unit>? {
        if (shown == null || shown.seen == confirmed.value) return null
        confirmed.value = shown.seen
        val cardIds = shown.newCardCityIds ?: return null
        return arrivals.get().markSeen(shown.seen, cardIds)
    }
}

/**
 * 스테이지 클리어 연출 (JRN-05, EXT-01). 홈과 러닝 결과(S10) 위에 [StageClearHost]로 띄운다.
 * uid는 [SessionRepository]에서 읽는다. [uiState]가 null이면 연출 없음.
 */
@HiltViewModel
class StageClearViewModel @Inject constructor(
    session: SessionRepository,
    arrivals: Lazy<ArrivalRepository>,
    cards: Lazy<CardRepository>,
) : ViewModel() {
    private val controller = StageClearController(session, arrivals, cards)

    private val visible: StateFlow<StageClearShown?> =
        controller.visible.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<StageClearUiState?> = visible
        .map { shown ->
            shown?.let { stageClearUiState(it.seen.pending, routeFor(it.seen.season), it.newCardCityIds) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** "확인". 지금 보이는 도착을 봤다고 쓰고(새 카드 포함) 연출을 닫는다. 쓰기를 시작하지 못하면 로그만 남긴다. */
    fun confirm() {
        val shown = visible.value
        val result = controller.confirm(shown)
        if (result == null && shown != null && shown.newCardCityIds == null) {
            Log.w(TAG, "가진 카드를 못 읽어 도착 확인을 쓰지 않음(다음에 연출이 다시 뜬다)")
        }
        result?.onFailure { Log.w(TAG, "도착 확인 쓰기 시작 실패", it) }
    }

    /**
     * "카드 보기". [confirm]과 같이 확인·카드를 먼저 쓰고, 연출에 보이던 카드의 도시 ID를 돌려준다(S18로 이동).
     * 보이던 카드가 없으면 null.
     */
    fun viewCard(): String? {
        val cityId = visible.value?.newCardCityIds?.lastOrNull()
        confirm()
        return cityId
    }

    private companion object {
        const val TAG = "TDR/journey"
    }
}

/**
 * 못 본 도착이 있으면 [StageClearScreen](전병국)을 화면 위에 덮어 그린다. 없으면 아무것도 그리지 않는다.
 *
 * - 내비게이션 목적지(홈·결과 화면) 안에서 화면 다음 줄에 둔다. 목적지 안의 형제라 화면 위에 겹쳐 그려진다.
 * - 복구 안내(RunRecoveryHost)는 다이얼로그 창이라 항상 이 연출 위에 뜬다. 복구를 먼저 고르고, 닫으면 연출이 보인다.
 * - 뒤로 가기는 "확인"과 같다(연출을 닫고 본 것으로 기록). 아래 화면으로 터치가 넘어가지 않게 막는다.
 * - "카드 보기"는 확인과 같이 쓴 뒤 [onViewCard]에 카드 도시 ID를 넘긴다(S18 카드 상세로 이동).
 */
@Composable
fun StageClearHost(
    modifier: Modifier = Modifier,
    onViewCard: (cityId: String) -> Unit = {},
    viewModel: StageClearViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState ?: return
    BackHandler(onBack = viewModel::confirm)
    StageClearScreen(
        uiState = state,
        onViewCard = { viewModel.viewCard()?.let(onViewCard) },
        onConfirm = viewModel::confirm,
        // 빈 pointerInput으로 이 영역의 터치를 받아 아래 화면이 눌리지 않게 한다.
        modifier = modifier.fillMaxSize().pointerInput(Unit) {},
    )
}
