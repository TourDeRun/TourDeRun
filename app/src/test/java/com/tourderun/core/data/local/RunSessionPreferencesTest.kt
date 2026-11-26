package com.tourderun.core.data.local

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunSessionPreferencesTest {
    private val oldId = stringPreferencesKey("run_id")
    private val oldStatus = stringPreferencesKey("status")
    private val id = stringPreferencesKey("active_run_id")
    private val status = stringPreferencesKey("run_state")
    private val started = longPreferencesKey("started_at_ms")
    private val pausedTotal = longPreferencesKey("paused_total_ms")
    private val pausedAt = longPreferencesKey("paused_at_ms")
    private val heartbeat = longPreferencesKey("last_heartbeat_ms")
    private val segment = intPreferencesKey("segment")

    private val paused = StoredRunSession(
        runId = "run-1",
        status = StoredRunStatus.PAUSED,
        startedAtMs = 1_000L,
        pausedTotalMs = 200L,
        pausedAtMs = 2_000L,
        lastHeartbeatMs = 1_900L,
        segment = 2,
    )

    @Test fun `기존 키로 저장된 일시정지 러닝을 복구한다`() {
        val prefs = mutablePreferencesOf(
            oldId to "run-1", oldStatus to "PAUSED", started to 1_000L,
            pausedTotal to 200L, pausedAt to 2_000L, heartbeat to 1_900L, segment to 2,
        )

        assertEquals(paused, prefs.readRunSession())
    }

    @Test fun `러닝 저장 시 새 키로 쓰고 이전 키를 제거한다`() {
        val prefs = mutablePreferencesOf(oldId to "run-1", oldStatus to "PAUSED", started to 1_000L)

        prefs.writeRunSession(paused)

        assertEquals("run-1", prefs[id])
        assertEquals("PAUSED", prefs[status])
        assertNull(prefs[oldId])
        assertNull(prefs[oldStatus])
        assertEquals(paused, prefs.readRunSession())
    }

    @Test fun `재개하여 저장하면 이전 일시정지 시각을 지운다`() {
        val prefs = mutablePreferencesOf(pausedAt to 2_000L)
        val running = paused.copy(status = StoredRunStatus.RUNNING, pausedAtMs = null)

        prefs.writeRunSession(running)

        assertNull(prefs[pausedAt])
        assertEquals(running, prefs.readRunSession())
    }

    @Test fun `다른 러닝의 정리 요청은 현재 세션을 지우지 않는다`() {
        val prefs = mutablePreferencesOf(id to "new-run", status to "RUNNING", started to 1_000L)

        prefs.clearRunSessionIfMatching("old-run")

        assertEquals("new-run", prefs.readRunSession()?.runId)
    }

    @Test fun `기존 키로 저장된 러닝도 일치하면 지운다`() {
        val prefs = mutablePreferencesOf(oldId to "run-1", oldStatus to "PAUSED", started to 1_000L)

        prefs.clearRunSessionIfMatching("run-1")

        assertNull(prefs.readRunSession())
        assertFalse(prefs.asMap().containsKey(oldId))
    }

    // --- 저장 대기 목록(#99) ---

    private val finished = paused.copy(status = StoredRunStatus.FINISHED)

    @Test fun `저장 대기로 옮기면 진행 중 키는 지우고 복구에 필요한 값은 대기 목록에 남는다`() {
        val prefs = mutablePreferencesOf()
        prefs.writeRunSession(finished)

        assertTrue(prefs.moveRunSessionToPendingSave("run-1"))

        assertNull(prefs.readRunSession())
        assertEquals(listOf(PendingRunSave(finished, PendingSaveState.AWAITING)), prefs.readPendingSaves())
    }

    @Test fun `진행 중인 러닝이 다르면 저장 대기로 옮기지 않는다`() {
        val prefs = mutablePreferencesOf()
        prefs.writeRunSession(finished)

        assertFalse(prefs.moveRunSessionToPendingSave("other"))

        assertEquals(finished, prefs.readRunSession())
        assertTrue(prefs.readPendingSaves().isEmpty())
    }

    @Test fun `진행 중 러닝을 지워도 저장 대기 목록은 남는다`() {
        val prefs = mutablePreferencesOf()
        prefs.writeRunSession(finished)
        prefs.moveRunSessionToPendingSave("run-1")
        prefs.writeRunSession(paused.copy(runId = "run-2", status = StoredRunStatus.RUNNING, pausedAtMs = null))

        prefs.clearRunSessionIfMatching("run-2")

        assertNull(prefs.readRunSession())
        assertEquals(listOf("run-1"), prefs.readPendingSaves().map { it.runId })
    }

    @Test fun `거부로 바꾼 뒤 진행 중 러닝이 없을 때만 원래 값으로 되돌린다`() {
        val prefs = mutablePreferencesOf()
        prefs.writeRunSession(finished)
        prefs.moveRunSessionToPendingSave("run-1")
        assertTrue(prefs.setPendingSaveState("run-1", PendingSaveState.REJECTED))
        prefs.writeRunSession(paused.copy(runId = "run-2"))

        assertNull(prefs.restorePendingSave("run-1"))
        assertEquals(PendingSaveState.REJECTED, prefs.readPendingSaves().single().state)

        prefs.clearRunSessionIfMatching("run-2")
        assertEquals(finished, prefs.restorePendingSave("run-1"))
        assertEquals(finished, prefs.readRunSession())
        assertTrue(prefs.readPendingSaves().isEmpty())
    }

    @Test fun `대기 항목은 일시정지 시각이 없어도 읽고, 형식이 깨진 항목은 건너뛴다`() {
        val save = PendingRunSave(finished.copy(pausedAtMs = null), PendingSaveState.AWAITING)

        assertEquals(save, decodePendingSave(encodePendingSave(save)))
        assertNull(decodePendingSave("run-1;AWAITING"))
        assertNull(decodePendingSave("run-1;UNKNOWN;1;0;;1;0"))
    }

    @Test fun `없는 대기 항목은 바꾸거나 지우지 않는다`() {
        val prefs = mutablePreferencesOf()

        assertFalse(prefs.setPendingSaveState("run-1", PendingSaveState.REJECTED))
        assertFalse(prefs.removePendingSave("run-1"))
    }
}
