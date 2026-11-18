package com.tourderun.domain.team

import kotlin.math.floor

/**
 * 팀 거리 중 한 사람(또는 이전 팀원 묶음)의 몫 (EXT-03).
 *
 * @property uid 팀원 uid. null이면 나간 팀원들의 거리를 묶은 "이전 팀원" 줄 (M-02 기본안: 팀 거리는 줄지 않는다)
 * @property distanceM 기여 거리 (정수 m)
 * @property percent 0~100. 모든 줄을 더하면 정확히 100 (팀 거리가 0이면 모두 0)
 */
data class ContributionShare(
    val uid: String?,
    val distanceM: Long,
    val percent: Int,
)

/**
 * 팀원별 기여 비율 (EXT-03, S14 막대).
 *
 * - 현재 팀원은 기여 거리 내림차순(같으면 uid 순), "이전 팀원" 줄은 맨 끝에 하나.
 * - 이전 팀원 몫 = 팀 거리 − 현재 팀원 기여 합. 0 이하면 줄을 만들지 않는다.
 * - 퍼센트는 버림한 뒤 모자란 만큼을 소수점 아래가 큰 줄부터 1씩 더한다(최대 나머지 방식). 그래서 합이 항상 100이다.
 *
 * @param teamDistanceM `teams.distanceM`
 * @param contributionM 현재 팀원 uid → `members.contributionM`
 */
fun contributionShares(teamDistanceM: Long, contributionM: Map<String, Long>): List<ContributionShare> {
    val current = contributionM.entries
        .map { (uid, m) -> ContributionShare(uid, m.coerceAtLeast(0), percent = 0) }
        .sortedWith(compareByDescending<ContributionShare> { it.distanceM }.thenBy { it.uid })
    val formerM = teamDistanceM - current.sumOf { it.distanceM }
    val rows = if (formerM > 0) current + ContributionShare(uid = null, distanceM = formerM, percent = 0) else current

    val total = rows.sumOf { it.distanceM }
    if (total == 0L) return rows

    val exact = rows.map { it.distanceM * 100.0 / total }
    val percents = exact.map { floor(it).toInt() }.toMutableList()
    var left = 100 - percents.sum()
    // 소수점 아래가 큰 줄부터. 같으면 앞줄(기여가 큰 사람)부터
    for (i in exact.indices.sortedByDescending { exact[it] - floor(exact[it]) }) {
        if (left == 0) break
        percents[i] += 1
        left -= 1
    }
    return rows.mapIndexed { i, row -> row.copy(percent = percents[i]) }
}

/**
 * 이번 주 MVP (EXT-03). 현재 팀원 중 이번 주 가장 많이 달린 사람들.
 *
 * - 동률이면 모두 MVP.
 * - 모두 0km면 없음 (빈 Set).
 * - 1인 팀이면 없음 (경쟁할 사람이 없다).
 * - 이번 주에 나간 팀원의 거리는 `memberM`에 남아 있어도 대상이 아니다.
 *
 * @param weeklyMemberM 이번 주 `teamStats.memberM` (uid → 거리). 이번 주 안 달린 팀원은 키가 없다(0으로 본다)
 * @param currentMembers 지금 팀원 uid
 */
fun weeklyMvps(weeklyMemberM: Map<String, Long>, currentMembers: Set<String>): Set<String> {
    if (currentMembers.size < 2) return emptySet()

    val weekly = currentMembers.associateWith { uid -> weeklyMemberM[uid] ?: 0L }

    val best = weekly.values.max()
    if (best <= 0L) return emptySet()

    return weekly.filter { it.value == best }.keys
}
