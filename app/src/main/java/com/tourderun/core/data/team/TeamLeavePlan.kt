package com.tourderun.core.data.team

import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.DocWrite
import com.tourderun.core.data.firestore.MemberFields
import com.tourderun.core.data.firestore.TeamFields
import com.tourderun.core.data.firestore.UserFields
import com.tourderun.core.data.firestore.WriteMode

/** TEAM-07 나가는 방식. 트랜잭션 안에서 읽은 값으로 정한다. */
sealed interface LeaveKind {
    /** 팀원(또는 다른 팀원이 남은 팀장이 아닌 경우)이 나간다. */
    data class Member(val newMemberCount: Int) : LeaveKind

    /** 팀장이 나가고 [successorUid]가 팀장이 된다 (M-04). */
    data class Leader(val newMemberCount: Int, val successorUid: String) : LeaveKind

    /** 마지막 1명이 나가며 팀을 지운다. [statDocIds]는 그 팀의 `teamStats` 문서 ID 전부. */
    data class Last(val inviteCode: String?, val statDocIds: List<String>) : LeaveKind
}

/**
 * TEAM-07 팀 나가기 쓰기 계획. 팀 거리·기여 거리·주간 통계 숫자는 줄이지 않는다(M-01·M-02).
 *
 * - 공통: 내 `members` 삭제, 내 `users.teamId = null`
 * - [LeaveKind.Member]·[LeaveKind.Leader]: `teams.memberCount` = 읽은 인원 − 1 (규칙이 정확히 −1인지 본다)
 * - [LeaveKind.Leader]: 다음 팀장의 `role = leader` (나가기 전 팀장 권한으로, TEAM-08 규칙)
 * - [LeaveKind.Last]: `teams`·`inviteCodes`·`teamStats` 삭제
 */
fun teamLeavePlan(uid: String, teamId: String, kind: LeaveKind): List<DocWrite> {
    val teamPath = "${Collections.TEAMS}/$teamId"
    val myMember = DocWrite("$teamPath/${Collections.MEMBERS}/$uid", WriteMode.DELETE, emptyMap())
    val user = DocWrite("${Collections.USERS}/$uid", WriteMode.UPDATE, mapOf(UserFields.TEAM_ID to null))

    return when (kind) {
        is LeaveKind.Member -> listOf(
            myMember,
            DocWrite(teamPath, WriteMode.UPDATE, mapOf(TeamFields.MEMBER_COUNT to kind.newMemberCount)),
            user,
        )
        is LeaveKind.Leader -> listOf(
            DocWrite(
                "$teamPath/${Collections.MEMBERS}/${kind.successorUid}",
                WriteMode.UPDATE,
                mapOf(MemberFields.ROLE to MemberFields.ROLE_LEADER),
            ),
            myMember,
            DocWrite(teamPath, WriteMode.UPDATE, mapOf(TeamFields.MEMBER_COUNT to kind.newMemberCount)),
            user,
        )
        is LeaveKind.Last -> buildList {
            add(myMember)
            add(DocWrite(teamPath, WriteMode.DELETE, emptyMap()))
            kind.inviteCode?.let { add(DocWrite("${Collections.INVITE_CODES}/$it", WriteMode.DELETE, emptyMap())) }
            kind.statDocIds.forEach { add(DocWrite("${Collections.TEAM_STATS}/$it", WriteMode.DELETE, emptyMap())) }
            add(user)
        }
    }
}

/**
 * TEAM-06 팀장이 팀원을 내보낸다. 그 사람의 `members` 삭제 + `teams.memberCount` = 읽은 인원 − 1.
 * 팀장은 남의 `users`를 쓸 수 없어서(D-05) 그 사람의 `users.teamId`는 본인 기기가 비운다.
 * 거리·기여·주간 통계는 그대로 남는다(M-02, S14 "이전 팀원").
 */
fun teamKickPlan(teamId: String, targetUid: String, newMemberCount: Int): List<DocWrite> = listOf(
    DocWrite("${Collections.TEAMS}/$teamId/${Collections.MEMBERS}/$targetUid", WriteMode.DELETE, emptyMap()),
    DocWrite("${Collections.TEAMS}/$teamId", WriteMode.UPDATE, mapOf(TeamFields.MEMBER_COUNT to newMemberCount)),
)

/**
 * TEAM-08 팀장 넘기기. 상대 `role = leader`, 내 `role = member`를 **같은 배치로** 쓴다(팀장이 둘이 되지 않게).
 * 상대를 먼저 올린다(규칙은 쓰기 전 상태로 내가 팀장인지 보므로 순서는 결과에 영향이 없다).
 */
fun teamHandOverPlan(teamId: String, fromUid: String, toUid: String): List<DocWrite> = listOf(
    DocWrite(
        "${Collections.TEAMS}/$teamId/${Collections.MEMBERS}/$toUid",
        WriteMode.UPDATE,
        mapOf(MemberFields.ROLE to MemberFields.ROLE_LEADER),
    ),
    DocWrite(
        "${Collections.TEAMS}/$teamId/${Collections.MEMBERS}/$fromUid",
        WriteMode.UPDATE,
        mapOf(MemberFields.ROLE to MemberFields.ROLE_MEMBER),
    ),
)
