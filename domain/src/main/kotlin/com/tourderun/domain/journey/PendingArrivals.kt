package com.tourderun.domain.journey

import com.tourderun.domain.model.City
import com.tourderun.domain.model.Route

/**
 * 아직 연출(JRN-05 스테이지 클리어)을 못 본 도착.
 *
 * @property seen 내가 연출을 본 도착 수(`members.seenArrivals`)
 * @property current 지금 팀 도착 수([arrivals]). 확인하면 `seenArrivals`를 이 값으로 바꾼다
 * @property newCities 새로 도착한 도시, 도착 순서. 비어 있지 않다
 * @property stageCount 경로의 스테이지 수(완주 판정용). 시즌 1은 7
 */
data class PendingArrivals(
    val seen: Int,
    val current: Int,
    val newCities: List<City>,
    val stageCount: Int,
) {
    init {
        require(newCities.isNotEmpty()) { "새 도착이 없으면 PendingArrivals를 만들지 않는다" }
    }

    /** 연출에 보일 마지막 도시. 한 번에 두 도시를 지났으면 뒤 도시(예: 오산·천안 → 천안). */
    val lastCity: City get() = newCities.last()

    /** 이번 도착으로 완주(마지막 도시 도착). 완주 연출을 띄운다. */
    val isFinish: Boolean get() = current == stageCount
}

/**
 * 못 본 도착 (JRN-05). 내 [seenArrivals]와 팀 거리로 계산한다.
 *
 * - 예: (0, 18,999m) → null, (0, 19,000m) → [안양], (1, 85,000m) → [오산, 천안], (6, 350,000m) → [부산] 완주
 * - [seenArrivals]가 지금 도착 수보다 크거나 같으면 null(팀 거리는 줄지 않지만 값이 엇갈려 와도 연출을 띄우지 않는다)
 * - 완주 뒤에도 거리는 쌓이지만 도착 수는 7에서 멈추므로 다시 띄우지 않는다
 *
 * @return 새 도착이 없으면 null
 */
fun pendingArrivals(seenArrivals: Int, teamDistanceM: Long, route: Route): PendingArrivals? {
    val current = arrivals(teamDistanceM, route)
    val cities = newArrivals(seenArrivals, current, route)
    if (cities.isEmpty()) return null
    return PendingArrivals(seen = seenArrivals, current = current, newCities = cities, stageCount = route.stages.size)
}
