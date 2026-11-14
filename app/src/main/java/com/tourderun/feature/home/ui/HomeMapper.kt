package com.tourderun.feature.home.ui

import com.tourderun.core.data.Cached
import com.tourderun.core.data.stats.TopPercent
import com.tourderun.core.data.stats.WeeklyStats
import com.tourderun.core.ui.model.MyRaceUi

/**
 * 이번 주 통계 → 홈 카드 값 (STAT-01·02).
 *
 * - 지난주 기록이 없으면 "지난주보다"를 보이지 않는다([WeeklySummaryUi.diffFromLastWeekM] = null)
 * - 상위 %는 오프라인이거나 이번 주 0km이거나 [top]이 없으면 숨긴다
 */
fun weeklySummaryUi(weekly: Cached<WeeklyStats>, top: TopPercent?): WeeklySummaryUi {
    val stats = weekly.value
    val showTop = top != null && !weekly.isFromCache && stats.thisWeekM > 0L
    return WeeklySummaryUi(
        distanceM = stats.thisWeekM,
        runCount = stats.thisWeekRuns.toInt(),
        diffFromLastWeekM = if (stats.lastWeekM > 0L) stats.thisWeekM - stats.lastWeekM else null,
        totalTeams = if (showTop) top?.totalTeams?.toInt() else null,
        topPercent = if (showTop) top?.percent else null,
    )
}

/**
 * 경쟁 상황 + 이번 주 요약 → S07 상태.
 *
 * TODO(TEAM-04): 팀원 목록(닉네임·기여 거리)은 `TeamRepository`(권민찬)가 생기면 채운다. 지금은 비어 있어 팀원 카드를 숨긴다.
 */
fun homeUiState(race: MyRaceUi, weekly: WeeklySummaryUi?): HomeUiState = HomeUiState(
    team = race.team,
    stage = race.stage,
    rank = race.rank,
    totalTeams = race.totalTeams,
    ahead = race.ahead,
    aheadIsOtherStage = race.aheadIsOtherStage,
    behind = race.behind,
    behindIsOtherStage = race.behindIsOtherStage,
    weekly = weekly,
    members = emptyList(),
    isOffline = race.isOffline,
)
