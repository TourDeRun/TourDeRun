package com.tourderun.domain.team

/** 팀 최대 인원 (TEAM-03). */
const val TEAM_MAX_MEMBERS = 4

/** 초대 코드로 참여할 수 없는 이유 (TEAM-03). 없는 코드·오프라인은 Repository가 따로 알린다. */
enum class JoinRejection {
    ALREADY_IN_TEAM,  // 이미 팀이 있다 → "지금 팀에서 나온 뒤 참여할 수 있어요"
    FULL,             // 인원 4명 → "인원이 가득 찼어요"
}

/**
 * 참여할 수 있는지. 되면 null.
 * 이미 팀이 있으면 인원과 상관없이 [JoinRejection.ALREADY_IN_TEAM]을 먼저 알린다.
 *
 * @param myTeamId 내 `users.teamId` (없으면 null)
 * @param memberCount 들어가려는 팀의 지금 인원
 */
fun joinRejection(myTeamId: String?, memberCount: Int): JoinRejection? {
    return when {
        myTeamId != null -> JoinRejection.ALREADY_IN_TEAM
        memberCount >= TEAM_MAX_MEMBERS -> JoinRejection.FULL
        else -> null
    }
}
