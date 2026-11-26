package com.tourderun.feature.run.service

import com.tourderun.core.data.Clock
import com.tourderun.domain.model.GpsPoint
import com.tourderun.feature.run.session.FakeActiveRunStore
import com.tourderun.feature.run.session.FakeRunPointDao
import com.tourderun.feature.run.session.RunPhase
import com.tourderun.feature.run.session.RunSessionManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class RunTrackingControllerTest {
    /** [throwOnRequest]가 있으면 요청이 바로 예외를 던지고, 없으면 [failLater]로 실패한 Task를 흉내 낸다. */
    private class FakeLocationSource(private val throwOnRequest: Exception? = null) : LocationSource {
        private var onFailure: ((Exception) -> Unit)? = null
        var removed = 0

        override fun request(onLocations: (List<GpsPoint>) -> Unit, onFailure: (Exception) -> Unit) {
            throwOnRequest?.let { throw it }
            this.onFailure = onFailure
        }

        override fun remove() {
            removed++
        }

        fun failLater(error: Exception) = requireNotNull(onFailure) { "request 전" }(error)
    }

    private val clock = Clock { 5_000L }

    private suspend fun runningManager() = RunSessionManager(FakeRunPointDao(), FakeActiveRunStore()).apply {
        beginCountdown("run1", 0)
        tick(3_000)
    }

    @Test fun `위치 요청이 바로 예외를 던지면 일시정지로 돌아가고 위치 요청과 서비스를 정리한다`() = runBlocking {
        val manager = runningManager()
        val source = FakeLocationSource(throwOnRequest = SecurityException("위치 권한 없음"))
        val stopped = CompletableDeferred<Unit>()
        RunTrackingController(manager, clock, source, this, onStop = { stopped.complete(Unit) }).start()

        withTimeout(1_000) { stopped.await() }
        assertEquals(RunPhase.PAUSED, manager.state.value.phase)
        assertEquals(1, source.removed)
    }

    @Test fun `위치 요청 Task가 나중에 실패해도 일시정지로 돌아가고 서비스를 정리한다`() = runBlocking {
        val manager = runningManager()
        val source = FakeLocationSource()
        val stopped = CompletableDeferred<Unit>()
        RunTrackingController(manager, clock, source, this, onStop = { stopped.complete(Unit) }).start()
        assertEquals(RunPhase.RUNNING, manager.state.value.phase)

        source.failLater(IllegalStateException("위치 서비스 오류"))
        withTimeout(1_000) { stopped.await() }
        assertEquals(RunPhase.PAUSED, manager.state.value.phase)
        assertEquals(1, source.removed)
    }

    @Test fun `카운트다운 중 위치 요청이 실패하면 IDLE로 돌아간다`() = runBlocking {
        val manager = RunSessionManager(FakeRunPointDao(), FakeActiveRunStore()).apply { beginCountdown("run1", 0) }
        val stopped = CompletableDeferred<Unit>()
        RunTrackingController(manager, clock, FakeLocationSource(SecurityException("위치 권한 없음")), this, onStop = { stopped.complete(Unit) })
            .start()

        withTimeout(1_000) { stopped.await() }
        assertEquals(RunPhase.IDLE, manager.state.value.phase)
    }

    @Test fun `실패가 두 번 와도 정리는 한 번만 한다`() = runBlocking {
        val manager = runningManager()
        val source = FakeLocationSource()
        var stops = 0
        val controller = RunTrackingController(manager, clock, source, this, onStop = { stops++ })
        controller.start()

        source.failLater(IllegalStateException("첫 실패"))
        controller.fail(IllegalStateException("startForeground 실패"))
        // launch된 정리 작업이 끝날 때까지 기다린다.
        withTimeout(1_000) { while (stops == 0) yield() }
        yield()
        assertEquals(1, stops)
        assertEquals(1, source.removed)
    }
}
