package com.tourderun.domain.team

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NicknameValidatorTest {

    // ---- 통과하는 경우 ----

    @Test fun `한글 2자는 통과`() {
        assertNull(validateNickname("민찬"))
    }

    @Test fun `한글·영문·숫자를 섞어도 통과`() {
        assertNull(validateNickname("민찬tronve7"))
    }

    @Test fun `정확히 10자는 통과`() {
        assertNull(validateNickname("가나다라마바사아자차"))
    }

    // ---- 길이 ----

    @Test fun `빈 값은 너무 짧음`() {
        assertEquals(NicknameError.TOO_SHORT, validateNickname(""))
    }

    @Test fun `1자는 너무 짧음`() {
        assertEquals(NicknameError.TOO_SHORT, validateNickname("a"))
    }

    @Test fun `11자는 너무 김`() {
        assertEquals(NicknameError.TOO_LONG, validateNickname("abcdefghijk"))
    }

    // ---- 글자 종류 ----

    @Test fun `가운데 공백은 허용하지 않음`() {
        assertEquals(NicknameError.INVALID_CHARS, validateNickname("민 지"))
    }

    @Test fun `자음만 쓴 글자는 허용하지 않음`() {
        assertEquals(NicknameError.INVALID_CHARS, validateNickname("ㅋㅋㅋ"))
    }

    @Test fun `기호는 허용하지 않음`() {
        assertEquals(NicknameError.INVALID_CHARS, validateNickname("run_ner"))
    }

    @Test fun `공백 한 글자는 길이보다 글자 종류를 먼저 알려줌`() {
        assertEquals(NicknameError.INVALID_CHARS, validateNickname(" "))
    }

    @Test fun `이모지는 허용하지 않음`() {
        assertEquals(NicknameError.INVALID_CHARS, validateNickname("러너😀"))
    }

    @Test fun `끝에 붙은 공백도 글자 종류 오류`() {
        assertEquals(NicknameError.INVALID_CHARS, validateNickname("민찬 "))
    }
}
