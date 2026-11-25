package com.tourderun.feature.run.session

import com.tourderun.core.data.local.PendingSaveState.AWAITING
import com.tourderun.core.data.local.PendingSaveState.REJECTED
import com.tourderun.core.data.run.RunBusyReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunBusyTest {

    @Test fun `아무 러닝도 없으면 막지 않는다`() {
        assertNull(runBusyReason(RunPhase.IDLE, hasStoredSession = false, pendingStates = emptyList()))
    }

    @Test fun `카운트다운·측정·일시정지·종료 직후에는 러닝 중`() {
        listOf(RunPhase.COUNTDOWN, RunPhase.RUNNING, RunPhase.PAUSED, RunPhase.FINISHED).forEach {
            assertEquals(RunBusyReason.ACTIVE, runBusyReason(it, hasStoredSession = false, pendingStates = emptyList()))
        }
    }

    @Test fun `앱을 다시 켰어도 기기에 진행 중 러닝이 남아 있으면 러닝 중`() {
        assertEquals(RunBusyReason.ACTIVE, runBusyReason(RunPhase.IDLE, hasStoredSession = true, pendingStates = emptyList()))
    }

    @Test fun `서버 확인 대기만 있으면 저장 중`() {
        assertEquals(RunBusyReason.SAVING, runBusyReason(RunPhase.IDLE, hasStoredSession = false, pendingStates = listOf(AWAITING)))
    }

    @Test fun `저장 거절 기록이 있으면 거절을 먼저 알린다`() {
        assertEquals(
            RunBusyReason.SAVE_REJECTED,
            runBusyReason(RunPhase.IDLE, hasStoredSession = false, pendingStates = listOf(AWAITING, REJECTED)),
        )
    }

    @Test fun `러닝 중이면 대기·거절보다 러닝 중을 먼저 알린다`() {
        assertEquals(RunBusyReason.ACTIVE, runBusyReason(RunPhase.RUNNING, hasStoredSession = true, pendingStates = listOf(REJECTED)))
    }
}
