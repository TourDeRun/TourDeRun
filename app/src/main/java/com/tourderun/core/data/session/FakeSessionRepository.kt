package com.tourderun.core.data.session

import com.tourderun.core.data.Loaded
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 서버 없이 동작하는 가짜 세션. 처음에는 로그인·닉네임·팀까지 끝난 [SessionState.Ready]로 시작해서,
 * 팀원들이 uid·teamId가 필요한 화면을 바로 띄워 볼 수 있게 한다.
 *
 *
 */
@Singleton
class FakeSessionRepository @Inject constructor() : SessionRepository {

    private val _state = MutableStateFlow<SessionState>(
        SessionState.Ready(SessionUser(FAKE_UID, FAKE_NICKNAME), FAKE_TEAM_ID),
    )
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        _state.value = SessionState.NeedsNickname(FAKE_UID)
        return Result.success(Unit)
    }

    override suspend fun createUser(nickname: String): Result<Unit> {
        val current = _state.value
        if (current is SessionState.NeedsNickname) {
            _state.value = SessionState.NoTeam(SessionUser(uid = current.uid, nickname = nickname))
            return Result.success(Unit)
        }
        return Result.failure(IllegalStateException("닉네임을 정할 단계가 아님: $current"))
    }

    override fun signOut() {
        _state.value = SessionState.SignedOut
    }

    override fun updateNickname(uid: String, teamId: String?, nickname: String): Result<Unit> {
        _state.value = when (val current = _state.value) {
            is SessionState.NoTeam -> current.copy(user = current.user.copy(nickname = nickname))
            is SessionState.Ready -> current.copy(user = current.user.copy(nickname = nickname))
            else -> return Result.failure(IllegalStateException("닉네임을 바꿀 수 없는 상태: $current"))
        }
        return Result.success(Unit)
    }

    override fun observeMyStats(uid: String): Flow<Loaded<MyStats>> =
        flowOf(Loaded.Ready(MyStats(totalM = 86_400, runCount = 21), isFromCache = false))

    companion object {
        const val FAKE_UID = "fake-uid"
        const val FAKE_NICKNAME = "개발자"
        // MyTeamSource의 고정 팀과 맞춘다
        const val FAKE_TEAM_ID = "fake-my-team"
    }
}
