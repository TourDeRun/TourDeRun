package com.tourderun.feature.run.data

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

    @Test fun `같은 러닝을 두 번 저장 → 저장은 1번, 두 번째는 처음 값`() = runBlocking {
        val coordinator = coordinator(finishedManager())
        val first = coordinator.saveFinished()
        val second = coordinator.saveFinished()
        assertEquals(1, saver.saveCount)
        assertNotNull(first)
        assertEquals(first, second)
        assertEquals("u1", saver.lastUid)
    }

    @Test fun `서버 확인 전에는 completeSave 안 함, 확인되면 기기 데이터를 지우고 IDLE`() = runBlocking {
        val manager = finishedManager()
        coordinator(manager).saveFinished()
        assertEquals(RunPhase.FINISHED, manager.state.value.phase)
        assertNotNull(store.session.value)

        requireNotNull(saver.confirm).invoke()
        yield()
        assertEquals(RunPhase.IDLE, manager.state.value.phase)
        assertNull(store.session.value)
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

    @Test fun `앱 재시작, 기기 캐시에 없음 → false, 아무것도 안 함`() = runBlocking {
        finishedManager()
        val restarted = RunSessionManager(dao, store)
        restarted.recover()
        assertFalse(coordinator(restarted).settleIfSavedOnDevice("run1"))
        assertEquals(RunPhase.FINISHED, restarted.state.value.phase)
        assertEquals(0, saver.saveCount)
    }
}
