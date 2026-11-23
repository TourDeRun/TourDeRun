package com.tourderun.core.data.firestore

import com.google.firebase.firestore.DocumentSnapshot

/**
 * `teams/{teamId}/members/{uid}` 필드 → 본 도착 수(JRN-05 `seenArrivals`).
 *
 * - 문서가 없으면([fields]가 null) null: 내 팀 소속이 확인되지 않았으므로 연출을 띄우지 않는다.
 * - 필드가 없거나 숫자가 아니면 0(팀 만들기 기본값과 같다). 음수는 0으로 올린다.
 */
fun seenArrivalsOf(fields: Map<String, Any?>?): Int? {
    if (fields == null) return null
    val value = (fields[MemberFields.SEEN_ARRIVALS] as? Number)?.toLong() ?: 0L
    return value.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
}

/** members 문서 스냅샷 → 본 도착 수. 없는 문서면 null. */
fun DocumentSnapshot.seenArrivals(): Int? = seenArrivalsOf(if (exists()) data.orEmpty() else null)

/**
 * S14 팀원 한 명 (TEAM-04). `teams/{teamId}/members/{uid}` → 화면에 필요한 값만.
 *
 * @property joinedAtMs 가입 시각. 서버 시각이 아직 없으면(방금 참여) 기기 추정값, 그것도 없으면 0
 */
data class TeamMemberDto(
    val uid: String,
    val nickname: String,
    val isLeader: Boolean,
    val joinedAtMs: Long,
    val contributionM: Long,
)

/** members 문서 → [TeamMemberDto]. 닉네임이 없으면 null(목록에서 뺀다). */
fun DocumentSnapshot.toTeamMemberDto(): TeamMemberDto? {
    val nickname = getString(MemberFields.NICKNAME) ?: return null
    return TeamMemberDto(
        uid = id,
        nickname = nickname,
        isLeader = getString(MemberFields.ROLE) == MemberFields.ROLE_LEADER,
        joinedAtMs = getTimestamp(MemberFields.JOINED_AT, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: 0L,
        contributionM = getLong(MemberFields.CONTRIBUTION_M) ?: 0L,
    )
}
