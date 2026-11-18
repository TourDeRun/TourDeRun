package com.tourderun.feature.run.data

import com.tourderun.core.data.local.StoredRunStatus
import com.tourderun.core.data.record.RunSaver
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.SessionState
import com.tourderun.core.data.session.SessionUser
import com.tourderun.core.data.team.MyTeam
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.domain.model.GpsPoint
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.model.TeamStanding
import com.tourderun.domain.record.SaveOutcome
import com.tourderun.feature.run.session.FakeActiveRunStore
import com.tourderun.feature.run.session.FakeRunPointDao
import com.tourderun.feature.run.session.RunPhase
import com.tourderun.feature.run.session.RunSessionManager
import dagger.Lazy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 저장 조정자: 한 러닝은 한 번만 저장하고, 서버 확인 뒤에만 기기 데이터를 지운다(#23). */
class RunSaveCoordinatorTest {

    private class FakeRunSaver : RunSaver {
        var saveCount = 0
        var lastUid: String? = null
        var lastTeam: MyTeam? = null
        var failure: Throwable? = null
        var confirm: (() -> Unit)? = null
        var savedOnDevice = false
        var confirmImmediately = false
        val pendingWrites = CompletableDeferred<Boolean>()

        override fun newRunId() = "run-new"

        override fun save(
            result: RunResult,
            uid: String,
            team: MyTeam?,
            title: String,
            onServerConfirmed: () -> Unit,
        ): Result<SaveOutcome?> {
            failure?.let { return Result.failure(it) }
            saveCount++
            lastUid = uid
            lastTeam = team
            confirm = onServerConfirmed
            if (confirmImmediately) onServerConfirmed()
            return Result.success(null)
        }

        override suspend fun isSavedOnDevice(runId: String) = savedOnDevice
        override suspend fun awaitPendingWrites() = pendingWrites.await()
    }

    private class FakeSession(initial: SessionState) : SessionRepository {
        override val state: StateFlow<SessionState> = MutableStateFlow(initial)
        override suspend fun signInWithGoogle(idToken: String) = Result.success(Unit)
        override suspend fun createUser(nickname: String) = Result.success(Unit)
        override fun signOut() = Unit
    }

    private class FixedTeamSource(private val team: MyTeam?) : MyTeamSource {
        override fun observeMyTeam(): Flow<MyTeam?> = flowOf(team)
    }

    private val user = SessionUser(uid = "u1", nickname = "러너")
    private val fixedTeam = MyTeam(1, TeamStanding("fake-my-team", "뛰뛰크루", 66_800, 0L))
    private val dao = FakeRunPointDao()
    private val store = FakeActiveRunStore()
    private val saver = FakeRunSaver()

    private fun coordinator(
        manager: RunSessionManager,
        session: SessionState = SessionState.NoTeam(user),
    ) = RunSaveCoordinator(
        manager = manager,
        runSaver = Lazy { saver },
        session = Lazy { FakeSession(session) },
        myTeamSource = FixedTeamSource(fixedTeam),
        // 서버 확인 뒤 정리를 바로 실행해 결과를 확인한다.
        appScope = CoroutineScope(Dispatchers.Unconfined),
    )

    /** 약 22m를 달리고 종료한 매니저. */
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

    @Test fun `저장 시작 → IDLE과 SAVED, 서버 확인 전에는 위치점을 남기고 확인되면 지운다`() = runBlocking {
        val manager = finishedManager()
        coordinator(manager).saveFinished()
        assertEquals(RunPhase.IDLE, manager.state.value.phase)
        assertEquals(StoredRunStatus.SAVED, store.session.value?.status)
        assertEquals(3, dao.pointsForRun("run1").size)

        requireNotNull(saver.confirm).invoke()
        yield()
        assertNull(store.session.value)
        assertTrue(dao.pointsForRun("run1").isEmpty())
    }

    @Test fun `서버 확인 전(오프라인)에도 새 러닝을 시작할 수 있다(#99)`() = runBlocking {
        val manager = finishedManager()
        coordinator(manager).saveFinished()
        assertTrue(manager.beginCountdown("run2", 30_000))
        manager.tick(33_000)
        requireNotNull(saver.confirm).invoke()
        yield()
        assertTrue(dao.pointsForRun("run1").isEmpty())
        assertEquals("run2", store.session.value?.runId)
        assertEquals(RunPhase.RUNNING, manager.state.value.phase)
    }

    @Test fun `서버 확인이 저장 시작 표시보다 먼저 와도 정리된다`() = runBlocking {
        val manager = finishedManager()
        saver.confirmImmediately = true
        assertNotNull(coordinator(manager).saveFinished())
        assertEquals(RunPhase.IDLE, manager.state.value.phase)
        assertNull(store.session.value)
        assertTrue(dao.pointsForRun("run1").isEmpty())
    }

    @Test fun `저장 실패 → null, completeSave 안 함`() = runBlocking {
        val manager = finishedManager()
        saver.failure = IllegalArgumentException("거리가 0m")
        assertNull(coordinator(manager).saveFinished())
        assertEquals(RunPhase.FINISHED, manager.state.value.phase)
        assertNotNull(store.session.value)
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
        coordinator(finishedManager(), SessionState.Ready(user, teamId = "t1")).saveFinished()
        assertEquals(1, saver.saveCount)
        assertNull(saver.lastTeam)
    }

    @Test fun `앱 재시작, 기기 캐시에 이미 있음 → 다시 저장 안 하고 쓰기 반영 뒤 completeSave`() = runBlocking {
        finishedManager()
        // 앱을 다시 켠 것처럼 같은 기기 저장소로 새 매니저를 만들고 복구한다.
        val restarted = RunSessionManager(dao, store)
        assertTrue(requireNotNull(restarted.recover()).finished)
        saver.savedOnDevice = true
        val coordinator = coordinator(restarted)

        assertTrue(coordinator.settleIfSavedOnDevice("run1"))
        assertEquals(RunPhase.FINISHED, restarted.state.value.phase)
        // 결과 화면을 다시 열어도 저장하지 않는다.
        assertNotNull(coordinator.saveFinished())
        assertEquals(0, saver.saveCount)

        saver.pendingWrites.complete(true)
        yield()
        assertEquals(RunPhase.IDLE, restarted.state.value.phase)
        assertNull(store.session.value)
    }

    @Test fun `앱 재시작, 캐시에 있었지만 반영 뒤 사라짐(서버 거부) → completeSave 안 하고 다시 저장할 수 있음`() = runBlocking {
        finishedManager()
        val restarted = RunSessionManager(dao, store)
        restarted.recover()
        saver.savedOnDevice = true
        val coordinator = coordinator(restarted)
        assertTrue(coordinator.settleIfSavedOnDevice("run1"))
        saver.savedOnDevice = false
        saver.pendingWrites.complete(true)
        yield()
        assertEquals(RunPhase.FINISHED, restarted.state.value.phase)
        assertEquals(3, dao.pointsForRun("run1").size)
        // 결과 보기에서 다시 저장한다.
        assertNotNull(coordinator.saveFinished())
        assertEquals(1, saver.saveCount)
    }

    /** 저장을 시작(SAVED)한 뒤 서버 확인 전에 앱이 꺼지고 다시 켜진 매니저. */
    private suspend fun restartedAfterSaveStarted(): RunSessionManager {
        coordinator(finishedManager()).saveFinished()
        return RunSessionManager(dao, store)
    }

    @Test fun `앱 재시작, 저장 대기 러닝이 캐시에 있음 → 반영 뒤 기기 데이터 정리`() = runBlocking {
        val restarted = restartedAfterSaveStarted()
        saver.savedOnDevice = true
        assertTrue(coordinator(restarted).settleSaved("run1"))
        assertEquals(StoredRunStatus.SAVED, store.session.value?.status)
        saver.pendingWrites.complete(true)
        yield()
        assertNull(store.session.value)
        assertTrue(dao.pointsForRun("run1").isEmpty())
        assertEquals(1, saver.saveCount)
    }

    @Test fun `앱 재시작, 저장 대기 러닝이 반영 뒤 사라짐(서버 거부) → 종료 상태로 되돌림`() = runBlocking {
        val restarted = restartedAfterSaveStarted()
        saver.savedOnDevice = true
        assertTrue(coordinator(restarted).settleSaved("run1"))
        saver.savedOnDevice = false
        saver.pendingWrites.complete(true)
        yield()
        assertEquals(StoredRunStatus.FINISHED, store.session.value?.status)
        assertEquals(3, dao.pointsForRun("run1").size)
    }

    @Test fun `앱 재시작, 저장 대기 러닝이 캐시에 없음 → false, 종료 상태로 되돌려 복구 안내`() = runBlocking {
        val restarted = restartedAfterSaveStarted()
        assertFalse(coordinator(restarted).settleSaved("run1"))
        assertEquals(StoredRunStatus.FINISHED, store.session.value?.status)
        assertTrue(requireNotNull(restarted.recover()).finished)
    }

    @Test fun `앱 재시작, 기기 캐시에 없음 → false, 아무것도 안 함`() = runBlocking {
        finishedManager()
        val restarted = RunSessionManager(dao, store)
        restarted.recover()
        assertFalse(coordinator(restarted).settleIfSavedOnDevice("run1"))
        assertEquals(RunPhase.FINISHED, restarted.state.value.phase)
        assertEquals(0, saver.saveCount)
    }
}
