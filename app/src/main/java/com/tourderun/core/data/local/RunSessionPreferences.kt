package com.tourderun.core.data.local

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

private object RunSessionKeys {
    val id = stringPreferencesKey("active_run_id")
    val status = stringPreferencesKey("run_state")
    // 기존 버전에서 저장한 진행 중 러닝도 복구한다.
    val legacyId = stringPreferencesKey("run_id")
    val legacyStatus = stringPreferencesKey("status")
    val started = longPreferencesKey("started_at_ms")
    val pausedTotal = longPreferencesKey("paused_total_ms")
    val pausedAt = longPreferencesKey("paused_at_ms")
    val heartbeat = longPreferencesKey("last_heartbeat_ms")
    val segment = intPreferencesKey("segment")
}

internal fun Preferences.readRunSession(): StoredRunSession? {
    val id = this[RunSessionKeys.id] ?: this[RunSessionKeys.legacyId] ?: return null
    val started = this[RunSessionKeys.started] ?: return null
    val statusName = this[RunSessionKeys.status] ?: this[RunSessionKeys.legacyStatus] ?: return null
    val status = StoredRunStatus.entries.firstOrNull { it.name == statusName } ?: return null
    return StoredRunSession(
        runId = id,
        status = status,
        startedAtMs = started,
        pausedTotalMs = this[RunSessionKeys.pausedTotal] ?: 0,
        pausedAtMs = this[RunSessionKeys.pausedAt],
        lastHeartbeatMs = this[RunSessionKeys.heartbeat] ?: started,
        segment = this[RunSessionKeys.segment] ?: 0,
    )
}

internal fun MutablePreferences.writeRunSession(session: StoredRunSession) {
    this[RunSessionKeys.id] = session.runId
    this[RunSessionKeys.status] = session.status.name
    this[RunSessionKeys.started] = session.startedAtMs
    this[RunSessionKeys.pausedTotal] = session.pausedTotalMs
    if (session.pausedAtMs == null) remove(RunSessionKeys.pausedAt) else this[RunSessionKeys.pausedAt] = session.pausedAtMs
    this[RunSessionKeys.heartbeat] = session.lastHeartbeatMs
    this[RunSessionKeys.segment] = session.segment
    // 새 키와 이전 키 제거는 호출자의 같은 DataStore edit 안에서 처리한다.
    remove(RunSessionKeys.legacyId)
    remove(RunSessionKeys.legacyStatus)
}

internal fun MutablePreferences.clearRunSessionIfMatching(runId: String) {
    val activeRunId = this[RunSessionKeys.id] ?: this[RunSessionKeys.legacyId]
    if (activeRunId == runId) clear()
}
