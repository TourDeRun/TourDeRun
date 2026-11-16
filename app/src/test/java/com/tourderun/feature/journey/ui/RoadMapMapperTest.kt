package com.tourderun.feature.journey.ui

import com.tourderun.core.designsystem.component.TeamAppearance
import com.tourderun.core.ui.model.RivalTeamUi
import com.tourderun.domain.model.LatLngValue
import com.tourderun.domain.route.Season1Route
import com.tourderun.domain.route.routeFor
import org.junit.Assert.assertEquals
import org.junit.Test

class RoadMapMapperTest {
    private val route = routeFor(1)

    private fun point(city: com.tourderun.domain.model.City) = LatLngValue(city.lat, city.lng)

    @Test fun `스테이지 3 중간이면 지나온 길은 서울 안양 오산 우리 팀`() {
        val map = roadMapUi(route, 66_800, emptyList())
        assertEquals(4, map.passedPath.size)
        assertEquals(point(Season1Route.SEOUL), map.passedPath.first())
        assertEquals(point(Season1Route.OSAN), map.passedPath[2])
        assertEquals(map.ourPosition, map.passedPath.last())
    }

    @Test fun `남은 길은 우리 팀에서 시작해 부산에서 끝난다`() {
        val map = roadMapUi(route, 66_800, emptyList())
        assertEquals(map.ourPosition, map.remainingPath.first())
        assertEquals(point(Season1Route.CHEONAN), map.remainingPath[1])
        assertEquals(point(Season1Route.BUSAN), map.remainingPath.last())
        assertEquals(6, map.remainingPath.size)
    }

    @Test fun `도시 표시는 도착 다음 나중 순서`() {
        val kinds = roadMapUi(route, 66_800, emptyList()).cities.map { it.kind }
        assertEquals(
            listOf(CityPinKind.ARRIVED, CityPinKind.ARRIVED, CityPinKind.ARRIVED, CityPinKind.NEXT) +
                List(4) { CityPinKind.LATER },
            kinds,
        )
    }

    @Test fun `도시 누적 거리는 고정값`() {
        assertEquals(
            listOf(0, 19, 48, 85, 125, 181, 262, 350),
            roadMapUi(route, 0, emptyList()).cities.map { it.cumulativeKm },
        )
    }

    @Test fun `출발 전이면 서울에 있고 처음 범위는 서울 안양`() {
        val map = roadMapUi(route, 0, emptyList())
        assertEquals(point(Season1Route.SEOUL), map.ourPosition)
        assertEquals(listOf(point(Season1Route.SEOUL), point(Season1Route.ANYANG)), map.stageFocus)
        assertEquals(CityPinKind.NEXT, map.cities[1].kind)
    }

    @Test fun `19000m는 안양에 도착했고 처음 범위는 안양 오산`() {
        val map = roadMapUi(route, 19_000, emptyList())
        assertEquals(point(Season1Route.ANYANG), map.ourPosition)
        assertEquals(listOf(point(Season1Route.ANYANG), point(Season1Route.OSAN)), map.stageFocus)
    }

    @Test fun `완주하면 부산에 멈추고 모든 도시가 도착`() {
        val map = roadMapUi(route, 400_000, emptyList())
        assertEquals(point(Season1Route.BUSAN), map.ourPosition)
        assertEquals(List(8) { CityPinKind.ARRIVED }, map.cities.map { it.kind })
        assertEquals(1, map.remainingPath.size)
        assertEquals(listOf(point(Season1Route.DAEGU), point(Season1Route.BUSAN)), map.stageFocus)
    }

    @Test fun `다른 팀은 다른 스테이지에 있어도 자기 거리의 위치에 놓인다`() {
        // 9.5km = 스테이지 1(서울 → 안양 19km)의 절반
        val rival = RivalTeamUi("앞 팀", TeamAppearance(), 2, 9_500)
        val pin = roadMapUi(route, 66_800, listOf(rival)).rivals.single()
        assertEquals((Season1Route.SEOUL.lat + Season1Route.ANYANG.lat) / 2, pin.position.lat, 1e-9)
        assertEquals((Season1Route.SEOUL.lng + Season1Route.ANYANG.lng) / 2, pin.position.lng, 1e-9)
    }
}
