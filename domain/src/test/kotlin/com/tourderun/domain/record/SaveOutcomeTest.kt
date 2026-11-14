package com.tourderun.domain.record

import com.tourderun.domain.route.Season1Route
import com.tourderun.domain.route.routeFor
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveOutcomeTest {
    private val route = routeFor(1)

    private fun cityIds(beforeTeamM: Long, runM: Long) =
        saveOutcome(beforeTeamM, runM, route).newCities.map { it.id }

    @Test fun `저장 후 팀 거리는 저장 전 + 러닝 거리`() {
        val o = saveOutcome(66_800, 5_000, route)
        assertEquals(66_800L, o.beforeTeamM)
        assertEquals(71_800L, o.afterTeamM)
    }

    @Test fun `도시를 지나지 않으면 새 도시 없음`() {
        assertEquals(emptyList<String>(), cityIds(10_000, 5_000))
    }

    @Test fun `18000m + 999m = 18999m는 안양 직전이라 새 도시 없음`() {
        assertEquals(emptyList<String>(), cityIds(18_000, 999))
    }

    @Test fun `18000m + 1000m = 19000m는 안양 도착`() {
        assertEquals(listOf("anyang"), cityIds(18_000, 1_000))
    }

    @Test fun `이미 19000m인 팀은 안양을 다시 받지 않는다`() {
        assertEquals(emptyList<String>(), cityIds(19_000, 5_000))
    }

    @Test fun `한 번에 여러 도시를 지나면 도착 순서대로`() {
        assertEquals(listOf("anyang", "osan", "cheonan"), cityIds(0, 85_000))
    }

    @Test fun `345000m + 10000m는 부산 도착, 거리는 350km를 넘어 계속 쌓인다`() {
        val o = saveOutcome(345_000, 10_000, route)
        assertEquals(listOf(Season1Route.BUSAN), o.newCities)
        assertEquals(355_000L, o.afterTeamM)
    }

    @Test fun `완주한 팀은 새 도시 없음`() {
        assertEquals(emptyList<String>(), cityIds(350_000, 5_000))
    }

    @Test fun `러닝 거리 0m면 새 도시 없음`() {
        assertEquals(emptyList<String>(), cityIds(19_000, 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `러닝 거리가 음수면 예외`() {
        saveOutcome(10_000, -1, route)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `저장 전 팀 거리가 음수면 예외`() {
        saveOutcome(-1, 1_000, route)
    }
}
