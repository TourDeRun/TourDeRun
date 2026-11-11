package com.tourderun.core.data.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.tourderun.core.data.race.RaceTeam
import com.tourderun.domain.model.TeamStanding

/**
 * `teams/{teamId}` 문서 중 다른 팀에 공개되는 필드(JRN-04: 이름·대표 이미지·인원·거리)와 순위용 도달 시각.
 *
 * @property icon 대표 아이콘 번호 0~7(M-06)
 * @property color 팀 색 번호 0~7(M-06)
 * @property reachedAtMs 현재 거리에 도달한 서버 시각(epoch ms)
 */
data class TeamDto(
    val teamId: String,
    val name: String,
    val icon: Int,
    val color: Int,
    val memberCount: Int,
    val distanceM: Long,
    val reachedAtMs: Long,
)

/**
 * 문서 → [TeamDto]. 필수 필드(`name`, `distanceM`)가 없으면 null(목록에서 뺀다).
 *
 * `reachedAt`은 서버 시각이 아직 정해지지 않은 대기 쓰기(내 팀이 방금 달린 경우)면 기기 추정값을 쓴다.
 * 값이 null이면 0으로 둔다(Firestore 오름차순 정렬에서 null이 가장 앞이라 같은 순서가 된다).
 */
fun DocumentSnapshot.toTeamDto(): TeamDto? {
    val name = getString(TeamFields.NAME) ?: return null
    val distanceM = getLong(TeamFields.DISTANCE_M) ?: return null
    val reachedAt = getTimestamp(TeamFields.REACHED_AT, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    return TeamDto(
        teamId = id,
        name = name,
        icon = getLong(TeamFields.ICON)?.toInt() ?: 0,
        color = getLong(TeamFields.COLOR)?.toInt() ?: 0,
        memberCount = getLong(TeamFields.MEMBER_COUNT)?.toInt() ?: 1,
        distanceM = distanceM,
        reachedAtMs = reachedAt?.toDate()?.time ?: 0L,
    )
}

fun TeamDto.toRaceTeam(): RaceTeam = RaceTeam(
    standing = TeamStanding(teamId = teamId, name = name, distanceM = distanceM, reachedAtMs = reachedAtMs),
    icon = icon,
    color = color,
    memberCount = memberCount,
)
