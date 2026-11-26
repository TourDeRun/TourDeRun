package com.tourderun.core.data.card

import com.google.firebase.Timestamp
import com.tourderun.core.data.firestore.FieldOp
import com.tourderun.core.data.firestore.UserFields
import com.tourderun.core.data.firestore.WriteMode
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OwnedCardsTest {
    private fun card(atMs: Long?, teamName: String?) = buildMap<String, Any?> {
        if (atMs != null) put(UserFields.CARD_AT, Timestamp(Date(atMs)))
        if (teamName != null) put(UserFields.CARD_TEAM_NAME, teamName)
    }

    @Test fun `문서 없음·cards 없음·빈 cards → 빈 목록`() {
        assertEquals(emptyList<OwnedCard>(), ownedCardsOf(null))
        assertEquals(emptyList<OwnedCard>(), ownedCardsOf(mapOf(UserFields.NICKNAME to "러너")))
        assertEquals(emptyList<OwnedCard>(), ownedCardsOf(mapOf(UserFields.CARDS to emptyMap<String, Any>())))
    }

    @Test fun `여러 장 → 받은 시각 순, 같으면 도시 ID 순`() {
        val fields = mapOf(
            UserFields.CARDS to mapOf(
                "osan" to card(3_000, "뛰뛰크루"),
                "seoul" to card(1_000, "뛰뛰크루"),
                "cheonan" to card(3_000, "새 팀"),
                "anyang" to card(2_000, "뛰뛰크루"),
            ),
        )
        assertEquals(
            listOf(
                OwnedCard("seoul", 1_000, "뛰뛰크루"),
                OwnedCard("anyang", 2_000, "뛰뛰크루"),
                OwnedCard("cheonan", 3_000, "새 팀"),
                OwnedCard("osan", 3_000, "뛰뛰크루"),
            ),
            ownedCardsOf(fields),
        )
    }

    @Test fun `at이 없는 대기 쓰기 → 시각 0, 카드는 가진 것으로 센다`() {
        val fields = mapOf(UserFields.CARDS to mapOf("anyang" to card(atMs = null, teamName = "뛰뛰크루")))
        assertEquals(listOf(OwnedCard("anyang", 0, "뛰뛰크루")), ownedCardsOf(fields))
    }

    @Test fun `카드 값이 Map이 아니거나 teamName이 없어도 → 가진 것으로 센다`() {
        val fields = mapOf(UserFields.CARDS to mapOf("seoul" to true, "anyang" to card(5_000, teamName = null)))
        assertEquals(
            listOf(OwnedCard("seoul", 0, ""), OwnedCard("anyang", 5_000, "")),
            ownedCardsOf(fields),
        )
    }

    @Test fun `캐시에서 온 없는 문서 → null(내지 않음)`() {
        assertNull(ownedCardsOrNull(exists = false, isFromCache = true, fields = null))
    }

    @Test fun `서버가 확인한 없는 문서 → 빈 목록`() {
        assertEquals(emptyList<OwnedCard>(), ownedCardsOrNull(exists = false, isFromCache = false, fields = null))
    }

    @Test fun `캐시에서 온 있는 문서 → 카드 목록`() {
        val fields = mapOf(UserFields.CARDS to mapOf("seoul" to card(1_000, "뛰뛰크루")))
        assertEquals(listOf(OwnedCard("seoul", 1_000, "뛰뛰크루")), ownedCardsOrNull(true, true, fields))
    }

    @Test fun `cardWrite - 빈 목록 → null`() {
        assertNull(cardWrite("uid-1", emptyList(), "뛰뛰크루"))
    }

    @Test fun `cardWrite - 서울 카드 → users 문서 set merge, 같은 ID는 한 번`() {
        val write = requireNotNull(cardWrite("uid-1", listOf("seoul", "seoul"), "뛰뛰크루"))
        assertEquals("users/uid-1", write.path)
        assertEquals(WriteMode.SET_MERGE, write.mode)
        val card = mapOf(UserFields.CARD_AT to FieldOp.ServerTime, UserFields.CARD_TEAM_NAME to "뛰뛰크루")
        assertEquals(mapOf(UserFields.CARDS to mapOf("seoul" to card)), write.fields)
        assertEquals(cardFields(listOf("seoul"), "뛰뛰크루"), write.fields)
    }
}
