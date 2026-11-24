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

/** 저장 대기 러닝의 서버 반영 상태(#99). 이름으로 저장하므로 이름은 바꾸지 않는다. */
enum class PendingSaveState {
    /** REC-01 저장 배치를 시작했고 서버 확인을 기다리는 중. */
    AWAITING,

    /** 서버가 저장을 받아들이지 않았다. 사용자가 다시 저장하거나 버릴 때까지 보존한다. */
    REJECTED,
}

/**
 * 저장 배치를 시작한 러닝(#99). 진행 중인 러닝([ActiveRunStore.session])과 따로 보관해, 서버 확인 전에 새 러닝을 시작해도
 * 앱 재시작 뒤 위치점을 지키고, 서버가 거부하면 같은 runId·시각으로 복구할 수 있다.
 *
 * @property session 종료 시점의 진행 상태(status는 FINISHED). 복구할 때 그대로 진행 중인 러닝으로 되돌린다
 */
data class PendingRunSave(val session: StoredRunSession, val state: PendingSaveState) {
    val runId: String get() = session.runId
}

/** 진행 중인 러닝 한 개와 저장 대기 러닝 목록을 기기에 남긴다(RUN-06·07). */
interface ActiveRunStore {
    /** 진행 중(측정·일시정지·저장 전 종료)인 러닝. 없으면 null. */
    val session: Flow<StoredRunSession?>

    /** 저장 대기 러닝. 시작 시각 순. */
    val pendingSaves: Flow<List<PendingRunSave>>

    suspend fun save(session: StoredRunSession)

    /** 진행 중인 러닝이 [runId]면 지운다. 저장 대기 목록은 건드리지 않는다. */
    suspend fun clearIfRunIdMatches(runId: String)

    /** 진행 중인 러닝 [runId]를 저장 대기([PendingSaveState.AWAITING])로 옮긴다(한 번에). 진행 중인 러닝이 [runId]가 아니면 false. */
    suspend fun moveToPendingSave(runId: String): Boolean

    /** 저장 대기 [runId]의 상태를 바꾼다. 목록에 없으면 false. */
    suspend fun setPendingSaveState(runId: String, state: PendingSaveState): Boolean

    /** 저장 대기 [runId]를 지운다. 목록에 없으면 false. */
    suspend fun removePendingSave(runId: String): Boolean

    /**
     * 저장 대기 [runId]를 진행 중인 러닝으로 되돌린다(한 번에). 다시 저장하거나 버리기 위해서다.
     * 이미 진행 중인 러닝이 있거나 목록에 없으면 아무것도 하지 않고 null.
     */
    suspend fun restorePendingSave(runId: String): StoredRunSession?

    /** RUN-02·AUTH-05 탈퇴 뒤: 진행 중 러닝과 저장 대기 목록을 전부 지운다. */
    suspend fun clearAll()
}

@Singleton
class RunSessionStore @Inject constructor(@ApplicationContext private val context: Context) : ActiveRunStore {
    override val session: Flow<StoredRunSession?> = context.runSessionDataStore.data.map { it.readRunSession() }
    override val pendingSaves: Flow<List<PendingRunSave>> = context.runSessionDataStore.data.map { it.readPendingSaves() }

    override suspend fun save(session: StoredRunSession) {
        context.runSessionDataStore.edit { it.writeRunSession(session) }
    }

    override suspend fun clearIfRunIdMatches(runId: String) {
        context.runSessionDataStore.edit { it.clearRunSessionIfMatching(runId) }
    }

    override suspend fun moveToPendingSave(runId: String): Boolean {
        var moved = false
        context.runSessionDataStore.edit { moved = it.moveRunSessionToPendingSave(runId) }
        return moved
    }

    override suspend fun setPendingSaveState(runId: String, state: PendingSaveState): Boolean {
        var changed = false
        context.runSessionDataStore.edit { changed = it.setPendingSaveState(runId, state) }
        return changed
    }

    override suspend fun removePendingSave(runId: String): Boolean {
        var removed = false
        context.runSessionDataStore.edit { removed = it.removePendingSave(runId) }
        return removed
    }

    override suspend fun clearAll() {
        context.runSessionDataStore.edit { it.clear() }
    }

    override suspend fun restorePendingSave(runId: String): StoredRunSession? {
        var restored: StoredRunSession? = null
        context.runSessionDataStore.edit { restored = it.restorePendingSave(runId) }
        return restored
    }
}
