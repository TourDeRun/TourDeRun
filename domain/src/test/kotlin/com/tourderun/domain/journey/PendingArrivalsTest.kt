package com.tourderun.domain.journey

import com.tourderun.domain.route.Season1Route
import com.tourderun.domain.route.routeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingArrivalsTest {
    private val route = routeFor(1)

    private fun pending(seen: Int, distanceM: Long) =
        requireNotNull(pendingArrivals(seen, distanceM, route)) { "새 도착이 있어야 함" }

    @Test fun `본 것 0, 18999m → 아직 도착 없음`() {
        assertNull(pendingArrivals(0, 18_999, route))
    }

    @Test fun `본 것 0, 19000m → 안양`() {
        val p = pending(0, 19_000)
        assertEquals(listOf(Season1Route.ANYANG), p.newCities)
        assertEquals(0, p.seen)
        assertEquals(1, p.current)
        assertEquals(Season1Route.ANYANG, p.lastCity)
        assertFalse(p.isFinish)
    }

    @Test fun `본 것 1, 85000m → 오산·천안, 마지막 도시는 천안`() {
        val p = pending(1, 85_000)
        assertEquals(listOf(Season1Route.OSAN, Season1Route.CHEONAN), p.newCities)
        assertEquals(3, p.current)
        assertEquals(Season1Route.CHEONAN, p.lastCity)
        assertFalse(p.isFinish)
    }

    @Test fun `본 것 6, 350000m → 부산 완주`() {
        val p = pending(6, 350_000)
        assertEquals(listOf(Season1Route.BUSAN), p.newCities)
        assertEquals(7, p.current)
        assertTrue(p.isFinish)
    }

    @Test fun `349999m는 아직 완주 아님`() {
        assertFalse(pending(5, 349_999).isFinish)
    }

    @Test fun `본 것이 지금 도착 수보다 크면 → null`() {
        // 60,000m는 도착 2(안양·오산)
        assertNull(pendingArrivals(3, 60_000, route))
    }

    @Test fun `본 것과 지금 도착 수가 같으면 → null`() {
        assertNull(pendingArrivals(2, 60_000, route))
    }

    @Test fun `완주 연출을 본 뒤 거리가 더 쌓여도 → null`() {
        assertNull(pendingArrivals(7, 400_000, route))
    }

    @Test fun `처음 가입한 팀이 한 번에 여러 도시를 지나면 지난 도시 전부`() {
        val p = pending(0, 125_000)
        assertEquals(listOf("anyang", "osan", "cheonan", "sejong"), p.newCities.map { it.id })
        assertEquals(Season1Route.SEJONG, p.lastCity)
    }
}
