package com.tourderun.feature.journey.ui

import com.tourderun.core.ui.model.RivalTeamUi
import com.tourderun.domain.journey.locate
import com.tourderun.domain.journey.mapPosition
import com.tourderun.domain.model.LatLngValue
import com.tourderun.domain.model.Route

/** 도시 표시 종류. */
enum class CityPinKind {
    /** 출발 도시와 이미 도착한 도시 */
    ARRIVED,

    /** 지금 스테이지의 도착 도시 */
    NEXT,

    /** 그 뒤 도시 */
    LATER,
}

/** 지도 위 도시 표시. [cumulativeKm] = 출발 도시부터 누적 거리(출발 도시는 0). */
data class CityPinUi(
    val name: String,
    val position: LatLngValue,
    val cumulativeKm: Int,
    val kind: CityPinKind,
)

/** 지도 위 다른 팀. 다른 팀은 이름·이미지·인원·거리만 보인다. */
data class RivalPinUi(val team: RivalTeamUi, val position: LatLngValue)

/**
 * S08 로드뷰 지도에 그릴 것 (JRN-04).
 *
 * @property passedPath 출발 도시 → 지나온 도시들 → 우리 팀 위치 (강조색 선)
 * @property remainingPath 우리 팀 위치 → 남은 도시들 (회색 선). 완주하면 점 하나뿐이라 그려지지 않는다
 * @property stageFocus 처음 화면에 맞출 범위: 지금 스테이지의 출발·도착 도시
 */
data class RoadMapUi(
    val cities: List<CityPinUi>,
    val passedPath: List<LatLngValue>,
    val remainingPath: List<LatLngValue>,
    val ourPosition: LatLngValue,
    val rivals: List<RivalPinUi>,
    val stageFocus: List<LatLngValue>,
)

/**
 * 팀 거리 → 지도에 그릴 값. 위치는 `locate` + `mapPosition`(도시 사이 직선 보간)을 그대로 쓴다.
 * 다른 팀은 어느 스테이지에 있든 같은 방법으로 놓는다(앞·뒤 팀이 다른 스테이지에 있는 경우).
 */
fun roadMapUi(route: Route, teamDistanceM: Long, rivals: List<RivalTeamUi>): RoadMapUi {
    val pos = locate(teamDistanceM, route)
    val our = mapPosition(pos, route)
    val cityPoints = route.cities.map { LatLngValue(it.lat, it.lng) }
    return RoadMapUi(
        cities = route.cities.mapIndexed { i, city ->
            CityPinUi(
                name = city.nameKo,
                position = cityPoints[i],
                cumulativeKm = if (i == 0) 0 else (route.cumulativeEndM[i - 1] / 1_000).toInt(),
                kind = when {
                    i <= pos.arrivals -> CityPinKind.ARRIVED
                    i == pos.arrivals + 1 -> CityPinKind.NEXT
                    else -> CityPinKind.LATER
                },
            )
        },
        passedPath = cityPoints.take(pos.arrivals + 1) + our,
        remainingPath = listOf(our) + cityPoints.drop(pos.arrivals + 1),
        ourPosition = our,
        rivals = rivals.map { RivalPinUi(it, mapPosition(locate(it.distanceM, route), route)) },
        stageFocus = listOf(cityPoints[pos.stageIndex], cityPoints[pos.stageIndex + 1]),
    )
}
