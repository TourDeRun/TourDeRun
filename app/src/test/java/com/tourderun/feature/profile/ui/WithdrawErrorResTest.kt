package com.tourderun.feature.profile.ui

import com.tourderun.R
import com.tourderun.core.data.DataError
import org.junit.Assert.assertEquals
import org.junit.Test

class WithdrawErrorResTest {
    @Test fun `연결 문제는 네트워크 문구`() {
        assertEquals(R.string.withdraw_error_network, withdrawErrorRes(DataError.Unavailable))
    }

    @Test fun `그 밖의 실패는 실패 문구`() {
        assertEquals(R.string.withdraw_error_failed, withdrawErrorRes(DataError.Other))
        assertEquals(R.string.withdraw_error_failed, withdrawErrorRes(DataError.PermissionDenied))
    }
}
