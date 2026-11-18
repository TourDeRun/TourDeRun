package com.tourderun.feature.run.session

import com.tourderun.core.data.local.ActiveRunStore
import com.tourderun.core.data.local.RunPointDao
import com.tourderun.core.data.local.RunPointEntity
import com.tourderun.core.data.local.StoredRunSession
import com.tourderun.domain.model.GpsPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RunSessionManagerTest {
    private class FakeDao : RunPointDao {
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
    private class FakeStore : ActiveRunStore {
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
    private fun point(lng: Double, timeMs: Long) = GpsPoint(0.0, lng, timeMs, 5f)

    @Test fun `일시정지 구간은 더하지 않고 각 구간 첫 점을 경로에 포함`() = runBlocking {
        val manager = RunSessionManager(FakeDao(), FakeStore())
        manager.beginCountdown("run1", 0)
        manager.onLocation(point(-0.1, 1_000), 1_000)
        manager.tick(3_000)
        val first = point(0.0, 4_000)
        manager.onLocation(first, 4_000)
        manager.onLocation(point(0.0001, 13_000), 13_000)
        manager.pause(23_000)
        manager.resume(83_000)
        val restarted = point(1.0, 84_000)
        manager.onLocation(restarted, 84_000)
        manager.onLocation(point(1.0001, 94_000), 94_000)
        val result = requireNotNull(manager.finish(103_000))
        assertEquals(40L, result.movingSec)
        assertEquals(22.2, result.distanceM, 0.2)
        assertEquals(first, result.acceptedPoints.first())
        assertTrue(restarted in result.acceptedPoints)
        assertEquals(4, result.acceptedPoints.size)
    }

    @Test fun `카운트다운 중 위치는 러닝 경로와 거리에 포함하지 않는다`() = runBlocking {
        val manager = RunSessionManager(FakeDao(), FakeStore())
        manager.beginCountdown("run1", 0)
        manager.onLocation(point(0.0, 1_000), 1_000)
        manager.tick(3_000)
        val first = point(1.0, 4_000)
        manager.onLocation(first, 4_000)
        assertEquals(listOf(first), manager.state.value.acceptedPoints)
        assertEquals(0.0, manager.state.value.distanceM, 0.0)
    }

    @Test fun `일시정지 뒤 종료하면 종료 시각은 유지하고 정지 시간은 제외`() = runBlocking {
        val manager = RunSessionManager(FakeDao(), FakeStore())
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        manager.onLocation(point(0.0, 4_000), 4_000)
        manager.pause(13_000)
        val result = requireNotNull(manager.finish(43_000))
        assertEquals(43_000L, result.endedAtMs)
        assertEquals(10L, result.movingSec)
    }

    @Test fun `위치 저장이 실패하면 실제 저장된 점으로 거리를 다시 계산한다`() = runBlocking {
        val dao = FakeDao()
        val manager = RunSessionManager(dao, FakeStore())
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        manager.onLocation(point(0.0, 4_000), 4_000)
        dao.failNext = true
        try {
            manager.onLocation(point(0.0001, 13_000), 13_000)
            fail("저장 실패가 전달되어야 한다")
        } catch (_: IllegalStateException) { }
        manager.tick(14_000)
        assertEquals(0.0, manager.state.value.distanceM, 0.0)
        manager.onLocation(point(0.0001, 15_000), 15_000)
        assertEquals(11.1, manager.state.value.distanceM, 0.2)
    }

    @Test fun `저장 결과를 알 수 없을 때는 종료 전에 저장된 점을 다시 읽는다`() = runBlocking {
        val dao = FakeDao()
        val manager = RunSessionManager(dao, FakeStore())
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        manager.onLocation(point(0.0, 4_000), 4_000)
        dao.failAfterInsert = true
        try {
            manager.onLocation(point(0.0001, 13_000), 13_000)
            fail("저장 결과 확인 실패가 전달되어야 한다")
        } catch (_: IllegalStateException) { }
        val result = requireNotNull(manager.finish(14_000))
        assertEquals(11.1, result.distanceM, 0.2)
        assertEquals(2, result.acceptedPoints.size)
    }

    @Test fun `재개 직후 저장에 실패해도 이전 구간과 거리를 잇지 않는다`() = runBlocking {
        val dao = FakeDao()
        val manager = RunSessionManager(dao, FakeStore())
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        manager.onLocation(point(0.0, 4_000), 4_000)
        manager.onLocation(point(0.0001, 14_000), 14_000)
        manager.pause(24_000)
        manager.resume(84_000)
        dao.failNext = true
        try {
            manager.onLocation(point(1.0, 85_000), 85_000)
            fail("저장 실패가 전달되어야 한다")
        } catch (_: IllegalStateException) { }
        manager.onLocation(point(1.0, 86_000), 86_000)
        manager.onLocation(point(1.0001, 96_000), 96_000)
        val result = requireNotNull(manager.finish(100_000))
        assertEquals(22.2, result.distanceM, 0.2)
        assertEquals(4, result.acceptedPoints.size)
    }

    @Test fun `미래 시각과 이전 시각의 위치는 기록하지 않는다`() = runBlocking {
        val dao = FakeDao()
        val manager = RunSessionManager(dao, FakeStore())
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        manager.onLocation(point(1.0, 5_000), 4_000)
        manager.onLocation(point(0.0, 4_000), 4_000)
        manager.onLocation(point(2.0, 3_999), 5_000)
        assertEquals(listOf(point(0.0, 4_000)), manager.state.value.acceptedPoints)
        assertEquals(1, dao.pointsForRun("run1").size)
    }

    @Test fun `종료 이전 시각으로 다시 시작하지 않는다`() = runBlocking {
        val manager = RunSessionManager(FakeDao(), FakeStore())
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        manager.pause(13_000)
        manager.finish(43_000)
        assertFalse(manager.resume(20_000))
        assertTrue(manager.resume(53_000))
        assertEquals(10L, manager.state.value.movingSec)
    }

    @Test fun `상태 저장 중 호출이 취소돼도 저장된 일시정지와 화면 상태가 일치한다`() = runBlocking {
        val store = FakeStore()
        val manager = RunSessionManager(FakeDao(), store)
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        val saved = CompletableDeferred<Unit>()
        val completeSave = CompletableDeferred<Unit>()
        store.savedSignal = saved
        store.completeSave = completeSave
        val pauseJob = launch { manager.pause(13_000) }
        saved.await()
        pauseJob.cancel()
        completeSave.complete(Unit)
        pauseJob.join()
        assertEquals(RunPhase.PAUSED, manager.state.value.phase)
        assertEquals(com.tourderun.core.data.local.StoredRunStatus.PAUSED, store.session.value?.status)
    }

    @Test fun `명시적으로 버릴 때만 해당 러닝을 지운다`() = runBlocking {
        val dao = FakeDao()
        val store = FakeStore()
        val manager = RunSessionManager(dao, store)
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        manager.onLocation(point(0.0, 4_000), 4_000)
        assertFalse(manager.discard("run1"))
        manager.finish(5_000)
        assertFalse(manager.discard("run2"))
        dao.failDelete = true
        try {
            manager.discard("run1")
            fail("삭제 실패가 전달되어야 한다")
        } catch (_: IllegalStateException) { }
        assertEquals("run1", store.session.value?.runId)
        assertEquals(1, dao.pointsForRun("run1").size)
        dao.failDelete = false
        assertTrue(manager.discard("run1"))
        assertNull(store.session.value)
        assertTrue(dao.pointsForRun("run1").isEmpty())
    }

}
