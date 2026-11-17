package com.tourderun.feature.stat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.Clock
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.stats.StatsRepository
import com.tourderun.core.data.stats.TopPercent
import com.tourderun.core.data.team.MyTeamSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update

/**
 * S16 주간 통계 (STAT-01·02).
 *
 * - 현재 시각은 구독을 시작할 때 [Clock]에서 한 번 읽는다. 화면을 떠났다가(5초 넘게) 다시 들어오면 새로 구독하므로
 *   이번 주·지난주 기간과 상위 %가 다시 계산된다.
 * - 상위 %는 온라인이고 이번 주 거리가 0보다 클 때만 조회하고, 이번 주 거리가 바뀌면 다시 조회한다.
 *   다시 조회하는 동안에는 직전 %를 그대로 보여 준다(깜빡임 방지).
 * - 불러오기에 실패하면 [WeeklyStatsUiState.Failed]를 보이고, [retry]로 처음부터 다시 구독한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeeklyStatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    myTeamSource: MyTeamSource,
    private val clock: Clock,
) : ViewModel() {

    /** 다시 시도할 때마다 1씩 늘린다. 값이 바뀌면 리스너를 새로 붙인다. */
    private val attempt = MutableStateFlow(0)

    val uiState: StateFlow<WeeklyStatsUiState> = attempt
        .flatMapLatest {
            myTeamSource.observeMyTeam()
                .distinctUntilChangedBy { it?.standing?.teamId }
                .flatMapLatest { team ->
                    if (team == null) flowOf(WeeklyStatsUiState.NoTeam) else weeklyState(team.standing.teamId)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyStatsUiState.Loading)

    /** 불러오기 실패 후 다시 시도. 다시 붙는 동안은 [WeeklyStatsUiState.Loading]. */
    fun retry() {
        attempt.update { it + 1 }
    }

    private fun weeklyState(teamId: String): Flow<WeeklyStatsUiState> = flow {
        emit(WeeklyStatsUiState.Loading)
        val nowMs = clock.nowMs()
        val period = weekPeriod(nowMs)
        var lastTop: TopFetch? = null
        emitAll(
            statsRepository.observeWeekly(teamId, nowMs).transformLatest { loaded ->
                if (loaded !is Loaded.Ready || !needsTopPercent(loaded)) {
                    emit(weeklyStatsState(loaded, top = null, period))
                    return@transformLatest
                }
                val weekly = loaded
                val thisWeekM = weekly.value.thisWeekM
                val known = lastTop
                if (known != null && known.thisWeekM == thisWeekM) {
                    emit(weeklyStatsContent(weekly, known.top, period))
                    return@transformLatest
                }
                emit(weeklyStatsContent(weekly, known?.top, period))
                val top = statsRepository.topPercent(teamId, nowMs)
                lastTop = TopFetch(thisWeekM, top)
                emit(weeklyStatsContent(weekly, top, period))
            },
        )
    }

    /** 어떤 이번 주 거리에서 조회한 상위 %인지. */
    private class TopFetch(val thisWeekM: Long, val top: TopPercent?)
}
