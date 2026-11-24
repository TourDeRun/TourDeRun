package com.tourderun.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.Clock
import com.tourderun.core.data.race.MyRace
import com.tourderun.core.data.race.RaceRepository
import com.tourderun.core.data.race.observeMyRace
import com.tourderun.core.data.stats.StatsRepository
import com.tourderun.core.data.stats.TopPercent
import com.tourderun.core.data.stats.WeeklyStats
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.core.ui.model.MyRaceUi
import com.tourderun.core.ui.model.myRaceUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update

/** S07이 지금 무엇을 보여 줄지. */
sealed interface HomeLoad {
    /** 받기 전이거나 팀이 없다(바탕만). */
    data object Loading : HomeLoad

    /** 경쟁 상황 리스너가 첫 값 전에 실패했다. 화면 전체를 실패 + 다시 시도로. */
    data class Failed(val error: DataError) : HomeLoad

    data class Content(val ui: HomeUiState) : HomeLoad
}

/** 경쟁 상황 쪽 상태. [HomeLoad]와 같지만 이번 주 카드를 합치기 전 값이다. */
internal sealed interface HomeRaceLoad {
    data object Loading : HomeRaceLoad
    data class Failed(val error: DataError) : HomeRaceLoad
    data class Content(val race: MyRaceUi) : HomeRaceLoad
}

/**
 * 리스너 값 → 홈 경쟁 상황 상태.
 * 팀이 없으면 [HomeRaceLoad.Loading], 첫 값 전 실패면 [HomeRaceLoad.Failed],
 * 스테이지 전환 중(순위 없음)이면 직전 내용을 그대로 둔다(직전이 없으면 Loading).
 */
internal fun nextHomeRace(previous: HomeRaceLoad, myRace: MyRace?): HomeRaceLoad {
    if (myRace == null) return HomeRaceLoad.Loading
    val race = myRace.race
    if (race is Loaded.Failed) return HomeRaceLoad.Failed(race.error)
    return myRaceUi(myRace)?.let { HomeRaceLoad.Content(it) }
        ?: previous as? HomeRaceLoad.Content
        ?: HomeRaceLoad.Loading
}

/** 이번 주 카드 쪽 상태. */
internal sealed interface WeeklyLoad {
    /** 받기 전(카드를 숨긴다). */
    data object Loading : WeeklyLoad
    data class Failed(val error: DataError) : WeeklyLoad
    data class Content(val ui: WeeklySummaryUi) : WeeklyLoad
}

/** 경쟁 상황 + 이번 주 → S07 상태. 이번 주만 실패하면 홈은 그대로 보이고 이번 주 자리에 실패 카드가 뜬다. */
internal fun homeLoad(race: HomeRaceLoad, weekly: WeeklyLoad): HomeLoad = when (race) {
    HomeRaceLoad.Loading -> HomeLoad.Loading
    is HomeRaceLoad.Failed -> HomeLoad.Failed(race.error)
    is HomeRaceLoad.Content -> HomeLoad.Content(
        homeUiState(race.race, (weekly as? WeeklyLoad.Content)?.ui)
            .copy(weeklyError = (weekly as? WeeklyLoad.Failed)?.error),
    )
}

/**
 * S07 홈 (JRN-03). 우리 팀 경쟁 상황(RACE-01·02)과 이번 주 통계(STAT-01·02)를 합친다.
 *
 * - [HomeLoad.Loading]이면 아직 못 받았거나 팀이 없는 것이다(화면은 비워 둔다).
 * - 스테이지가 바뀌는 순간에는 순위가 잠깐 비므로 직전 화면을 그대로 둔다.
 * - 경쟁 상황을 못 받으면 [HomeLoad.Failed], [retry]로 처음부터 다시 구독한다.
 * - 이번 주 카드는 통계를 받기 전까지 숨긴다. 통계가 늦어도 홈의 나머지는 먼저 보인다.
 *   통계만 실패하면 그 자리에 실패 카드를 두고 [retryWeekly]로 통계만 다시 구독한다(홈 나머지는 그대로).
 * - 현재 시각은 구독을 시작할 때 한 번 읽는다. 화면을 5초 넘게 떠났다 돌아오면 새로 구독해 주가 바뀐 것을 반영한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    raceRepository: RaceRepository,
    private val statsRepository: StatsRepository,
    myTeamSource: MyTeamSource,
    private val clock: Clock,
) : ViewModel() {

    /** 다시 시도할 때마다 1씩 늘린다. 값이 바뀌면 리스너를 새로 붙인다. */
    private val raceAttempt = MutableStateFlow(0)
    private val weeklyAttempt = MutableStateFlow(0)

    private val race: Flow<HomeRaceLoad> = raceAttempt.flatMapLatest {
        observeMyRace(myTeamSource, raceRepository).scan<_, HomeRaceLoad>(HomeRaceLoad.Loading, ::nextHomeRace)
    }

    private val weekly: Flow<WeeklyLoad> = weeklyAttempt.flatMapLatest {
        myTeamSource.observeMyTeam()
            .map { it?.standing?.teamId }
            .distinctUntilChanged()
            .flatMapLatest { teamId -> if (teamId == null) flowOf(WeeklyLoad.Loading) else weeklySummary(teamId) }
            .onStart { emit(WeeklyLoad.Loading) }
    }

    val uiState: StateFlow<HomeLoad> = combine(race, weekly, ::homeLoad)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeLoad.Loading)

    /** 경쟁 상황 불러오기 실패 후 다시 시도. 이번 주 통계도 같이 다시 구독한다. */
    fun retry() {
        raceAttempt.update { it + 1 }
        weeklyAttempt.update { it + 1 }
    }

    /** 이번 주 통계만 다시 시도. */
    fun retryWeekly() {
        weeklyAttempt.update { it + 1 }
    }

    /**
     * 이번 주 요약. 상위 %는 온라인이고 이번 주 거리가 0보다 클 때만 묻고, 이번 주 거리가 바뀌면 다시 묻는다.
     * 다시 묻는 동안에는 직전 %를 그대로 보여 준다.
     */
    private fun weeklySummary(teamId: String): Flow<WeeklyLoad> = flow {
        val nowMs = clock.nowMs()
        var lastTop: TopFetch? = null
        emitAll(
            statsRepository.observeWeekly(teamId, nowMs).transformLatest { loaded: Loaded<WeeklyStats> ->
                val weekly = when (loaded) {
                    is Loaded.Failed -> {
                        emit(WeeklyLoad.Failed(loaded.error))
                        return@transformLatest
                    }
                    is Loaded.Ready -> loaded
                }
                if (weekly.isFromCache || weekly.value.thisWeekM <= 0L) {
                    emit(WeeklyLoad.Content(weeklySummaryUi(weekly, top = null)))
                    return@transformLatest
                }
                val known = lastTop
                emit(WeeklyLoad.Content(weeklySummaryUi(weekly, known?.top)))
                if (known == null || known.thisWeekM != weekly.value.thisWeekM) {
                    val top = statsRepository.topPercent(teamId, nowMs)
                    lastTop = TopFetch(weekly.value.thisWeekM, top)
                    emit(WeeklyLoad.Content(weeklySummaryUi(weekly, top)))
                }
            },
        )
    }

    /** 어떤 이번 주 거리에서 받은 상위 %인지. */
    private class TopFetch(val thisWeekM: Long, val top: TopPercent?)
}
