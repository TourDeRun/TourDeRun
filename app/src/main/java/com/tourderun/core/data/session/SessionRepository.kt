package com.tourderun.core.data.session

import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 로그인 상태와 내 `users` 문서 (AUTH-01, AUTH-02). 담당: 권민찬.
 * 다른 영역은 [state]만 보고 uid·닉네임·teamId를 얻는다. FirebaseAuth를 직접 부르지 않는다.
 */
interface SessionRepository {

    /** 지금 세션 상태. 로그인·`users` 문서가 바뀌면 새 값이 나온다. */
    val state: StateFlow<SessionState>

    /**
     * `users` 문서 읽기 실패(권한 거절·오프라인 첫 진입 등). 정상이면 null.
     * 실패해도 [state]는 마지막 값을 유지한다(처음이면 Loading). 진입 로딩 화면이 이 값으로 다시 시도를 보여 준다.
     */
    val loadError: StateFlow<DataError?> get() = NO_LOAD_ERROR

    /** [loadError] 뒤 `users` 리스너를 다시 붙인다. */
    fun retry() {}

    /** S01: Credential Manager가 준 구글 ID 토큰으로 로그인한다. 온라인 필수. */
    suspend fun signInWithGoogle(idToken: String): Result<Unit>

    /**
     * S02: 첫 로그인 때 `users/{uid}` 문서를 만든다. 약관 동의 시각(`termsAgreedAt`, D-08)도 같이 기록한다.
     * 닉네임 검사는 부르는 쪽이 `validateNickname`으로 먼저 한다. 온라인 필수.
     */
    suspend fun createUser(nickname: String): Result<Unit>

    /** AUTH-04 로그아웃. */
    fun signOut()

    /**
     * AUTH-03 닉네임 바꾸기([nicknameUpdatePlan]). 검사는 부르는 쪽이 `validateNickname`으로 먼저 한다.
     * 오프라인 대기 쓰기라 끝날 때까지 기다리지 않는다. 기기 캐시에 바로 반영돼 [state]의 닉네임이 바뀐다.
     * 배치를 시작하지 못하면(잘못된 경로 등) failure.
     */
    fun updateNickname(uid: String, teamId: String?, nickname: String): Result<Unit>

    /** S13 내 누적 거리·러닝 횟수 (`users.totalM`·`runCount`) 실시간. */
    fun observeMyStats(uid: String): Flow<Loaded<MyStats>>
}

/** S13 위쪽 숫자. */
data class MyStats(val totalM: Long, val runCount: Long) {
    companion object {
        val EMPTY = MyStats(totalM = 0, runCount = 0)
    }
}

/** 실패를 내지 않는 구현(가짜·테스트)의 [SessionRepository.loadError] 기본값. */
private val NO_LOAD_ERROR: StateFlow<DataError?> = MutableStateFlow<DataError?>(null).asStateFlow()
