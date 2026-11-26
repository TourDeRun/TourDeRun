package com.tourderun.domain.run

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunRulesTest {
    @Test fun `99m를 5분 달리면 저장하지 않는다`() {
        assertTrue(isTooShortToSave(99.9, 300))
    }

    @Test fun `2km를 59초 달리면 저장하지 않는다`() {
        assertTrue(isTooShortToSave(2_000.0, 59))
    }

    @Test fun `100m를 정확히 1분 달리면 저장한다`() {
        assertFalse(isTooShortToSave(100.0, 60))
    }

    @Test fun `채택된 점이 9초 동안 없으면 아직 약하지 않다`() {
        assertFalse(isGpsWeak(lastAcceptedMs = 1_000, segmentStartedMs = 0, nowMs = 10_999))
    }

    @Test fun `채택된 점이 10초 동안 없으면 약하다`() {
        assertTrue(isGpsWeak(lastAcceptedMs = 1_000, segmentStartedMs = 0, nowMs = 11_000))
    }

    @Test fun `시작 후 점이 하나도 없으면 시작 시각부터 센다`() {
        assertFalse(isGpsWeak(lastAcceptedMs = null, segmentStartedMs = 5_000, nowMs = 14_999))
        assertTrue(isGpsWeak(lastAcceptedMs = null, segmentStartedMs = 5_000, nowMs = 15_000))
    }

    @Test fun `재개 전에 채택된 점은 재개 시각으로 대신한다`() {
        assertFalse(isGpsWeak(lastAcceptedMs = 1_000, segmentStartedMs = 60_000, nowMs = 65_000))
    }
}
