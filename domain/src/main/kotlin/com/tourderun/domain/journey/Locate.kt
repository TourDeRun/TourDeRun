package com.tourderun.domain.journey

import com.tourderun.domain.model.City
import com.tourderun.domain.model.LatLngValue
import com.tourderun.domain.model.Route
import com.tourderun.domain.model.TeamPosition

/**
 * 도착 수 (JRN-02): 팀 거리 이하인 누적 거리를 가진 도시 수, 출발 도시 제외.
 * 18_999 → 0, 19_000 → 1, 350_000 이상 → 7.
 */
fun arrivals(totalM: Long, route: Route): Int =
    route.cumulativeEndM.count { it <= totalM }

/**
 * 팀 거리 → 현재 위치 (JRN-02). 구간 경계는 "시작 이상, 끝 미만".
 * 음수는 0으로 본다. 완주 후에도 거리는 늘 수 있지만 위치는 마지막 도시에 멈춘다.
 */
fun locate(totalM: Long, route: Route): TeamPosition {
    val m = totalM.coerceAtLeast(0L)
    val arrived = arrivals(m, route)
    if (arrived >= route.stages.size) {
        return TeamPosition(
            stageIndex = route.stages.lastIndex,
            progress = 1.0,
            toNextM = 0L,
            arrivals = arrived,
            finished = true,
        )
    }
    val stageIndex = arrived
    val start = route.stageStartM(stageIndex)
    val length = route.stages[stageIndex].lengthM
    val inStage = m - start
    return TeamPosition(
        stageIndex = stageIndex,
        progress = inStage.toDouble() / length,
        toNextM = length - inStage,
        arrivals = arrived,
        finished = false,
    )
}

/**
 * 새로 도착한 도시 목록 (JRN-05 클리어 연출, EXT-01 카드 공용).
 * seen = 내가 연출을 본 도착 수(members.seenArrivals), current = 지금 도착 수.
 * 예: (0, 1) → [안양], (1, 3) → [오산, 천안], (3, 3) → [], (6, 7) → [부산].
 */
fun newArrivals(seen: Int, current: Int, route: Route): List<City> {
    val max = route.stages.size
    val from = seen.coerceIn(0, max)
    val to = current.coerceIn(0, max)
    if (to <= from) return emptyList()
    // cities[i] = i번째 도착 도시 (cities[0]은 출발 도시)
    return route.cities.subList(from + 1, to + 1)
}

/** 지도 위 팀 좌표 (JRN-02): 현재 스테이지 출발·도착 도시를 진행률만큼 선형 보간. */
fun mapPosition(pos: TeamPosition, route: Route): LatLngValue {
    if (pos.finished) {
        val last = route.cities.last()
        return LatLngValue(last.lat, last.lng)
    }
    val stage = route.stages[pos.stageIndex]
    val t = pos.progress.coerceIn(0.0, 1.0)
    return LatLngValue(
        lat = stage.from.lat + (stage.to.lat - stage.from.lat) * t,
        lng = stage.from.lng + (stage.to.lng - stage.from.lng) * t,
    )
}
