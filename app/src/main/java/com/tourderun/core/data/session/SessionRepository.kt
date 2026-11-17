package com.tourderun.core.data.session

import kotlinx.coroutines.flow.StateFlow

/**
 * 로그인 상태와 내 `users` 문서 (AUTH-01, AUTH-02). 담당: 권민찬.
 * 다른 영역은 [state]만 보고 uid·닉네임·teamId를 얻는다. FirebaseAuth를 직접 부르지 않는다.
 */
interface SessionRepository {

    /** 지금 세션 상태. 로그인·`users` 문서가 바뀌면 새 값이 나온다. */
    val state: StateFlow<SessionState>

    /** S01: Credential Manager가 준 구글 ID 토큰으로 로그인한다. 온라인 필수. */
    suspend fun signInWithGoogle(idToken: String): Result<Unit>

    /**
     * S02: 첫 로그인 때 `users/{uid}` 문서를 만든다. 약관 동의 시각(`termsAgreedAt`, D-08)도 같이 기록한다.
     * 닉네임 검사는 부르는 쪽이 `validateNickname`으로 먼저 한다. 온라인 필수.
     */
    suspend fun createUser(nickname: String): Result<Unit>

    /** AUTH-04 로그아웃. */
    fun signOut()
}
