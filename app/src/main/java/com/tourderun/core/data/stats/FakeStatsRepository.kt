package com.tourderun.core.data.stats

import com.tourderun.core.data.Cached
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 화면 연결용 가짜 구현 (#9). Firestore 구현은 #12.
 * 기본값: 이번 주 42.5km·9회, 지난주 36.4km·7회, 상위 11%(전체 38팀 중, 더 달린 팀 3개).
 *
 * Hilt는 기본값으로 만든다. 다른 경우는 Preview·테스트에서 직접 만든다:
 * - 빈 주: `FakeStatsRepository(weekly = WeeklyStats.EMPTY)` → 상위 %도 null
 * - 오프라인: `FakeStatsRepository(DEFAULT_WEEKLY, topPercent = null, fromCache = true)`
 *
 * 가짜 구현은 teamId·nowMs를 보지 않는다(기간 계산은 [FirestoreStatsRepository]).
 */
@Singleton
class FakeStatsRepository(
    private val weekly: WeeklyStats,
    private val topPercent: TopPercent? = DEFAULT_TOP_PERCENT,
    private val fromCache: Boolean = false,
) : StatsRepository {

    @Inject constructor() : this(DEFAULT_WEEKLY)

    override fun observeWeekly(teamId: String, nowMs: Long): Flow<Cached<WeeklyStats>> =
        flowOf(Cached(weekly, isFromCache = fromCache))

    // 이번 주 0km면 숨김(null)은 실제 구현과 같게 맞춘다.
    override suspend fun topPercent(teamId: String, nowMs: Long): TopPercent? =
        if (weekly.thisWeekM <= 0) null else topPercent

    companion object {
        val DEFAULT_TOP_PERCENT = TopPercent(percent = 11, moreCount = 3, totalTeams = 38)

        val DEFAULT_WEEKLY = WeeklyStats(
            thisWeekM = 42_500,
            thisWeekRuns = 9,
            lastWeekM = 36_400,
            lastWeekRuns = 7,
            memberM = mapOf(
                "fake-uid-1" to 15_200,
                "fake-uid-2" to 12_100,
                "fake-uid-3" to 9_400,
                "fake-uid-4" to 5_800,
            ),
        )
    }
}
