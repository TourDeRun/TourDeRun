package com.tourderun.feature.run.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunBusyTest {

    @Test fun `아무 러닝도 없으면 막지 않는다`() {
        assertFalse(isRunBusy(RunPhase.IDLE, hasStoredSession = false, pendingSaveCount = 0))
    }

    @Test fun `카운트다운·측정·일시정지·종료 후에는 막는다`() {
        listOf(RunPhase.COUNTDOWN, RunPhase.RUNNING, RunPhase.PAUSED, RunPhase.FINISHED).forEach {
            assertTrue(isRunBusy(it, hasStoredSession = false, pendingSaveCount = 0))
        }
    }

    @Test fun `앱을 다시 켰어도 기기에 진행 중 러닝이 남아 있으면 막는다`() {
        assertTrue(isRunBusy(RunPhase.IDLE, hasStoredSession = true, pendingSaveCount = 0))
    }

    @Test fun `서버 확인을 기다리는 러닝이 있으면 막는다`() {
        assertTrue(isRunBusy(RunPhase.IDLE, hasStoredSession = false, pendingSaveCount = 1))
    }
}
