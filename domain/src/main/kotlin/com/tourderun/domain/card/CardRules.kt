package com.tourderun.domain.card

import com.tourderun.domain.journey.newArrivals
import com.tourderun.domain.model.Route

/**
 * 이번에 새로 받을 지역 카드의 도시 ID 목록 (EXT-01). 순서: 출발 도시 → 도착 순서.
 *
 * - 도착 도시 카드: `newArrivals(seenArrivals, currentArrivals)`로 새로 도착한 도시
 * - 출발 도시(서울) 카드: 처음 팀에 소속될 때만([isFirstTeamJoin])
 * - 이미 가진 카드([ownedCityIds])는 다시 주지 않는다(첫 기록 유지, 팀을 옮겨 다시 도착해도 그대로)
 * - 가입 전에 팀이 지난 도시는 카드 대상이 아니다. `members.seenArrivals` 초깃값으로 지킨다:
 *   팀 만들기(TEAM-01)는 0, 팀 참여(TEAM-03)는 그 시점 팀 도착 수. 그래서 참여 순간에는 서울 카드만 생긴다.
 *
 * `members.seenArrivals` 갱신과 `users.cards` 쓰기는 이 결과로 같은 배치에서 한다.
 *
 * @param seenArrivals 내가 연출을 본 도착 수(`members.seenArrivals`). 팀 만들기·참여 때는 위 초깃값
 * @param currentArrivals 지금 팀의 도착 수(`arrivals(팀 거리)`)
 * @param ownedCityIds 이미 가진 카드(`users.cards`의 키)
 * @param isFirstTeamJoin 처음으로 팀에 소속되는 순간(팀 만들기·참여 배치)이면 true
 */
fun newCardCityIds(
    seenArrivals: Int,
    currentArrivals: Int,
    ownedCityIds: Set<String>,
    isFirstTeamJoin: Boolean,
    route: Route,
): List<String> {
    val start = if (isFirstTeamJoin) listOf(route.cities.first().id) else emptyList()
    val arrived = newArrivals(seenArrivals, currentArrivals, route).map { it.id }
    return (start + arrived).filterNot { it in ownedCityIds }
}
