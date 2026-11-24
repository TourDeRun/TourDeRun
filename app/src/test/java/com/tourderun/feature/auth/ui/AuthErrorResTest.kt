package com.tourderun.feature.auth.ui

import com.tourderun.R
import com.tourderun.core.data.DataError
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorResTest {

    @Test fun `로그인 연결 문제는 네트워크 문구`() {
        assertEquals(R.string.login_error_network, loginErrorRes(DataError.Unavailable))
    }

    @Test fun `로그인 그 밖의 실패는 실패 문구`() {
        assertEquals(R.string.login_error_failed, loginErrorRes(DataError.Other))
        assertEquals(R.string.login_error_failed, loginErrorRes(DataError.PermissionDenied))
    }

    @Test fun `닉네임 저장 연결 문제는 네트워크 문구`() {
        assertEquals(R.string.nickname_error_network, nicknameErrorRes(DataError.Unavailable))
    }

    @Test fun `닉네임 저장 그 밖의 실패는 실패 문구`() {
        assertEquals(R.string.nickname_error_failed, nicknameErrorRes(DataError.Other))
    }
}
