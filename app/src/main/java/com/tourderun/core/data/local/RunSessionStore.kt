package com.tourderun.core.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
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

    /** 서버 저장 후 정리할 때는 서버 반영 성공을 확인한 뒤 호출한다. */
    suspend fun clearIfRunIdMatches(runId: String)
}

@Singleton
class RunSessionStore @Inject constructor(@ApplicationContext private val context: Context) : ActiveRunStore {
    override val session: Flow<StoredRunSession?> = context.runSessionDataStore.data.map { it.readRunSession() }

    override suspend fun save(session: StoredRunSession) {
        context.runSessionDataStore.edit { it.writeRunSession(session) }
    }

    override suspend fun clearIfRunIdMatches(runId: String) {
        context.runSessionDataStore.edit { it.clearRunSessionIfMatching(runId) }
    }
}
