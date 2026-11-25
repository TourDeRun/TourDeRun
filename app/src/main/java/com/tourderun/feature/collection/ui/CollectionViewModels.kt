package com.tourderun.feature.collection.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.card.CardRepository
import com.tourderun.core.data.card.OwnedCard
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.uid
import com.tourderun.core.data.team.MyTeam
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.core.navigation.CardDetailRoute
import com.tourderun.domain.route.routeFor
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** 팀이 없을 때 쓰는 시즌(경로 이름·거리만 필요하다). */
private const val DEFAULT_SEASON = 1

/** S17이 지금 무엇을 보여 줄지. */
sealed interface CollectionLoad {
    data object Loading : CollectionLoad
    data class Failed(val error: DataError) : CollectionLoad
    data class Content(val ui: CollectionUiState) : CollectionLoad
}

/** S18이 지금 무엇을 보여 줄지. */
sealed interface CardDetailLoad {
    data object Loading : CardDetailLoad
    data class Failed(val error: DataError) : CardDetailLoad

    /** 경로에 없는 도시 ID. 화면을 닫는다. */
    data object NotFound : CardDetailLoad

    data class Content(val ui: CardDetailUiState) : CardDetailLoad
}

/** 가진 카드 + 내 팀 → S17 상태. 카드를 못 읽으면 실패, 팀이 없으면 다음 카드 없이 보인다. */
internal fun collectionLoad(owned: Loaded<List<OwnedCard>>, team: MyTeam?): CollectionLoad = when (owned) {
    is Loaded.Failed -> CollectionLoad.Failed(owned.error)
    is Loaded.Ready -> CollectionLoad.Content(
        collectionUiState(routeFor(team?.season ?: DEFAULT_SEASON), owned.value, team?.standing?.distanceM),
    )
}

/** 가진 카드 + 내 팀 → S18 상태. */
internal fun cardDetailLoad(cityId: String, owned: Loaded<List<OwnedCard>>, team: MyTeam?): CardDetailLoad = when (owned) {
    is Loaded.Failed -> CardDetailLoad.Failed(owned.error)
    is Loaded.Ready -> cardDetailUiState(routeFor(team?.season ?: DEFAULT_SEASON), cityId, owned.value)
        ?.let { CardDetailLoad.Content(it) }
        ?: CardDetailLoad.NotFound
}

/** 로그인한 계정의 가진 카드. uid가 바뀌면 다시 구독한다. */
@OptIn(ExperimentalCoroutinesApi::class)
private fun ownedCards(session: SessionRepository, cardRepository: CardRepository): Flow<Loaded<List<OwnedCard>>> =
    session.state
        .map { it.uid }
        .distinctUntilChanged()
        .flatMapLatest { uid -> cardRepository.observeOwned(uid) }

/**
 * S17 지역 도감 (EXT-01). 가진 카드(`users.cards`)와 팀 거리(다음 카드까지 남은 거리)를 합친다.
 * 불러오지 못하면 [CollectionLoad.Failed], [retry]로 다시 구독한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CollectionViewModel @Inject constructor(
    session: SessionRepository,
    cardRepository: CardRepository,
    myTeamSource: MyTeamSource,
) : ViewModel() {

    /** 다시 시도할 때마다 1씩 늘린다. 값이 바뀌면 리스너를 새로 붙인다. */
    private val attempt = MutableStateFlow(0)

    val uiState: StateFlow<CollectionLoad> = attempt
        .flatMapLatest {
            combine(ownedCards(session, cardRepository), myTeamSource.observeMyTeam(), ::collectionLoad)
                .onStart { emit(CollectionLoad.Loading) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionLoad.Loading)

    /** 불러오기 실패 후 다시 시도. */
    fun retry() {
        attempt.update { it + 1 }
    }
}

/**
 * S18 지역 카드 상세 (EXT-01). 글 콘텐츠는 M-07, 받은 날짜·팀 이름은 가진 카드에서 읽는다.
 * 스테이지 클리어 연출에서 바로 들어와도 카드 쓰기(오프라인 대기)가 캐시에 먼저 반영돼 받은 줄이 보인다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    session: SessionRepository,
    cardRepository: CardRepository,
    myTeamSource: MyTeamSource,
) : ViewModel() {

    private val cityId = savedStateHandle.toRoute<CardDetailRoute>().cityId

    /** 다시 시도할 때마다 1씩 늘린다. 값이 바뀌면 리스너를 새로 붙인다. */
    private val attempt = MutableStateFlow(0)

    val uiState: StateFlow<CardDetailLoad> = attempt
        .flatMapLatest {
            combine(ownedCards(session, cardRepository), myTeamSource.observeMyTeam()) { owned, team ->
                cardDetailLoad(cityId, owned, team)
            }.onStart { emit(CardDetailLoad.Loading) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardDetailLoad.Loading)

    /** 불러오기 실패 후 다시 시도. */
    fun retry() {
        attempt.update { it + 1 }
    }
}
