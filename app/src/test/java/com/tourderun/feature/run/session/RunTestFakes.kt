package com.tourderun.feature.run.session

import com.tourderun.core.data.local.ActiveRunStore
import com.tourderun.core.data.local.RunPointDao
import com.tourderun.core.data.local.RunPointEntity
import com.tourderun.core.data.local.StoredRunSession
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
}
/** 메모리에만 저장하는 진행 중 러닝 상태. */
internal class FakeActiveRunStore : ActiveRunStore {
    override val session = MutableStateFlow<StoredRunSession?>(null)
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
}
