package com.tourderun.core.data.firestore

import com.google.firebase.firestore.DocumentSnapshot

/**
 * `teamStats/{teamId}_{periodId}` 문서 중 주간 통계가 읽는 필드. 거리는 정수 m.
 *
 * @property memberM 팀원별 거리(uid → m). 나간 팀원 몫도 남아 있다
 */
data class TeamStatDto(
    val distanceM: Long,
    val runCount: Long,
    val memberM: Map<String, Long>,
) {
    companion object {
        /** 그 주에 러닝이 없어 문서가 없을 때. */
        val EMPTY = TeamStatDto(distanceM = 0, runCount = 0, memberM = emptyMap())
    }
}

/** 문서 → [TeamStatDto]. 문서나 필드가 없으면 0으로 채운다. */
fun DocumentSnapshot.toTeamStatDto(): TeamStatDto {
    if (!exists()) return TeamStatDto.EMPTY
    val memberM = (get(TeamStatFields.MEMBER_M) as? Map<*, *>)
        .orEmpty()
        .mapNotNull { (uid, m) -> if (uid is String && m is Number) uid to m.toLong() else null }
        .toMap()
    return TeamStatDto(
        distanceM = getLong(TeamStatFields.DISTANCE_M) ?: 0,
        runCount = getLong(TeamStatFields.RUN_COUNT) ?: 0,
        memberM = memberM,
    )
}
