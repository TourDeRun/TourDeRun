package com.tourderun.core.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.runSessionDataStore by preferencesDataStore(name = "active_run")

enum class StoredRunStatus { RUNNING, PAUSED, FINISHED }

data class StoredRunSession(
    val runId: String,
    val status: StoredRunStatus,
    val startedAtMs: Long,
    val pausedTotalMs: Long,
    val pausedAtMs: Long?,
    val lastHeartbeatMs: Long,
    val segment: Int = 0,
)

interface ActiveRunStore {
    val session: Flow<StoredRunSession?>
    suspend fun save(session: StoredRunSession)
    suspend fun clearIfRunIdMatches(runId: String)
}

@Singleton
class RunSessionStore @Inject constructor(@ApplicationContext private val context: Context) : ActiveRunStore {
    private object Keys {
        val id = stringPreferencesKey("run_id")
        val status = stringPreferencesKey("status")
        val started = longPreferencesKey("started_at_ms")
        val pausedTotal = longPreferencesKey("paused_total_ms")
        val pausedAt = longPreferencesKey("paused_at_ms")
        val heartbeat = longPreferencesKey("last_heartbeat_ms")
        val segment = intPreferencesKey("segment")
    }

    override val session: Flow<StoredRunSession?> = context.runSessionDataStore.data.map { prefs ->
        val id = prefs[Keys.id] ?: return@map null
        val started = prefs[Keys.started] ?: return@map null
        val status = prefs[Keys.status]?.let { value -> StoredRunStatus.entries.firstOrNull { it.name == value } }
            ?: return@map null
        StoredRunSession(
            runId = id,
            status = status,
            startedAtMs = started,
            pausedTotalMs = prefs[Keys.pausedTotal] ?: 0,
            pausedAtMs = prefs[Keys.pausedAt],
            lastHeartbeatMs = prefs[Keys.heartbeat] ?: started,
            segment = prefs[Keys.segment] ?: 0,
        )
    }

    override suspend fun save(session: StoredRunSession) {
        context.runSessionDataStore.edit { prefs ->
            prefs[Keys.id] = session.runId
            prefs[Keys.status] = session.status.name
            prefs[Keys.started] = session.startedAtMs
            prefs[Keys.pausedTotal] = session.pausedTotalMs
            if (session.pausedAtMs == null) prefs.remove(Keys.pausedAt) else prefs[Keys.pausedAt] = session.pausedAtMs
            prefs[Keys.heartbeat] = session.lastHeartbeatMs
            prefs[Keys.segment] = session.segment
        }
    }

    override suspend fun clearIfRunIdMatches(runId: String) {
        context.runSessionDataStore.edit { prefs -> if (prefs[Keys.id] == runId) prefs.clear() }
    }
}
