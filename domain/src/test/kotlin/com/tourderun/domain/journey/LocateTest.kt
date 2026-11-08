package com.tourderun.domain.journey

import com.tourderun.domain.route.Season1Route
import com.tourderun.domain.route.routeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteTest {
    private val route = routeFor(1)

    @Test fun `시즌 1은 도시 8개 스테이지 7개 350km`() {
        assertEquals(8, route.cities.size)
        assertEquals(7, route.stages.size)
        assertEquals(350_000L, route.totalM)
    }

    @Test fun `누적 거리는 19 48 85 125 181 262 350km`() {
        assertEquals(
            listOf(19_000L, 48_000L, 85_000L, 125_000L, 181_000L, 262_000L, 350_000L),
            route.cumulativeEndM,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `없는 시즌은 예외`() {
        routeFor(2)
    }
}

class LocateTest {
    private val route = routeFor(1)

    @Test fun `0m는 스테이지 1의 시작`() {
        val p = locate(0, route)
        assertEquals(0, p.stageIndex)
        assertEquals(0.0, p.progress, 1e-9)
        assertEquals(19_000L, p.toNextM)
        assertEquals(0, p.arrivals)
        assertFalse(p.finished)
    }

    @Test fun `음수는 0m로 본다`() {
        assertEquals(locate(0, route), locate(-500, route))
    }

    @Test fun `18999m는 스테이지 1의 끝`() {
        val p = locate(18_999, route)
        assertEquals(0, p.stageIndex)
        assertEquals(1L, p.toNextM)
        assertEquals(0, p.arrivals)
    }

    @Test fun `19000m는 스테이지 2의 시작`() {
        val p = locate(19_000, route)
        assertEquals(1, p.stageIndex)
        assertEquals(0.0, p.progress, 1e-9)
        assertEquals(29_000L, p.toNextM)
        assertEquals(1, p.arrivals)
    }

    @Test fun `66500m는 스테이지 3 진행률 절반`() {
        // 스테이지 3: 48km ~ 85km (37km), 절반은 66.5km
        val p = locate(66_500, route)
        assertEquals(2, p.stageIndex)
        assertEquals(0.5, p.progress, 1e-9)
        assertEquals(18_500L, p.toNextM)
    }

    @Test fun `349999m는 마지막 스테이지 진행 중`() {
        val p = locate(349_999, route)
        assertEquals(6, p.stageIndex)
        assertEquals(6, p.arrivals)
        assertFalse(p.finished)
    }

    @Test fun `350000m 이상은 완주`() {
        listOf(350_000L, 400_000L).forEach {
            val p = locate(it, route)
            assertTrue(p.finished)
            assertEquals(6, p.stageIndex)
            assertEquals(1.0, p.progress, 1e-9)
            assertEquals(0L, p.toNextM)
            assertEquals(7, p.arrivals)
        }
    }
}

class ArrivalsTest {
    private val route = routeFor(1)

    @Test fun `도착 수는 18999m 0 19000m 1 350000m 7`() {
        assertEquals(0, arrivals(18_999, route))
        assertEquals(1, arrivals(19_000, route))
        assertEquals(7, arrivals(350_000, route))
    }

    @Test fun `새 도착 0에서 1은 안양`() {
        assertEquals(listOf("anyang"), newArrivals(0, 1, route).map { it.id })
    }

    @Test fun `새 도착 1에서 3은 오산 천안`() {
        assertEquals(listOf("osan", "cheonan"), newArrivals(1, 3, route).map { it.id })
    }

    @Test fun `새 도착 3에서 3은 없음`() {
        assertTrue(newArrivals(3, 3, route).isEmpty())
    }

    @Test fun `새 도착 6에서 7은 부산`() {
        assertEquals(listOf("busan"), newArrivals(6, 7, route).map { it.id })
    }

    @Test fun `값이 줄어들면 새 도착 없음`() {
        assertTrue(newArrivals(4, 2, route).isEmpty())
    }
}

class MapPositionTest {
    private val route = routeFor(1)

    @Test fun `스테이지 시작은 출발 도시 좌표`() {
        val pos = mapPosition(locate(0, route), route)
        assertEquals(Season1Route.SEOUL.lat, pos.lat, 1e-9)
        assertEquals(Season1Route.SEOUL.lng, pos.lng, 1e-9)
    }

    @Test fun `진행률 절반은 두 도시의 중간`() {
        val pos = mapPosition(locate(9_500, route), route)
        val s = Season1Route.SEOUL
        val a = Season1Route.ANYANG
        assertEquals((s.lat + a.lat) / 2, pos.lat, 1e-9)
        assertEquals((s.lng + a.lng) / 2, pos.lng, 1e-9)
    }

    @Test fun `완주하면 부산에 멈춘다`() {
        val pos = mapPosition(locate(500_000, route), route)
        assertEquals(Season1Route.BUSAN.lat, pos.lat, 1e-9)
        assertEquals(Season1Route.BUSAN.lng, pos.lng, 1e-9)
    }
}

class HaversineTest {
    @Test fun `같은 점은 0m`() {
        assertEquals(0.0, haversineM(37.5, 127.0, 37.5, 127.0), 1e-9)
    }

    @Test fun `위도 1도는 약 111km`() {
        assertEquals(111_195.0, haversineM(37.0, 127.0, 38.0, 127.0), 50.0)
    }
}
