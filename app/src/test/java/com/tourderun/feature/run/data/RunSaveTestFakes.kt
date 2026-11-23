package com.tourderun.feature.run.data

import kotlinx.coroutines.flow.emptyFlow
import com.tourderun.core.data.session.MyStats
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.record.RunSaver
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.SessionState
import com.tourderun.core.data.session.SessionUser
import com.tourderun.core.data.team.MyTeam
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.model.TeamStanding
import com.tourderun.domain.record.SaveOutcome
import com.tourderun.feature.run.session.RunSessionManager
import dagger.Lazy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * 오프라인 대기 쓰기를 흉내 내는 [RunSaver].
 * - [savedOnDevice]: `runs/{runId}`가 기기 캐시에 있는지. 서버 거부는 이 값을 false로 바꾸고 [pendingWrites]를 끝내서 흉내 낸다.
 * - [pendingWrites]: `waitForPendingWrites`. 완료시키기 전까지 기다린다(오프라인).
 * - [reject]: 마지막 저장의 `onServerRejected`. 부르면 앱이 켜진 동안 서버 거부가 온 것. [rejectImmediately]면 저장 중에 바로 부른다.
 */
internal class FakeRunSaver : RunSaver {
    var saveCount = 0
    var lastUid: String? = null
    var lastRunId: String? = null
    var lastTeam: MyTeam? = null
    var failure: Throwable? = null
    var confirm: (() -> Unit)? = null
    var savedOnDevice = false
    var confirmImmediately = false
    var reject: (() -> Unit)? = null
    var rejectImmediately = false
    var pendingWrites = CompletableDeferred<Boolean>()

    override fun newRunId() = "run-new"

    override fun save(
        result: RunResult,
        uid: String,
        team: MyTeam?,
        title: String,
        onServerConfirmed: () -> Unit,
        onServerRejected: () -> Unit,
    ): Result<SaveOutcome?> {
        failure?.let { return Result.failure(it) }
        saveCount++
        lastUid = uid
        lastRunId = result.runId
        lastTeam = team
        confirm = onServerConfirmed
        reject = onServerRejected
        if (confirmImmediately) onServerConfirmed()
        if (rejectImmediately) onServerRejected()
        return Result.success(null)
    }

    override suspend fun isSavedOnDevice(runId: String) = savedOnDevice
    override suspend fun awaitPendingWrites() = pendingWrites.await()
}

internal class FakeSession(initial: SessionState) : SessionRepository {
    override val state: StateFlow<SessionState> = MutableStateFlow(initial)
    override suspend fun signInWithGoogle(idToken: String) = Result.success(Unit)
    override suspend fun createUser(nickname: String) = Result.success(Unit)
    override fun signOut() = Unit
    override fun updateNickname(uid: String, teamId: String?, nickname: String) = Result.success(Unit)
    override fun observeMyStats(uid: String) = emptyFlow<Loaded<MyStats>>()
}

internal class FixedTeamSource(private val team: MyTeam?) : MyTeamSource {
    override fun observeMyTeam(): Flow<MyTeam?> = flowOf(team)
}

/** 팀 값을 바꿀 수 있는 [MyTeamSource]. 기본은 값을 끝내 내지 않는다(팀 문서 첫 읽기가 실패해 멈춘 상태). */
internal class SwitchableTeamSource : MyTeamSource {
    var flow: Flow<MyTeam?> = flow { awaitCancellation() }
    override fun observeMyTeam(): Flow<MyTeam?> = flow
}

internal val testUser = SessionUser(uid = "u1", nickname = "러너")
internal val fixedTeam = MyTeam(1, TeamStanding("fake-my-team", "뛰뛰크루", 66_800, 0L))

/** 팀 문서를 기다리는 시간(테스트용, 실제 3초를 기다리지 않게). */
internal const val TEST_TEAM_WAIT_MS = 20L

/**
 * 서버 확인 뒤 정리를 바로 실행해 결과를 확인하는 조정자. 경고 로그는 [warnings]에 모은다.
 */
internal fun testCoordinator(
    manager: RunSessionManager,
    saver: FakeRunSaver,
    session: SessionState = SessionState.NoTeam(testUser),
    teamSource: MyTeamSource = FixedTeamSource(fixedTeam),
    warnings: MutableList<String> = mutableListOf(),
) = RunSaveCoordinator(
    manager = manager,
    runSaver = Lazy { saver },
    session = Lazy { FakeSession(session) },
    myTeamSource = teamSource,
    appScope = CoroutineScope(Dispatchers.Unconfined),
    teamWaitMs = TEST_TEAM_WAIT_MS,
    warn = { warnings += it },
)
