package com.tourderun.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.Clock
import com.tourderun.core.data.race.RaceRepository
import com.tourderun.core.data.race.observeMyRace
import com.tourderun.core.data.stats.StatsRepository
import com.tourderun.core.data.stats.TopPercent
import com.tourderun.core.data.stats.WeeklyStats
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.core.data.team.TeamRepository
import com.tourderun.core.ui.model.MemberUi
import com.tourderun.core.ui.model.teamMembersUi
import com.tourderun.core.ui.model.MyRaceUi
import com.tourderun.core.ui.model.myRaceUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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

/**
 * S07 홈 (JRN-03). 우리 팀 경쟁 상황(RACE-01·02)과 이번 주 통계(STAT-01·02)를 합친다.
 *
 * - 상태가 null이면 아직 못 받았거나 팀이 없는 것이다(화면은 비워 둔다).
 * - 스테이지가 바뀌는 순간에는 순위가 잠깐 비므로 직전 화면을 그대로 둔다.
 * - 이번 주 카드는 통계를 받기 전까지 숨긴다. 통계가 늦어도 홈의 나머지는 먼저 보인다.
 * - 현재 시각은 구독을 시작할 때 한 번 읽는다. 화면을 5초 넘게 떠났다 돌아오면 새로 구독해 주가 바뀐 것을 반영한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    raceRepository: RaceRepository,
    private val statsRepository: StatsRepository,
    myTeamSource: MyTeamSource,
    private val clock: Clock,
    private val teamRepository: TeamRepository,
) : ViewModel() {

    private val race: Flow<MyRaceUi?> = observeMyRace(myTeamSource, raceRepository)
        .scan<_, MyRaceUi?>(null) { previous, myRace ->
            // 팀이 없으면 비우고, 스테이지 전환 중(순위 없음)이면 직전 값을 유지한다.
            when {
                myRace == null -> null
                // TODO(JRN-03): 불러오기 실패 화면. 지금은 못 받았을 때처럼 비워 둔다.
                myRace.race is Loaded.Failed -> null
                else -> myRaceUi(myRace) ?: previous
            }
        }

    private val weekly: Flow<WeeklySummaryUi?> = myTeamSource.observeMyTeam()
        .map { it?.standing?.teamId }
        .distinctUntilChanged()
        .flatMapLatest { teamId -> if (teamId == null) flowOf(null) else weeklySummary(teamId) }
        .onStart { emit(null) }

    /**
     * 팀원 카드(TEAM-04). 팀원 문서 + 이번 주 거리(MVP).
     * 리스너는 팀 ID로만 붙이고(팀 거리가 바뀌어도 끊지 않는다), 팀 거리는 combine으로 받아 비율만 다시 계산한다(#136 리뷰).
     * 불러오는 중·실패면 빈 목록 → 카드를 숨긴다.
     */
    private val members: Flow<List<MemberUi>> = myTeamSource.observeMyTeam()
        .map { it?.standing?.teamId }
        .distinctUntilChanged()
        .flatMapLatest<String?, List<MemberUi>> { teamId ->
            if (teamId == null) {
                flowOf(emptyList())
            } else {
                combine(
                    teamRepository.observeMembers(teamId),
                    statsRepository.observeWeekly(teamId, clock.nowMs()),
                    myTeamSource.observeMyTeam().map { it?.standing?.distanceM ?: 0L }.distinctUntilChanged(),
                ) { members, weekly, teamDistanceM ->
                    val list = (members as? Loaded.Ready)?.value ?: return@combine emptyList()
                    val weeklyMemberM = (weekly as? Loaded.Ready)?.value?.memberM.orEmpty()
                    teamMembersUi(teamDistanceM, list, weeklyMemberM).members
                }
            }
        }
        .onStart { emit(emptyList()) }

    val uiState: StateFlow<HomeUiState?> = combine(race, weekly, members) { race, weekly, members ->
        race?.let { homeUiState(it, weekly, members) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * 이번 주 요약. 상위 %는 온라인이고 이번 주 거리가 0보다 클 때만 묻고, 이번 주 거리가 바뀌면 다시 묻는다.
     * 다시 묻는 동안에는 직전 %를 그대로 보여 준다.
     */
    private fun weeklySummary(teamId: String): Flow<WeeklySummaryUi?> = flow {
        val nowMs = clock.nowMs()
        var lastTop: TopFetch? = null
        emitAll(
            statsRepository.observeWeekly(teamId, nowMs).transformLatest { loaded: Loaded<WeeklyStats> ->
                // TODO(JRN-03): 불러오기 실패 표시. 지금은 받기 전처럼 이번 주 카드를 숨긴다.
                if (loaded !is Loaded.Ready) {
                    emit(null)
                    return@transformLatest
                }
                val weekly = loaded
                if (weekly.isFromCache || weekly.value.thisWeekM <= 0L) {
                    emit(weeklySummaryUi(weekly, top = null))
                    return@transformLatest
                }
                val known = lastTop
                emit(weeklySummaryUi(weekly, known?.top))
                if (known == null || known.thisWeekM != weekly.value.thisWeekM) {
                    val top = statsRepository.topPercent(teamId, nowMs)
                    lastTop = TopFetch(weekly.value.thisWeekM, top)
                    emit(weeklySummaryUi(weekly, top))
                }
            },
        )
    }

    /** 어떤 이번 주 거리에서 받은 상위 %인지. */
    private class TopFetch(val thisWeekM: Long, val top: TopPercent?)
}
