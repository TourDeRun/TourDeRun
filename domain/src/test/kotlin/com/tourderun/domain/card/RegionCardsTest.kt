package com.tourderun.domain.card

import com.tourderun.domain.route.routeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionCardsTest {
    private val route = routeFor(1)

    @Test fun `카드 8장이 경로 도시와 같은 순서다`() {
        assertEquals(route.cities.map { it.id }, RegionCards.all.map { it.cityId })
    }

    @Test fun `모든 카드에 소개와 랜드마크 2~3곳이 있다`() {
        for (card in RegionCards.all) {
            assertTrue(card.cityId, card.intro.isNotBlank())
            assertTrue(card.cityId, card.landmarks.size in 2..3)
            assertTrue(card.cityId, card.landmarks.all { it.name.isNotBlank() && it.description.isNotBlank() })
        }
    }

    @Test fun `도시 ID로 카드를 찾고 없는 ID는 null`() {
        assertEquals("Busan", RegionCards.of("busan")?.nameEn)
        assertNull(RegionCards.of("jeju"))
    }
}
