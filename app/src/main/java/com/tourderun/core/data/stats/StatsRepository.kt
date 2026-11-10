package com.tourderun.core.data.stats

import com.tourderun.core.data.Cached
import kotlinx.coroutines.flow.Flow

/**
 * 팀 주간 통계 (STAT-01·02). S07 홈 이번 주 카드, S16 주간 통계가 쓴다.
 *
 * 구현 규칙
 * - 주간은 한국 시간 월요일 0시 시작, ISO 주차 연도. 이번 주 = `weekId(nowMs)`, 지난주 = `weekId(nowMs - 7일)`.
 *   문서는 `teamStats/{teamId}_{periodId}`. 러닝은 **시작 시각**의 주에 쌓인다(REC-01).
 * - 현재 시각은 읽지 않고 [nowMs]로 받는다.
 * - 오류를 던지지 않는다. 연결이 끊기면 캐시 값을 [Cached.isFromCache] = true로 낸다.
 */
interface StatsRepository {
    /** 이번 주·지난주 팀 통계. 문서가 없는 주는 0으로 채운다. */
    fun observeWeekly(teamId: String, nowMs: Long): Flow<Cached<WeeklyStats>>

    /**
     * 이번 주 상위 % (STAT-02) = `topPercent(이번 주 우리 팀보다 더 달린 팀 수, 시즌 1 팀 전부)`.
     * 전체 팀 수에는 이번 주 0km 팀도 들어간다.
     *
     * @return 오프라인이면(count 집계는 서버가 필요) 또는 우리 팀이 이번 주 0km면 null → 화면에서 숨긴다
     */
    suspend fun topPercent(teamId: String, nowMs: Long): TopPercent?
}

/**
 * 이번 주 상위 % 결과. 화면 문구 "전체 [totalTeams]팀 중 상위 [percent]% · 더 달린 팀 [moreCount]개"에 쓴다.
 *
 * @property percent 1~100. `ceil((moreCount + 1) / totalTeams × 100)`
 * @property moreCount 이번 주 우리 팀보다 더 달린 팀 수
 * @property totalTeams 시즌 1 팀 전부(이번 주 0km 팀 포함)
 */
data class TopPercent(
    val percent: Int,
    val moreCount: Long,
    val totalTeams: Long,
)

/**
 * 팀 주간 통계. 거리는 정수 m.
 *
 * @property memberM 이번 주 팀원별 거리(uid → m). 나간 팀원 몫도 남아 있다("이전 팀원"으로 표시)
 */
data class WeeklyStats(
    val thisWeekM: Long,
    val thisWeekRuns: Long,
    val lastWeekM: Long,
    val lastWeekRuns: Long,
    val memberM: Map<String, Long>,
) {
    companion object {
        /** 문서가 없을 때. */
        val EMPTY = WeeklyStats(0, 0, 0, 0, emptyMap())
    }
}
