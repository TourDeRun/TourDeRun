package com.tourderun.domain.card

import com.tourderun.domain.route.routeFor
import org.junit.Assert.assertEquals
import org.junit.Test

class CardRulesTest {
    private val route = routeFor(1)

    private fun cards(
        seen: Int,
        current: Int,
        owned: Set<String> = emptySet(),
        firstJoin: Boolean = false,
    ) = newCardCityIds(seen, current, owned, firstJoin, route)

    @Test fun `새 도착이 없으면 카드 없음`() {
        assertEquals(emptyList<String>(), cards(seen = 2, current = 2, owned = setOf("seoul", "anyang", "osan")))
    }

    @Test fun `도시 하나 도착 → 그 도시 카드`() {
        assertEquals(listOf("anyang"), cards(seen = 0, current = 1, owned = setOf("seoul")))
    }

    @Test fun `여러 도시 한 번에 도착 → 도착 순서대로 여러 장`() {
        assertEquals(listOf("osan", "cheonan", "sejong"), cards(seen = 1, current = 4))
    }

    @Test fun `이미 가진 카드는 다시 주지 않는다`() {
        assertEquals(listOf("osan"), cards(seen = 0, current = 2, owned = setOf("seoul", "anyang")))
    }

    @Test fun `팀을 옮겨 같은 도시에 다시 도착해도 카드는 그대로`() {
        assertEquals(emptyList<String>(), cards(seen = 0, current = 1, owned = setOf("seoul", "anyang")))
    }

    @Test fun `부산 도착 → 부산 카드`() {
        assertEquals(listOf("busan"), cards(seen = 6, current = 7))
    }

    @Test fun `처음 팀에 소속되면 서울 카드`() {
        assertEquals(listOf("seoul"), cards(seen = 0, current = 0, firstJoin = true))
    }

    @Test fun `처음 소속이 아니면 서울 카드 없음`() {
        assertEquals(emptyList<String>(), cards(seen = 0, current = 0, firstJoin = false))
    }

    @Test fun `서울 카드를 이미 가졌으면 처음 소속이어도 다시 주지 않는다`() {
        assertEquals(emptyList<String>(), cards(seen = 0, current = 0, owned = setOf("seoul"), firstJoin = true))
    }

    @Test fun `서울 카드와 도착 카드가 같이 생기면 서울이 먼저`() {
        assertEquals(listOf("seoul", "anyang", "osan"), cards(seen = 0, current = 2, firstJoin = true))
    }

    @Test fun `본 도착 수가 지금보다 크면 카드 없음`() {
        assertEquals(emptyList<String>(), cards(seen = 3, current = 1))
    }
}
