package com.tourderun.core.navigation

/** 앱을 열었을 때 사용자가 어디까지 와 있는지 (기능 명세 3장 "최초 진입 흐름"). */
enum class EntryStage {
    /** 로그인 안 됨 → S01 */
    SIGNED_OUT,

    /** 로그인했지만 `users` 문서(닉네임)가 없음 → S02 */
    NEEDS_NICKNAME,

    /** 닉네임은 있지만 소속 팀이 없음 → S03 */
    NEEDS_TEAM,

    /** 팀까지 있음 → 홈 */
    READY,
}

/** 앞 단계가 안 됐으면 뒤 단계 값은 보지 않는다(로그아웃 상태면 닉네임·팀이 남아 있어도 로그인부터). */
fun entryStage(signedIn: Boolean, hasNickname: Boolean, hasTeam: Boolean): EntryStage = when {
    !signedIn -> EntryStage.SIGNED_OUT
    !hasNickname -> EntryStage.NEEDS_NICKNAME
    !hasTeam -> EntryStage.NEEDS_TEAM
    else -> EntryStage.READY
}
