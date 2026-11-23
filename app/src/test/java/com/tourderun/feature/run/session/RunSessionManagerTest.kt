package com.tourderun.feature.run.session

import com.tourderun.core.data.local.ActiveRunStore
import com.tourderun.core.data.local.PendingSaveState
import com.tourderun.core.data.local.RunPointDao
import com.tourderun.core.data.local.RunPointEntity
import com.tourderun.core.data.local.StoredRunSession
import com.tourderun.core.data.local.StoredRunStatus
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

private typealias FakeDao = FakeRunPointDao
private typealias FakeStore = FakeActiveRunStore

class RunSessionManagerTest {
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


    @Test fun `저장 확인 후 completeSave하면 기기 데이터를 지우고 다음 러닝을 시작할 수 있다`() = runBlocking {
        val dao = FakeDao()
        val store = FakeStore()
        val manager = RunSessionManager(dao, store)
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        manager.onLocation(point(0.0, 4_000), 4_000)
        requireNotNull(manager.finish(10_000))
        assertFalse(manager.completeSave("other"))
        assertTrue(manager.completeSave("run1"))
        assertEquals(RunPhase.IDLE, manager.state.value.phase)
        assertTrue(dao.pointsForRun("run1").isEmpty())
        assertNull(store.session.value)
        assertTrue(manager.beginCountdown("run2", 20_000))
    }

    /** run1(러닝 A)을 달리고 종료한 뒤 저장을 시작(저장 대기)한 매니저. */
    private suspend fun savedRun1(dao: FakeDao, store: FakeStore): RunSessionManager {
        val manager = RunSessionManager(dao, store)
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        manager.onLocation(point(0.0, 4_000), 4_000)
        requireNotNull(manager.finish(10_000))
        assertTrue(manager.markSaveStarted("run1"))
        return manager
    }

    /** [savedRun1] 뒤 run2(러닝 B)를 시작해 점 하나를 받은 상태. */
    private suspend fun RunSessionManager.startRun2() {
        assertTrue(beginCountdown("run2", 20_000))
        tick(23_000)
        onLocation(point(1.0, 24_000), 24_000)
    }

    @Test fun `저장 시작 → IDLE, 진행 상태는 비고 저장 대기 목록에 A와 위치점이 남는다`() = runBlocking {
        val dao = FakeDao()
        val store = FakeStore()
        val manager = savedRun1(dao, store)
        assertEquals(RunPhase.IDLE, manager.state.value.phase)
        assertNull(store.session.value)
        assertEquals(PendingSaveState.AWAITING, store.pendingState("run1"))
        // 복구에 필요한 시각도 같이 보존한다.
        assertEquals(10_000L, store.pendingSaves.value.single().session.pausedAtMs)
        assertEquals(1, dao.pointsForRun("run1").size)
        assertFalse(manager.markSaveStarted("run1"))
    }

    @Test fun `종료 전이거나 다른 러닝이면 저장 시작 표시를 하지 않는다`() = runBlocking {
        val store = FakeStore()
        val manager = RunSessionManager(FakeDao(), store)
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        assertFalse(manager.markSaveStarted("run1"))
        manager.finish(10_000)
        assertFalse(manager.markSaveStarted("run2"))
        assertEquals(StoredRunStatus.FINISHED, store.session.value?.status)
        assertTrue(store.pendingSaves.value.isEmpty())
    }

    @Test fun `저장하지 않은 종료 러닝이 있으면 새 러닝을 시작할 수 없다`() = runBlocking {
        val store = FakeStore()
        RunSessionManager(FakeDao(), store).apply {
            beginCountdown("run1", 0)
            tick(3_000)
            finish(10_000)
        }
        // 앱을 다시 켠 것처럼 메모리 상태는 IDLE
        assertFalse(RunSessionManager(FakeDao(), store).beginCountdown("run2", 20_000))
    }

    @Test fun `저장 대기 중(서버 확인 전)에도 새 러닝을 시작할 수 있다`() = runBlocking {
        val dao = FakeDao()
        val store = FakeStore()
        val manager = savedRun1(dao, store)
        manager.startRun2()
        assertEquals("run2", store.session.value?.runId)
        assertEquals(RunPhase.RUNNING, manager.state.value.phase)
        assertEquals(PendingSaveState.AWAITING, store.pendingState("run1"))
        assertEquals(1, dao.pointsForRun("run1").size)
    }

    @Test fun `B 측정 중 A 서버 확인 → A만 지우고 B의 상태와 경로는 그대로`() = runBlocking {
        val dao = FakeDao()
        val store = FakeStore()
        val manager = savedRun1(dao, store)
        manager.startRun2()
        assertTrue(manager.completeSave("run1"))
        assertTrue(dao.pointsForRun("run1").isEmpty())
        assertTrue(store.pendingSaves.value.isEmpty())
        assertEquals(1, dao.pointsForRun("run2").size)
        assertEquals("run2", store.session.value?.runId)
        assertEquals(RunPhase.RUNNING, manager.state.value.phase)
        assertFalse(manager.completeSave("run1"))
    }

    @Test fun `저장 대기 중 서버 확인 → 위치점과 대기 기록을 지운다`() = runBlocking {
        val dao = FakeDao()
        val store = FakeStore()
        val manager = savedRun1(dao, store)
        assertTrue(manager.completeSave("run1"))
        assertTrue(dao.pointsForRun("run1").isEmpty())
        assertTrue(store.pendingSaves.value.isEmpty())
        assertNull(store.session.value)
        assertEquals(RunPhase.IDLE, manager.state.value.phase)
    }

    @Test fun `A 오프라인 저장 → B 측정 시작 → 앱 재시작 → A·B 위치점 모두 남고, A 서버 확인 뒤 A만 지운다`() = runBlocking {
        val dao = FakeDao()
        val store = FakeStore()
        val manager = savedRun1(dao, store)
        manager.startRun2()
        // 측정 중(IDLE 아님)에는 정리하지 않는다.
        manager.pruneOrphanPoints()

        // 앱이 꺼졌다가 다시 켜짐: A는 아직 서버 확인을 받지 못했다.
        val restarted = RunSessionManager(dao, store)
        restarted.pruneOrphanPoints()
        assertEquals(1, dao.pointsForRun("run1").size)
        assertEquals(1, dao.pointsForRun("run2").size)
        assertEquals(PendingSaveState.AWAITING, store.pendingState("run1"))
        assertEquals("run2", store.session.value?.runId)

        assertEquals("run2", requireNotNull(restarted.recover()).runId)
        assertTrue(restarted.completeSave("run1"))
        assertTrue(dao.pointsForRun("run1").isEmpty())
        assertEquals(1, dao.pointsForRun("run2").size)
        assertEquals("run2", store.session.value?.runId)
        assertEquals(RunPhase.PAUSED, restarted.state.value.phase)
    }

    @Test fun `진행 중·저장 대기 어디에도 없는 러닝의 위치점만 정리한다`() = runBlocking {
        val dao = FakeDao()
        val store = FakeStore()
        savedRun1(dao, store)
        dao.insert(RunPointEntity(runId = "old", segment = 0, lat = 0.0, lng = 0.0, timeMs = 1, accuracyM = 5f, accepted = true))
        RunSessionManager(dao, store).pruneOrphanPoints()
        assertTrue(dao.pointsForRun("old").isEmpty())
        assertEquals(1, dao.pointsForRun("run1").size)
    }

    @Test fun `진행 상태와 저장 대기가 모두 없으면 남은 위치점을 모두 정리한다`() = runBlocking {
        val dao = FakeDao()
        dao.insert(RunPointEntity(runId = "old", segment = 0, lat = 0.0, lng = 0.0, timeMs = 1, accuracyM = 5f, accepted = true))
        RunSessionManager(dao, FakeStore()).pruneOrphanPoints()
        assertTrue(dao.pointsForRun("old").isEmpty())
    }

    @Test fun `거부된 A는 IDLE일 때 원래 runId·시각으로 종료 상태에 올린다`() = runBlocking {
        val dao = FakeDao()
        val store = FakeStore()
        savedRun1(dao, store)
        val restarted = RunSessionManager(dao, store)
        assertNull(restarted.pendingRecovery())
        assertNull(restarted.recoverRejected())
        assertTrue(restarted.rejectPendingSave("run1"))

        val run = requireNotNull(restarted.recoverRejected())
        assertEquals("run1", run.runId)
        assertTrue(run.finished)
        assertTrue(run.saveRejected)
        assertEquals(RunPhase.FINISHED, restarted.state.value.phase)
        assertEquals(10_000L, restarted.state.value.result?.endedAtMs)
        assertEquals(StoredRunStatus.FINISHED, store.session.value?.status)
        assertEquals("run1", store.session.value?.runId)
        assertTrue(store.pendingSaves.value.isEmpty())
        // 올린 러닝을 저장하거나 버리기 전에는 새 러닝을 시작할 수 없다.
        assertFalse(restarted.beginCountdown("run3", 30_000))
    }

    @Test fun `B 측정 중 A 거부 → B는 계속, A는 거부로 보존했다가 B를 끝낸 뒤 올린다`() = runBlocking {
        val dao = FakeDao()
        val store = FakeStore()
        val manager = savedRun1(dao, store)
        manager.startRun2()
        assertTrue(manager.rejectPendingSave("run1"))
        assertNull(manager.recoverRejected())
        assertEquals(RunPhase.RUNNING, manager.state.value.phase)
        assertEquals(PendingSaveState.REJECTED, store.pendingState("run1"))
        assertEquals(1, dao.pointsForRun("run1").size)

        // B를 끝내고 저장을 시작하면(대기) IDLE이 되어 A를 올릴 수 있다. B의 대기 기록은 그대로다.
        manager.finish(30_000)
        assertTrue(manager.markSaveStarted("run2"))
        assertEquals("run1", requireNotNull(manager.recoverRejected()).runId)
        assertEquals(PendingSaveState.AWAITING, store.pendingState("run2"))
    }

    @Test fun `올린 A를 버리면 A만 지우고 저장 대기 B는 그대로`() = runBlocking {
        val dao = FakeDao()
        val store = FakeStore()
        val manager = savedRun1(dao, store)
        manager.startRun2()
        manager.finish(30_000)
        manager.markSaveStarted("run2")
        manager.rejectPendingSave("run1")
        requireNotNull(manager.recoverRejected())

        assertTrue(manager.discard("run1"))
        assertTrue(dao.pointsForRun("run1").isEmpty())
        assertNull(store.session.value)
        assertEquals(PendingSaveState.AWAITING, store.pendingState("run2"))
        assertEquals(1, dao.pointsForRun("run2").size)
    }

    @Test fun `종료 전에는 completeSave가 동작하지 않는다`() = runBlocking {
        val manager = RunSessionManager(FakeDao(), FakeStore())
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        assertFalse(manager.completeSave("run1"))
        assertEquals(RunPhase.RUNNING, manager.state.value.phase)
    }

    @Test fun `채택된 점이 10초 동안 없으면 GPS 약함, 점이 들어오면 해제`() = runBlocking {
        val manager = RunSessionManager(FakeDao(), FakeStore())
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        manager.tick(12_000)
        assertFalse(manager.state.value.gpsWeak)
        manager.tick(13_000)
        assertTrue(manager.state.value.gpsWeak)
        manager.onLocation(point(0.0, 13_500), 13_500)
        assertFalse(manager.state.value.gpsWeak)
        manager.pause(14_000)
        manager.tick(40_000)
        assertFalse(manager.state.value.gpsWeak)
    }

    @Test fun `앱이 꺼진 뒤 복구하면 거리를 다시 계산하고 꺼진 시간은 일시정지로 본다`() = runBlocking {
        val dao = FakeDao()
        val store = FakeStore()
        val before = RunSessionManager(dao, store)
        before.beginCountdown("run1", 0)
        before.tick(3_000)
        before.onLocation(point(0.0, 4_000), 4_000)
        before.onLocation(point(0.0001, 14_000), 14_000)
        // 프로세스가 죽고 새 매니저가 같은 기기 저장소를 읽는다.
        val after = RunSessionManager(dao, store)
        val pending = requireNotNull(after.pendingRecovery())
        assertEquals("run1", pending.runId)
        val recovered = requireNotNull(after.recover())
        assertEquals(11.1, recovered.distanceM, 0.2)
        assertEquals(14_000L, recovered.endMs)
        assertEquals(11L, recovered.movingSec)
        assertFalse(recovered.finished)
        assertEquals(RunPhase.PAUSED, after.state.value.phase)

        // 이어하기: 꺼져 있던 14초~100초는 움직인 시간에서 빠진다.
        assertTrue(after.resume(100_000))
        after.onLocation(point(1.0, 101_000), 101_000)
        val result = requireNotNull(after.finish(111_000))
        assertEquals(22L, result.movingSec)
        assertEquals(11.1, result.distanceM, 0.2)
    }

    @Test fun `복구 후 지금까지 저장하면 마지막으로 살아 있던 시각에 끝난다`() = runBlocking {
        val dao = FakeDao()
        val store = FakeStore()
        val before = RunSessionManager(dao, store)
        before.beginCountdown("run1", 0)
        before.tick(3_000)
        before.onLocation(point(0.0, 4_000), 4_000)
        before.onLocation(point(0.0001, 14_000), 14_000)
        val after = RunSessionManager(dao, store)
        val recovered = requireNotNull(after.recover())
        val result = requireNotNull(after.finish(recovered.endMs))
        assertEquals(14_000L, result.endedAtMs)
        assertEquals(2, result.acceptedPoints.size)
    }

    @Test fun `복구 후 버리면 기기 데이터를 지운다`() = runBlocking {
        val dao = FakeDao()
        val store = FakeStore()
        val before = RunSessionManager(dao, store)
        before.beginCountdown("run1", 0)
        before.tick(3_000)
        before.onLocation(point(0.0, 4_000), 4_000)
        val after = RunSessionManager(dao, store)
        requireNotNull(after.recover())
        assertTrue(after.discard("run1"))
        assertNull(store.session.value)
        assertTrue(dao.pointsForRun("run1").isEmpty())
        assertNull(after.pendingRecovery())
    }

    @Test fun `측정 중인 프로세스에서는 복구 안내를 띄우지 않는다`() = runBlocking {
        val manager = RunSessionManager(FakeDao(), FakeStore())
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        assertNull(manager.pendingRecovery())
        assertNull(manager.recover())
    }

    @Test fun `서비스 실패면 측정 중은 일시정지로 돌아간다`() = runBlocking {
        val manager = RunSessionManager(FakeDao(), FakeStore())
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        assertTrue(manager.stopForServiceFailure(5_000))
        assertEquals(RunPhase.PAUSED, manager.state.value.phase)
    }

    @Test fun `서비스 실패면 카운트다운은 취소돼 IDLE이 된다`() = runBlocking {
        val manager = RunSessionManager(FakeDao(), FakeStore())
        manager.beginCountdown("run1", 0)
        assertTrue(manager.stopForServiceFailure(1_000))
        assertEquals(RunPhase.IDLE, manager.state.value.phase)
    }

    @Test fun `서비스 실패여도 종료된 러닝은 결과를 그대로 둔다`() = runBlocking {
        val manager = RunSessionManager(FakeDao(), FakeStore())
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        manager.onLocation(point(0.0, 4_000), 4_000)
        val result = requireNotNull(manager.finish(10_000))
        assertFalse(manager.stopForServiceFailure(11_000))
        assertEquals(RunPhase.FINISHED, manager.state.value.phase)
        assertEquals(result, manager.state.value.result)
    }
}
