package com.tourderun.core.data.local

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
}
