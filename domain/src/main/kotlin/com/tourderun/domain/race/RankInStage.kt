package com.tourderun.domain.race

import com.tourderun.domain.model.StageRanking
import com.tourderun.domain.model.TeamStanding

/**
 * 순위 정렬: 거리 내림차순 → 먼저 도달한 팀 → teamId(완전 동점일 때 순서 고정).
 * 순위 목록을 그릴 때도 이 순서를 쓴다(RaceRepository).
 */
val StandingOrder: Comparator<TeamStanding> =
    compareByDescending<TeamStanding> { it.distanceM }
        .thenBy { it.reachedAtMs }
        .thenBy { it.teamId }

/**
 * 같은 스테이지 팀 목록에서 내 팀의 순위 (RACE-01·02).
 *
 * @param teams 같은 스테이지의 팀 전부(내 팀 포함). 순서는 상관없다.
 * @return 내 팀이 [teams]에 없으면 null. 스테이지가 바뀌는 순간 리스너가 옛 스테이지 목록을 줄 수 있다.
 *   ahead/behind가 null이면 다른 스테이지의 가장 가까운 팀은 RaceRepository가 따로 찾는다.
 */
fun rankInStage(teams: List<TeamStanding>, myTeamId: String): StageRanking? {
    val sorted = teams.sortedWith(StandingOrder)
    val index = sorted.indexOfFirst { it.teamId == myTeamId }
    if (index < 0) return null
    return StageRanking(
        rank = index + 1,
        total = sorted.size,
        ahead = sorted.getOrNull(index - 1),
        behind = sorted.getOrNull(index + 1),
    )
}
