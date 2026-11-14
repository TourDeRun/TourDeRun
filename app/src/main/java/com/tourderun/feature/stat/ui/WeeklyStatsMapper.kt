package com.tourderun.feature.stat.ui

import com.tourderun.core.data.Cached
import com.tourderun.core.data.stats.TopPercent
import com.tourderun.core.data.stats.WeeklyStats
import com.tourderun.domain.stats.SEOUL
import com.tourderun.domain.stats.weekId
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * 주간 통계 → S16 상태.
 *
 * - 지난주 0km → [WeeklyChangeUi.NoLastWeek], 같으면 [WeeklyChangeUi.Same], 아니면 차이
 * - 상위 %: 이번 주 0km → [WeeklyRankUi.FirstRun], 오프라인이거나 [top]이 null → [WeeklyRankUi.Hidden]
 */
fun weeklyStatsContent(
    weekly: Cached<WeeklyStats>,
    top: TopPercent?,
    period: WeekPeriodUi,
): WeeklyStatsUiState.Content {
    val stats = weekly.value
    val change = when {
        stats.lastWeekM <= 0L -> WeeklyChangeUi.NoLastWeek
        stats.thisWeekM == stats.lastWeekM -> WeeklyChangeUi.Same
        else -> WeeklyChangeUi.Diff(stats.thisWeekM - stats.lastWeekM)
    }
    val rank = when {
        stats.thisWeekM <= 0L -> WeeklyRankUi.FirstRun
        weekly.isFromCache || top == null -> WeeklyRankUi.Hidden
        else -> WeeklyRankUi.Shown(top.percent, top.totalTeams, top.moreCount)
    }
    return WeeklyStatsUiState.Content(
        period = period,
        distanceM = stats.thisWeekM,
        runCount = stats.thisWeekRuns,
        lastWeekDistanceM = stats.lastWeekM,
        lastWeekRunCount = stats.lastWeekRuns,
        change = change,
        rank = rank,
        isOffline = weekly.isFromCache,
    )
}

/** 상위 %를 서버에 물어볼 상황인지. 오프라인이거나 이번 주 0km면 묻지 않는다(어차피 숨김). */
fun needsTopPercent(weekly: Cached<WeeklyStats>): Boolean =
    !weekly.isFromCache && weekly.value.thisWeekM > 0L

/** [nowMs]가 속한 주(월요일 0시 시작, 한국 시간). 2027-01-01(금) → 2026-W53, 12월 28일(월) ~ 1월 3일(일). */
fun weekPeriod(nowMs: Long, zone: ZoneId = SEOUL): WeekPeriodUi {
    val monday = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val sunday = monday.plusDays(6)
    return WeekPeriodUi(
        weekId = weekId(nowMs, zone),
        startMonth = monday.monthValue,
        startDay = monday.dayOfMonth,
        endMonth = sunday.monthValue,
        endDay = sunday.dayOfMonth,
    )
}
