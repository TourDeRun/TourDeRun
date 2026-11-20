package com.tourderun.feature.run.ui

import com.tourderun.core.data.Clock
import com.tourderun.core.data.local.PendingSaveState
import com.tourderun.core.data.local.StoredRunStatus
import com.tourderun.domain.model.GpsPoint
import com.tourderun.feature.run.data.FakeRunSaver
import com.tourderun.feature.run.data.testCoordinator
import com.tourderun.feature.run.session.FakeActiveRunStore
import com.tourderun.feature.run.session.FakeRunPointDao
import com.tourderun.feature.run.session.RunPhase
import com.tourderun.feature.run.session.RunSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 앱을 다시 연 뒤 서버가 저장을 거부하면 앱을 다시 켜지 않아도 복구 안내가 뜬다(#99). */
class RunRecoveryControllerTest {
    private val dao = FakeRunPointDao()
    private val store = FakeActiveRunStore()
    private val saver = FakeRunSaver()
    private var nowMs = 200_000L
    private val clock = Clock { nowMs }
    private val scope = CoroutineScope(Dispatchers.Unconfined + Job())

    @After fun tearDown() = scope.cancel()

    /** 러닝 A(run1, 약 22m·21초)를 달리고 종료한 매니저. */
    private suspend fun finishedRunA(): RunSessionManager = RunSessionManager(dao, store).apply {
        beginCountdown("run1", 0)
        tick(3_000)
        onLocation(GpsPoint(0.0, 0.0, 4_000, 5f), 4_000)
        onLocation(GpsPoint(0.0, 0.0001, 13_000, 5f), 13_000)
        onLocation(GpsPoint(0.0, 0.0002, 22_000, 5f), 22_000)
        requireNotNull(finish(25_000))
    }

    private fun controller(manager: RunSessionManager) =
        RunRecoveryController(manager, clock, testCoordinator(manager, saver), scope).also { it.start() }

    /** M-05 기준(100m·1분)을 넘는 러닝 A. 저장 버튼이 보이려면 필요하다. */
    private suspend fun finishedLongRunA(): RunSessionManager = RunSessionManager(dao, store).apply {
        beginCountdown("run1", 0)
        tick(3_000)
        // 위도 0.0003°(약 33m)씩 30초 간격 → 약 133m, 약 2분
        for (i in 0..4) onLocation(GpsPoint(0.0003 * i, 0.0, 4_000L + i * 30_000L, 5f), 4_000L + i * 30_000L)
        requireNotNull(finish(130_000))
    }

    @Test fun `앱 재시작 때 A가 캐시에 있음 → 기다리는 중 서버 거부 → DataStore는 A 종료, 화면에 다시 저장·버리기`() = runBlocking {
        // 1) A를 오프라인에서 저장(저장 대기)하고 앱이 꺼진다.
        testCoordinator(finishedLongRunA(), saver).saveFinished()
        assertEquals(PendingSaveState.AWAITING, store.pendingState("run1"))

        // 2) 앱을 다시 연다. A는 기기 캐시에 있고 서버 응답을 기다린다 → 안내 없음.
        saver.savedOnDevice = true
        val restarted = RunSessionManager(dao, store)
        val controller = controller(restarted)
        yield()
        assertNull(controller.recovered.value)
        assertEquals(RunPhase.IDLE, restarted.state.value.phase)
        assertEquals(PendingSaveState.AWAITING, store.pendingState("run1"))

        // 3) 기다리는 중 서버가 거부한다(캐시에서 되돌려짐).
        saver.savedOnDevice = false
        saver.pendingWrites.complete(true)
        val run = withTimeout(1_000) { controller.recovered.first { it != null } }

        // DataStore: A가 원래 runId로 진행 중(종료) 러닝이 되고, 대기 목록에서 빠진다. 위치점은 그대로.
        assertEquals("run1", store.session.value?.runId)
        assertEquals(StoredRunStatus.FINISHED, store.session.value?.status)
        assertTrue(store.pendingSaves.value.isEmpty())
        assertEquals(5, dao.pointsForRun("run1").size)
        assertEquals(RunPhase.FINISHED, restarted.state.value.phase)

        // 화면: 거부 안내와 다시 저장·버리기 선택지.
        requireNotNull(run)
        assertEquals("run1", run.runId)
        assertTrue(run.saveRejected)
        assertEquals(listOf(RecoveryChoice.Save, RecoveryChoice.Discard), recoveryChoices(run))

        // 4) 다시 저장 → 결과 화면 → 원래 runId로 저장한다.
        controller.save()
        assertEquals(RecoveryOutcome.OpenResult, withTimeout(1_000) { controller.outcomeFlow.first() })
        assertNotNull(testCoordinator(restarted, saver).saveFinished())
        assertEquals("run1", saver.lastRunId)
        assertEquals(PendingSaveState.AWAITING, store.pendingState("run1"))
    }

    @Test fun `서버 거부 뒤 버리기 → A의 위치점과 기록을 지우고 새 러닝을 시작할 수 있다`() = runBlocking {
        testCoordinator(finishedRunA(), saver).saveFinished()
        val restarted = RunSessionManager(dao, store)
        // 캐시에 없음 → 바로 거부로 처리된다.
        val controller = controller(restarted)
        val run = requireNotNull(withTimeout(1_000) { controller.recovered.first { it != null } })
        // 약 22m라 저장할 수 없고 버리기만 남는다(M-05).
        assertEquals(listOf(RecoveryChoice.Discard), recoveryChoices(run))

        controller.discard()
        assertEquals(RecoveryOutcome.Discarded, withTimeout(1_000) { controller.outcomeFlow.first() })
        assertNull(store.session.value)
        assertTrue(store.pendingSaves.value.isEmpty())
        assertTrue(dao.pointsForRun("run1").isEmpty())
        assertTrue(restarted.beginCountdown("run3", nowMs))
    }

    @Test fun `B 측정 중 앱 재시작 → B 안내, A 거부는 보존하고 B를 저장한 뒤 A 안내`() = runBlocking {
        // A 저장 대기 → B 측정 중 앱이 꺼진다.
        val manager = finishedLongRunA()
        testCoordinator(manager, saver).saveFinished()
        manager.beginCountdown("run2", 140_000)
        manager.tick(143_000)
        manager.onLocation(GpsPoint(1.0, 1.0, 144_000, 5f), 144_000)

        // 다시 열면 B(일시정지)의 복구 안내가 먼저 뜬다. A는 캐시에 없어 거부로 보존된다.
        val restarted = RunSessionManager(dao, store)
        val controller = controller(restarted)
        val first = requireNotNull(withTimeout(1_000) { controller.recovered.first { it != null } })
        assertEquals("run2", first.runId)
        assertEquals(PendingSaveState.REJECTED, store.pendingState("run1"))
        assertEquals("run2", store.session.value?.runId)
        assertEquals(5, dao.pointsForRun("run1").size)
        assertEquals(1, dao.pointsForRun("run2").size)

        // B를 이어서 측정한다(앱이 꺼진 시각 이후). A는 그대로 보존된다.
        nowMs = 200_000L
        controller.resume()
        assertEquals(RecoveryOutcome.Resumed, withTimeout(1_000) { controller.outcomeFlow.first() })
        assertEquals(RunPhase.RUNNING, restarted.state.value.phase)
        assertNull(controller.recovered.value)
        assertEquals(PendingSaveState.REJECTED, store.pendingState("run1"))

        // B를 끝내고 저장(대기)하면 IDLE → A의 거부 안내가 뜬다.
        requireNotNull(restarted.finish(210_000))
        assertNotNull(testCoordinator(restarted, saver).saveFinished())
        val second = requireNotNull(withTimeout(1_000) { controller.recovered.first { it != null } })
        assertEquals("run1", second.runId)
        assertTrue(second.saveRejected)
        assertEquals(PendingSaveState.AWAITING, store.pendingState("run2"))
    }
}
