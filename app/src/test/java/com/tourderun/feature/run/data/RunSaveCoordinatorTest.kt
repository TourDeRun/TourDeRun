package com.tourderun.feature.run.data

import com.tourderun.core.data.local.PendingSaveState
import com.tourderun.core.data.session.SessionState
import com.tourderun.domain.model.GpsPoint
import com.tourderun.feature.run.session.FakeActiveRunStore
import com.tourderun.feature.run.session.FakeRunPointDao
import com.tourderun.feature.run.session.RunPhase
import com.tourderun.feature.run.session.RunSessionManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 저장 조정자: 한 러닝은 한 번만 저장하고, 서버 확인 뒤에만 기기 데이터를 지운다(#23, #99). */
class RunSaveCoordinatorTest {

    private val dao = FakeRunPointDao()
    private val store = FakeActiveRunStore()
    private val saver = FakeRunSaver()

    private fun coordinator(manager: RunSessionManager, session: SessionState = SessionState.NoTeam(testUser)) =
        testCoordinator(manager, saver, session)

    /** 약 22m를 달리고 종료한 매니저(러닝 A = run1). */
    private suspend fun finishedManager(): RunSessionManager {
        val manager = RunSessionManager(dao, store)
        manager.beginCountdown("run1", 0)
        manager.tick(3_000)
        manager.onLocation(GpsPoint(0.0, 0.0, 4_000, 5f), 4_000)
        manager.onLocation(GpsPoint(0.0, 0.0001, 13_000, 5f), 13_000)
        manager.onLocation(GpsPoint(0.0, 0.0002, 22_000, 5f), 22_000)
        requireNotNull(manager.finish(25_000))
        return manager
    }

    @Test fun `같은 러닝을 두 번 저장 → 저장은 1번`() = runBlocking {
        val coordinator = coordinator(finishedManager())
        assertNotNull(coordinator.saveFinished())
        // 저장을 시작하면 IDLE로 넘어가 두 번째는 저장할 러닝이 없다.
        assertNull(coordinator.saveFinished())
        assertEquals(1, saver.saveCount)
        assertEquals("u1", saver.lastUid)
    }

    @Test fun `저장 시작 → IDLE과 저장 대기, 서버 확인 전에는 위치점을 남기고 확인되면 지운다`() = runBlocking {
        val manager = finishedManager()
        coordinator(manager).saveFinished()
        assertEquals(RunPhase.IDLE, manager.state.value.phase)
        assertNull(store.session.value)
        assertEquals(PendingSaveState.AWAITING, store.pendingState("run1"))
        assertEquals(3, dao.pointsForRun("run1").size)

        requireNotNull(saver.confirm).invoke()
        yield()
        assertTrue(store.pendingSaves.value.isEmpty())
        assertTrue(dao.pointsForRun("run1").isEmpty())
    }

    @Test fun `서버 확인 전(오프라인)에도 새 러닝을 시작할 수 있고, 확인되면 A만 지운다(#99)`() = runBlocking {
        val manager = finishedManager()
        coordinator(manager).saveFinished()
        assertTrue(manager.beginCountdown("run2", 30_000))
        manager.tick(33_000)
        manager.onLocation(GpsPoint(1.0, 1.0, 34_000, 5f), 34_000)
        requireNotNull(saver.confirm).invoke()
        yield()
        assertTrue(dao.pointsForRun("run1").isEmpty())
        assertEquals(1, dao.pointsForRun("run2").size)
        assertEquals("run2", store.session.value?.runId)
        assertEquals(RunPhase.RUNNING, manager.state.value.phase)
    }

    @Test fun `서버 확인이 저장 대기 표시보다 먼저 와도 정리된다`() = runBlocking {
        val manager = finishedManager()
        saver.confirmImmediately = true
        assertNotNull(coordinator(manager).saveFinished())
        assertEquals(RunPhase.IDLE, manager.state.value.phase)
        assertNull(store.session.value)
        assertTrue(store.pendingSaves.value.isEmpty())
        assertTrue(dao.pointsForRun("run1").isEmpty())
    }

    @Test fun `저장 실패 → null, 종료 상태 그대로`() = runBlocking {
        val manager = finishedManager()
        saver.failure = IllegalArgumentException("거리가 0m")
        assertNull(coordinator(manager).saveFinished())
        assertEquals(RunPhase.FINISHED, manager.state.value.phase)
        assertNotNull(store.session.value)
        assertTrue(store.pendingSaves.value.isEmpty())
    }

    @Test fun `종료된 러닝이 없음 → 저장하지 않고 null`() = runBlocking {
        assertNull(coordinator(RunSessionManager(dao, store)).saveFinished())
        assertEquals(0, saver.saveCount)
    }

    @Test fun `로그아웃 상태 → 저장하지 않고 null`() = runBlocking {
        assertNull(coordinator(finishedManager(), SessionState.SignedOut).saveFinished())
        assertEquals(0, saver.saveCount)
    }

    @Test fun `세션의 팀과 고정 팀이 다름 → 팀 없이 저장(fake-my-team으로 저장하지 않음)`() = runBlocking {
        coordinator(finishedManager(), SessionState.Ready(testUser, teamId = "t1")).saveFinished()
        assertEquals(1, saver.saveCount)
        assertNull(saver.lastTeam)
    }

    // --- 저장 배치 시작 직후(저장 대기로 옮기기 전) 앱이 꺼진 종료 러닝 ---

    @Test fun `앱 재시작, 종료 러닝이 기기 캐시에 있음 → 다시 저장 안 하고 저장 대기로 옮긴 뒤 반영되면 정리`() = runBlocking {
        finishedManager()
        // 앱을 다시 켠 것처럼 같은 기기 저장소로 새 매니저를 만들고 복구한다.
        val restarted = RunSessionManager(dao, store)
        assertTrue(requireNotNull(restarted.recover()).finished)
        saver.savedOnDevice = true
        val coordinator = coordinator(restarted)

        assertTrue(coordinator.settleIfSavedOnDevice("run1"))
        assertEquals(RunPhase.IDLE, restarted.state.value.phase)
        assertEquals(PendingSaveState.AWAITING, store.pendingState("run1"))
        assertNull(coordinator.saveFinished())
        assertEquals(0, saver.saveCount)

        saver.pendingWrites.complete(true)
        yield()
        assertTrue(store.pendingSaves.value.isEmpty())
        assertNull(store.session.value)
        assertTrue(dao.pointsForRun("run1").isEmpty())
    }

    @Test fun `앱 재시작, 종료 러닝이 기기 캐시에 없음 → false, 아무것도 안 함`() = runBlocking {
        finishedManager()
        val restarted = RunSessionManager(dao, store)
        restarted.recover()
        assertFalse(coordinator(restarted).settleIfSavedOnDevice("run1"))
        assertEquals(RunPhase.FINISHED, restarted.state.value.phase)
        assertEquals(0, saver.saveCount)
    }

    // --- 저장 대기 러닝(앱 재시작) ---

    /** A를 저장 대기로 넘긴 뒤 서버 확인 전에 앱이 꺼지고 다시 켜진 매니저. */
    private suspend fun restartedAfterSaveStarted(): RunSessionManager {
        coordinator(finishedManager()).saveFinished()
        return RunSessionManager(dao, store)
    }

    @Test fun `앱 재시작, 저장 대기 A가 캐시에 있음 → 반영 뒤 정리`() = runBlocking {
        val restarted = restartedAfterSaveStarted()
        saver.savedOnDevice = true
        assertTrue(coordinator(restarted).settleSaved("run1"))
        assertEquals(PendingSaveState.AWAITING, store.pendingState("run1"))
        saver.pendingWrites.complete(true)
        yield()
        assertTrue(store.pendingSaves.value.isEmpty())
        assertTrue(dao.pointsForRun("run1").isEmpty())
        assertEquals(1, saver.saveCount)
    }

    @Test fun `앱 재시작, 저장 대기 A가 캐시에 있음 → 기다리는 중 서버 거부 → 거부로 보존하고 원래 runId로 다시 저장`() = runBlocking {
        val restarted = restartedAfterSaveStarted()
        saver.savedOnDevice = true
        val coordinator = coordinator(restarted)
        assertTrue(coordinator.settleSaved("run1"))

        saver.savedOnDevice = false
        saver.pendingWrites.complete(true)
        yield()
        assertEquals(PendingSaveState.REJECTED, store.pendingState("run1"))
        assertEquals(3, dao.pointsForRun("run1").size)

        requireNotNull(restarted.recoverRejected())
        assertNotNull(coordinator.saveFinished())
        assertEquals(2, saver.saveCount)
        assertEquals("run1", saver.lastRunId)
        assertEquals(PendingSaveState.AWAITING, store.pendingState("run1"))
    }

    @Test fun `앱 재시작, 저장 대기 A가 캐시에 없음 → false, 거부로 보존`() = runBlocking {
        val restarted = restartedAfterSaveStarted()
        assertFalse(coordinator(restarted).settleSaved("run1"))
        assertEquals(PendingSaveState.REJECTED, store.pendingState("run1"))
        assertEquals(3, dao.pointsForRun("run1").size)
    }

    @Test fun `저장 뒤 서버 거부 → 거부로 보존하고 같은 runId로 다시 저장을 시작한다`() = runBlocking {
        val manager = finishedManager()
        val coordinator = coordinator(manager)
        coordinator.saveFinished()
        assertEquals(PendingSaveState.AWAITING, store.pendingState("run1"))

        requireNotNull(saver.reject).invoke()
        yield()
        assertEquals(PendingSaveState.REJECTED, store.pendingState("run1"))
        assertEquals(3, dao.pointsForRun("run1").size)

        requireNotNull(manager.recoverRejected())
        assertNotNull(coordinator.saveFinished())
        assertEquals(2, saver.saveCount)
        assertEquals("run1", saver.lastRunId)
        assertEquals(PendingSaveState.AWAITING, store.pendingState("run1"))
    }

    @Test fun `서버 거부가 저장 대기 표시보다 먼저 와도 거부로 보존된다`() = runBlocking {
        val manager = finishedManager()
        saver.rejectImmediately = true
        assertNotNull(coordinator(manager).saveFinished())
        yield()
        assertEquals(RunPhase.IDLE, manager.state.value.phase)
        assertEquals(PendingSaveState.REJECTED, store.pendingState("run1"))
        assertEquals(3, dao.pointsForRun("run1").size)
    }

    @Test fun `서버 확인이 온 러닝은 거부로 바뀌지 않는다`() = runBlocking {
        val manager = finishedManager()
        coordinator(manager).saveFinished()
        requireNotNull(saver.confirm).invoke()
        yield()
        // Task는 성공·실패 중 하나만 부르지만, 늦은 거부가 와도 정리한 러닝을 되살리지 않는지 본다.
        requireNotNull(saver.reject).invoke()
        yield()
        assertTrue(store.pendingSaves.value.isEmpty())
        assertNull(store.pendingState("run1"))
        assertTrue(dao.pointsForRun("run1").isEmpty())
        assertNull(manager.recoverRejected())
        assertEquals(1, saver.saveCount)
    }
}
