package com.tourderun.core.data.session

/** 로그인한 사용자. 닉네임은 `users/{uid}` 문서의 값이다. */
data class SessionUser(
    val uid: String,
    val nickname: String,
)

/**
 * 앱 진입 분기 (기술명세 3-4). 전병국의 `EntryStage`가 이 값으로 계산된다.
 *
 * - [Loading]       → 아직 확인 중 (앱 시작 직후, 서버 응답 전)
 * - [SignedOut]     → S01 로그인
 * - [NeedsNickname] → S01-1 약관 → S02 닉네임
 * - [NoTeam]        → S03 팀 시작
 * - [Ready]         → 홈
 */
sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class NeedsNickname(val uid: String) : SessionState
    data class NoTeam(val user: SessionUser) : SessionState
    data class Ready(val user: SessionUser, val teamId: String) : SessionState
}

/** 로그인했으면 uid, 아직 모르거나 로그아웃이면 null. RunSaver·RunRepository가 쓴다. */
val SessionState.uid: String?
    get() = when (this) {
        SessionState.Loading -> null
        SessionState.SignedOut -> null
        is SessionState.NeedsNickname -> uid
        is SessionState.NoTeam -> user.uid
        is SessionState.Ready -> user.uid
    }

/** 팀이 있으면 teamId, 없으면 null. */
val SessionState.teamId: String?
    get() = when (this) {
        is SessionState.Ready -> teamId
        else -> null
    }
