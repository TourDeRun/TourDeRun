package com.tourderun.feature.stat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.Clock
import com.tourderun.core.data.stats.StatsRepository
import com.tourderun.core.data.stats.TopPercent
import com.tourderun.feature.stat.data.MyTeamSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

/**
 * S16 주간 통계 (STAT-01·02).
 *
 * - 현재 시각은 구독을 시작할 때 [Clock]에서 한 번 읽는다. 화면을 떠났다가(5초 넘게) 다시 들어오면 새로 구독하므로
 *   이번 주·지난주 기간과 상위 %가 다시 계산된다.
 * - 상위 %는 온라인이고 이번 주 거리가 0보다 클 때만 조회하고, 이번 주 거리가 바뀌면 다시 조회한다.
 *   다시 조회하는 동안에는 직전 %를 그대로 보여 준다(깜빡임 방지).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeeklyStatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    myTeamSource: MyTeamSource,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<WeeklyStatsUiState> = myTeamSource.observeMyTeam()
        .distinctUntilChangedBy { it?.teamId }
        .flatMapLatest { team ->
            if (team == null) flowOf(WeeklyStatsUiState.NoTeam) else weeklyState(team.teamId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyStatsUiState.Loading)

    private fun weeklyState(teamId: String): Flow<WeeklyStatsUiState> = flow {
        val nowMs = clock.nowMs()
        val period = weekPeriod(nowMs)
        var lastTop: TopFetch? = null
        emitAll(
            statsRepository.observeWeekly(teamId, nowMs).transformLatest { weekly ->
                if (!needsTopPercent(weekly)) {
                    emit(weeklyStatsContent(weekly, top = null, period))
                    return@transformLatest
                }
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
