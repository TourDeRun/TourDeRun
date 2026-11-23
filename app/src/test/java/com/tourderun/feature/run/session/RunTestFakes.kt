package com.tourderun.feature.run.session

import com.tourderun.core.data.local.ActiveRunStore
import com.tourderun.core.data.local.PendingRunSave
import com.tourderun.core.data.local.PendingSaveState
import com.tourderun.core.data.local.RunPointDao
import com.tourderun.core.data.local.RunPointEntity
import com.tourderun.core.data.local.StoredRunSession
import com.tourderun.core.data.local.StoredRunStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** 메모리에만 저장하는 RUN-06 위치점 Dao. 실패 주입 플래그로 저장 오류를 흉내 낸다. */
internal class FakeRunPointDao : RunPointDao {
    private val rows = MutableStateFlow<List<RunPointEntity>>(emptyList())
    var failNext = false
    var failAfterInsert = false
    var failDelete = false
    override suspend fun insert(point: RunPointEntity): Long {
        if (failNext) { failNext = false; throw IllegalStateException("저장 실패") }
        val id = (rows.value.lastOrNull()?.id ?: 0) + 1
        rows.value = rows.value + point.copy(id = id)
        if (failAfterInsert) { failAfterInsert = false; throw IllegalStateException("저장 결과 알 수 없음") }
        return id
    }
    override suspend fun pointsForRun(runId: String) = rows.value.filter { it.runId == runId }
    override suspend fun acceptedPointsForRun(runId: String) = rows.value.filter { it.runId == runId && it.accepted }
    override fun observePointsForRun(runId: String): Flow<List<RunPointEntity>> = rows.map { list -> list.filter { it.runId == runId } }
    override suspend fun deleteForRun(runId: String) {
        if (failDelete) throw IllegalStateException("삭제 실패")
        rows.value = rows.value.filterNot { it.runId == runId }
    }
    override suspend fun deleteExcept(runIds: List<String>) {
        rows.value = rows.value.filter { it.runId in runIds }
    }
    override suspend fun deleteAll() {
        rows.value = emptyList()
    }
}
/** 메모리에만 저장하는 진행 중 러닝 상태와 저장 대기 목록. 실제 저장소(`RunSessionStore`)와 같은 규칙으로 동작한다. */
internal class FakeActiveRunStore : ActiveRunStore {
    override val session = MutableStateFlow<StoredRunSession?>(null)
    override val pendingSaves = MutableStateFlow<List<PendingRunSave>>(emptyList())

    /** 저장 대기 [runId]의 상태. 없으면 null. */
    fun pendingState(runId: String): PendingSaveState? = pendingSaves.value.firstOrNull { it.runId == runId }?.state

    var savedSignal: CompletableDeferred<Unit>? = null
    var completeSave: CompletableDeferred<Unit>? = null
    override suspend fun save(session: StoredRunSession) {
        this.session.value = session
        savedSignal?.complete(Unit)
        completeSave?.await()
    }
    override suspend fun clearIfRunIdMatches(runId: String) {
        if (session.value?.runId == runId) session.value = null
    }
    override suspend fun moveToPendingSave(runId: String): Boolean {
        val active = session.value?.takeIf { it.runId == runId } ?: return false
        pendingSaves.value = (pendingSaves.value.filterNot { it.runId == runId } +
            PendingRunSave(active.copy(status = StoredRunStatus.FINISHED), PendingSaveState.AWAITING))
            .sortedBy { it.session.startedAtMs }
        session.value = null
        return true
    }
    override suspend fun setPendingSaveState(runId: String, state: PendingSaveState): Boolean {
        if (pendingSaves.value.none { it.runId == runId }) return false
        pendingSaves.value = pendingSaves.value.map { if (it.runId == runId) it.copy(state = state) else it }
        return true
    }
    override suspend fun removePendingSave(runId: String): Boolean {
        if (pendingSaves.value.none { it.runId == runId }) return false
        pendingSaves.value = pendingSaves.value.filterNot { it.runId == runId }
        return true
    }
    override suspend fun restorePendingSave(runId: String): StoredRunSession? {
        if (session.value != null) return null
        val target = pendingSaves.value.firstOrNull { it.runId == runId } ?: return null
        pendingSaves.value = pendingSaves.value - target
        return target.session.copy(status = StoredRunStatus.FINISHED).also { session.value = it }
    }
}
