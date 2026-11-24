package com.tourderun.domain.team

/** 팀원 한 명의 가입 정보 (팀장 승계 판단용). */
data class MemberJoin(val uid: String, val joinedAtMs: Long)

/**
 * TEAM-07 팀장이 나갈 때 다음 팀장 (M-04 기본안: 가장 먼저 가입한 팀원).
 * 같은 시각이면 uid 순. 나가는 사람 말고 아무도 없으면 null(마지막 1명).
 *
 * @param members 지금 팀원 전부(나가는 사람 포함 가능)
 * @param leavingUid 나가는 팀장
 */
fun nextLeader(members: List<MemberJoin>, leavingUid: String): String? =
    members
        .filter { it.uid != leavingUid }
        .minWithOrNull(compareBy<MemberJoin> { it.joinedAtMs }.thenBy { it.uid })
        ?.uid
