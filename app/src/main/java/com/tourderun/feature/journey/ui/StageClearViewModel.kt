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
import com.tourderun.core.data.journey.ArrivalRepository
import com.tourderun.core.data.journey.PendingSeen
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.uid
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
 * 스테이지 클리어 연출에 보일 못 본 도착과 확인 처리 (JRN-05). 화면 수명과 떨어져 있어 단위 테스트한다.
 *
 * - 로그인 uid로 [ArrivalRepository.observePending]을 구독한다. uid가 바뀌면 다시 구독하고, null이면 구독하지 않는다.
 * - [confirm]한 값은 저장소가 아직 같은 값을 내도 다시 보이지 않는다(쓰기가 실패해도 연출은 닫는다).
 *   다른 도착이 새로 오면 보인다.
 * - 저장소는 uid가 있을 때 처음 꺼낸다([Lazy]). 로그인 전에는 Firestore를 만들지 않는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StageClearController(
    session: SessionRepository,
    private val arrivals: Lazy<ArrivalRepository>,
) {
    private val confirmed = MutableStateFlow<PendingSeen?>(null)

    /** 지금 연출로 보일 못 본 도착. 없으면 null. */
    val visible: Flow<PendingSeen?> = combine(
        session.state.map { it.uid }.distinctUntilChanged().flatMapLatest { uid ->
            if (uid == null) flowOf(null) else arrivals.get().observePending(uid)
        },
        confirmed,
    ) { seen, done -> seen?.takeIf { it != done } }
        .distinctUntilChanged()

    /**
     * [seen]을 봤다고 쓴다. 지역 카드는 넘기지 않는다(#21). 같은 값으로 두 번 부르면 두 번째는 쓰지 않는다.
     *
     * @return 쓰기를 시작한 결과. [seen]이 null이거나 이미 확인한 값이면 null(쓰지 않음)
     */
    fun confirm(seen: PendingSeen?): Result<Unit>? {
        if (seen == null || seen == confirmed.value) return null
        confirmed.value = seen
        return arrivals.get().markSeen(seen)
    }
}

/**
 * 스테이지 클리어 연출 (JRN-05). 홈과 러닝 결과(S10) 위에 [StageClearHost]로 띄운다.
 * uid는 [SessionRepository]에서 읽는다. [uiState]가 null이면 연출 없음.
 */
@HiltViewModel
class StageClearViewModel @Inject constructor(
    session: SessionRepository,
    arrivals: Lazy<ArrivalRepository>,
) : ViewModel() {
    private val controller = StageClearController(session, arrivals)

    private val visible: StateFlow<PendingSeen?> =
        controller.visible.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<StageClearUiState?> = visible
        .map { seen -> seen?.let { stageClearUiState(it.pending, routeFor(it.season)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** "확인". 지금 보이는 도착을 봤다고 쓰고 연출을 닫는다. 쓰기를 시작하지 못하면 로그만 남긴다. */
    fun confirm() {
        controller.confirm(visible.value)?.onFailure { Log.w(TAG, "도착 확인 쓰기 시작 실패", it) }
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
 */
@Composable
fun StageClearHost(
    modifier: Modifier = Modifier,
    viewModel: StageClearViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState ?: return
    BackHandler(onBack = viewModel::confirm)
    StageClearScreen(
        uiState = state,
        // TODO(#21): 지역 카드 보기. 지금은 card가 항상 null이라 버튼이 보이지 않는다.
        onViewCard = {},
        onConfirm = viewModel::confirm,
        // 빈 pointerInput으로 이 영역의 터치를 받아 아래 화면이 눌리지 않게 한다.
        modifier = modifier.fillMaxSize().pointerInput(Unit) {},
    )
}
