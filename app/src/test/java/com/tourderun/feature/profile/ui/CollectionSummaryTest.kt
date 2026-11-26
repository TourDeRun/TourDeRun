package com.tourderun.feature.profile.ui

import com.tourderun.core.data.card.OwnedCard
import com.tourderun.domain.route.routeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionSummaryTest {
    private val route = routeFor(1)

    @Test fun `카드 없음, 팀 거리 0m → 0 of 8, 다음 카드 안양`() {
        val summary = collectionSummary(route, owned = emptyList(), teamDistanceM = 0)
        assertEquals(CollectionSummaryUi(collected = 0, total = 8, nextCity = "안양"), summary)
    }

    @Test fun `서울·안양 카드, 팀 거리 19000m → 2 of 8, 다음 카드 오산`() {
        val owned = listOf(OwnedCard("seoul", 1, "팀"), OwnedCard("anyang", 2, "팀"))
        assertEquals(CollectionSummaryUi(2, 8, "오산"), collectionSummary(route, owned, teamDistanceM = 19_000))
    }

    @Test fun `경로에 없는 도시 카드는 세지 않는다`() {
        val owned = listOf(OwnedCard("seoul", 1, "팀"), OwnedCard("jeju", 2, "팀"))
        assertEquals(1, collectionSummary(route, owned, teamDistanceM = 0).collected)
    }

    @Test fun `팀이 없으면 다음 카드 없음`() {
        assertNull(collectionSummary(route, emptyList(), teamDistanceM = null).nextCity)
    }

    @Test fun `완주(350000m 이상) → 다음 카드 없음`() {
        assertNull(collectionSummary(route, emptyList(), teamDistanceM = 350_000).nextCity)
    }
}
