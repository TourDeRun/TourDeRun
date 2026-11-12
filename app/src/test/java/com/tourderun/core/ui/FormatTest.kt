package com.tourderun.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun `kmUp은 남은 거리를 올림해서 도착 전에 0점0이 나오지 않는다`() {
        assertEquals("0.1", Format.kmUp(40))
        assertEquals("0.1", Format.kmUp(1))
        assertEquals("0.0", Format.kmUp(0))
        assertEquals("0.1", Format.kmUp(100))
        assertEquals("0.2", Format.kmUp(101))
        assertEquals("0.01", Format.kmUp(4, decimals = 2))
    }

    @Test fun `kmDown은 지나온 거리를 내림해서 끝 거리로 보이지 않는다`() {
        assertEquals("84.9", Format.kmDown(84_960))
        assertEquals("85.0", Format.kmDown(85_000))
        assertEquals("0.0", Format.kmDown(99))
        assertEquals("36.96", Format.kmDown(36_960, decimals = 2))
    }

    @Test fun `km은 지금처럼 반올림`() {
        assertEquals("85.0", Format.km(84_960))
        assertEquals("66.8", Format.km(66_800))
    }
}
