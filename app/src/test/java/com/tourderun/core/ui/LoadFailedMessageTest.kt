package com.tourderun.core.ui

import com.tourderun.R
import com.tourderun.core.data.DataError
import org.junit.Assert.assertEquals
import org.junit.Test

/** 실패 종류 → 불러오기 실패 문구 (S15·S16 공용). */
class LoadFailedMessageTest {

    @Test fun `서버에 닿지 못함 → 연결 확인 문구`() {
        assertEquals(R.string.load_failed_unavailable, DataError.Unavailable.loadFailedMessageRes())
    }

    @Test fun `권한 거절 → 처리할 수 없음 문구`() {
        assertEquals(R.string.load_failed_other, DataError.PermissionDenied.loadFailedMessageRes())
    }

    @Test fun `그 밖의 실패 → 처리할 수 없음 문구`() {
        assertEquals(R.string.load_failed_other, DataError.Other.loadFailedMessageRes())
    }
}
