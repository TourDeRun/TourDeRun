package com.tourderun.core.data.local

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

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

    /** 저장 대기 러닝(#99). 항목 하나가 러닝 하나([encodePendingSave]). */
    val pendingSaves = stringSetPreferencesKey("pending_saves")

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
    if (activeRunId == runId) clearRunSession()
}

/** 진행 중인 러닝 키만 지운다. 저장 대기 목록은 남긴다. */
private fun MutablePreferences.clearRunSession() {
    remove(RunSessionKeys.id)
    remove(RunSessionKeys.status)
    remove(RunSessionKeys.legacyId)
    remove(RunSessionKeys.legacyStatus)
    remove(RunSessionKeys.started)
    remove(RunSessionKeys.pausedTotal)
    remove(RunSessionKeys.pausedAt)
    remove(RunSessionKeys.heartbeat)
    remove(RunSessionKeys.segment)
}

internal fun Preferences.readPendingSaves(): List<PendingRunSave> =
    this[RunSessionKeys.pendingSaves].orEmpty()
        .mapNotNull(::decodePendingSave)
        .sortedBy { it.session.startedAtMs }

private fun MutablePreferences.writePendingSaves(saves: List<PendingRunSave>) {
    if (saves.isEmpty()) remove(RunSessionKeys.pendingSaves) else this[RunSessionKeys.pendingSaves] = saves.map(::encodePendingSave).toSet()
}

/** 진행 중인 러닝 [runId]를 저장 대기 목록으로 옮긴다. 진행 중인 러닝이 [runId]가 아니면 false. */
internal fun MutablePreferences.moveRunSessionToPendingSave(runId: String): Boolean {
    val active = readRunSession()?.takeIf { it.runId == runId } ?: return false
    val others = readPendingSaves().filterNot { it.runId == runId }
    writePendingSaves(others + PendingRunSave(active.copy(status = StoredRunStatus.FINISHED), PendingSaveState.AWAITING))
    clearRunSession()
    return true
}

internal fun MutablePreferences.setPendingSaveState(runId: String, state: PendingSaveState): Boolean {
    val saves = readPendingSaves()
    if (saves.none { it.runId == runId }) return false
    writePendingSaves(saves.map { if (it.runId == runId) it.copy(state = state) else it })
    return true
}

internal fun MutablePreferences.removePendingSave(runId: String): Boolean {
    val saves = readPendingSaves()
    if (saves.none { it.runId == runId }) return false
    writePendingSaves(saves.filterNot { it.runId == runId })
    return true
}

/** 저장 대기 [runId]를 진행 중인 러닝(FINISHED)으로 되돌린다. 진행 중인 러닝이 있거나 목록에 없으면 null. */
internal fun MutablePreferences.restorePendingSave(runId: String): StoredRunSession? {
    if (readRunSession() != null) return null
    val saves = readPendingSaves()
    val target = saves.firstOrNull { it.runId == runId } ?: return null
    val restored = target.session.copy(status = StoredRunStatus.FINISHED)
    writePendingSaves(saves - target)
    writeRunSession(restored)
    return restored
}

/** 저장 대기 항목 한 줄. runId는 Firestore 문서 ID(영숫자)라 구분자 `;`를 쓰지 않는다. */
internal fun encodePendingSave(save: PendingRunSave): String = with(save.session) {
    listOf(runId, save.state.name, startedAtMs, pausedTotalMs, pausedAtMs?.toString().orEmpty(), lastHeartbeatMs, segment)
        .joinToString(PENDING_SEPARATOR)
}

internal fun decodePendingSave(line: String): PendingRunSave? {
    val parts = line.split(PENDING_SEPARATOR)
    if (parts.size != PENDING_FIELD_COUNT || parts[0].isBlank()) return null
    val state = PendingSaveState.entries.firstOrNull { it.name == parts[1] } ?: return null
    val startedAtMs = parts[2].toLongOrNull() ?: return null
    return PendingRunSave(
        StoredRunSession(
            runId = parts[0],
            status = StoredRunStatus.FINISHED,
            startedAtMs = startedAtMs,
            pausedTotalMs = parts[3].toLongOrNull() ?: 0,
            pausedAtMs = parts[4].toLongOrNull(),
            lastHeartbeatMs = parts[5].toLongOrNull() ?: startedAtMs,
            segment = parts[6].toIntOrNull() ?: 0,
        ),
        state,
    )
}

private const val PENDING_SEPARATOR = ";"
private const val PENDING_FIELD_COUNT = 7
