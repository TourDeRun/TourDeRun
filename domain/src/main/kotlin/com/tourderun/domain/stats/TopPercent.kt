package com.tourderun.domain.stats

/**
 * 이번 주 상위 % (STAT-02) = ceil((더 많이 달린 팀 수 + 1) / 전체 팀 수 × 100).
 * 부동소수 오차가 없도록 정수로 올림한다. 결과는 1~100.
 *
 * 우리 팀이 0km면 화면에서 숨기는 것은 호출하는 쪽이 정한다.
 *
 * @param moreCount 이번 주 우리 팀보다 더 많이 달린 팀 수
 * @param totalTeams 시즌 1 팀 전부(이번 주 0km 팀 포함)
 */
fun topPercent(moreCount: Long, totalTeams: Long): Int {
    require(totalTeams > 0) { "전체 팀 수는 1 이상이어야 한다: $totalTeams" }
    val numerator = (moreCount + 1) * 100
    val ceil = (numerator + totalTeams - 1) / totalTeams
    return ceil.coerceIn(1L, 100L).toInt()
}
