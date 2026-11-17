package com.tourderun.domain.team

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCodeTest {

    // ---- 만들기 (TEAM-02) ----

    @Test fun `코드는 6자리`() {
        assertEquals(6, generateInviteCode(Random(1)).length)
    }

    @Test fun `같은 씨앗이면 같은 코드`() {
        assertEquals(generateInviteCode(Random(42)), generateInviteCode(Random(42)))
    }

    @Test fun `1000번 만들어도 허용 글자만 쓴다`() {
        val random = Random(7)
        repeat(1000) {
            val code = generateInviteCode(random)
            assertTrue(code, isInviteCodeFormat(code))
        }
    }

    @Test fun `헷갈리는 0 O 1 I는 나오지 않는다`() {
        val random = Random(7)
        val used = (1..1000).flatMap { generateInviteCode(random).toList() }.toSet()
        assertFalse(used.any { it in "0O1I" })
    }

    @Test fun `허용 글자는 32개이고 0 O 1 I가 없다`() {
        assertEquals(32, INVITE_CODE_CHARS.length)
        assertEquals(32, INVITE_CODE_CHARS.toSet().size)
        assertFalse(INVITE_CODE_CHARS.any { it in "0O1I" })
    }

    // ---- 입력 정리 (TEAM-03) ----

    @Test fun `소문자는 대문자로 바꾼다`() {
        assertEquals("ABC234", normalizeInviteCode("abc234"))
    }

    @Test fun `앞뒤 공백은 지운다`() {
        assertEquals("ABC234", normalizeInviteCode("  ABC234 "))
    }

    // ---- 형식 검사 (TEAM-03) ----

    @Test fun `6자리 허용 글자는 통과`() {
        assertTrue(isInviteCodeFormat("ABC234"))
    }

    @Test fun `5자리는 실패`() {
        assertFalse(isInviteCodeFormat("ABC23"))
    }

    @Test fun `7자리는 실패`() {
        assertFalse(isInviteCodeFormat("ABC2345"))
    }

    @Test fun `0이 섞이면 실패`() {
        assertFalse(isInviteCodeFormat("ABC230"))
    }

    @Test fun `소문자는 정리 전이면 실패`() {
        assertFalse(isInviteCodeFormat("abc234"))
    }

    @Test fun `빈 값은 실패`() {
        assertFalse(isInviteCodeFormat(""))
    }
}