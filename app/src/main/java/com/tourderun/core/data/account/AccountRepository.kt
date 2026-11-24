package com.tourderun.core.data.account

/**
 * 계정 (AUTH-05 회원 탈퇴). 담당: 권민찬.
 * uid·teamId는 부르는 ViewModel이 `SessionRepository.state`에서 읽어 넘긴다.
 */
interface AccountRepository {

    /**
     * 회원 탈퇴. 온라인 필수, 순서대로 하다가 실패하면 멈춘다(앞 단계는 되돌리지 않는다).
     *
     * 1. 구글 재인증([idToken]) — Firebase는 오래된 로그인으로 계정 삭제를 막는다
     * 2. 팀이 있으면 나가기(TEAM-07 `leaveTeam`: 팀장 승계·마지막 1명이면 팀 삭제). 팀 거리·기여·주간 통계는 그대로(M-01·M-02)
     * 3. 내 `runs` 삭제(경로 좌표 포함 개인 기록)
     * 4. 내 `users` 삭제
     * 5. Firebase 계정 삭제 → 세션이 SignedOut이 된다
     */
    suspend fun withdraw(uid: String, teamId: String?, idToken: String): Result<Unit>
}
