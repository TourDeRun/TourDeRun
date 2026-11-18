package com.tourderun.core.data.firestore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** members 문서 필드 → 본 도착 수(JRN-05). */
class MemberDtoTest {

    @Test fun `문서 없음 → null`() {
        assertNull(seenArrivalsOf(null))
    }

    @Test fun `seenArrivals 필드 없음 → 0`() {
        assertEquals(0, seenArrivalsOf(mapOf(MemberFields.NICKNAME to "영훈")))
    }

    @Test fun `빈 문서 → 0`() {
        assertEquals(0, seenArrivalsOf(emptyMap()))
    }

    @Test fun `Firestore 정수(Long) 3 → 3`() {
        assertEquals(3, seenArrivalsOf(mapOf(MemberFields.SEEN_ARRIVALS to 3L)))
    }

    @Test fun `Double 값 → 정수로`() {
        assertEquals(7, seenArrivalsOf(mapOf(MemberFields.SEEN_ARRIVALS to 7.0)))
    }

    @Test fun `숫자가 아니거나 null → 0`() {
        assertEquals(0, seenArrivalsOf(mapOf(MemberFields.SEEN_ARRIVALS to "3")))
        assertEquals(0, seenArrivalsOf(mapOf(MemberFields.SEEN_ARRIVALS to null)))
    }

    @Test fun `음수 → 0`() {
        assertEquals(0, seenArrivalsOf(mapOf(MemberFields.SEEN_ARRIVALS to -1L)))
    }
}
