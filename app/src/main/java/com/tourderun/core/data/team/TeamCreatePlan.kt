package com.tourderun.core.data.team

import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.DocWrite
import com.tourderun.core.data.firestore.FieldOp
import com.tourderun.core.data.firestore.InviteCodeFields
import com.tourderun.core.data.firestore.MemberFields
import com.tourderun.core.data.firestore.TeamFields
import com.tourderun.core.data.firestore.UserFields
import com.tourderun.core.data.firestore.WriteMode

/** 새 팀의 시즌. 지금은 시즌 1뿐이다. */
const val NEW_TEAM_SEASON = 1

/**
 * TEAM-01 팀 만들기 배치 계획. 문서 4개를 한 배치로 쓴다 (보안 규칙이 서로를 `getAfter`로 확인한다).
 * Firebase 타입을 쓰지 않아 JVM 테스트로 확인한다 (`runSavePlan`과 같은 방식).
 *
 * 1. `teams/{teamId}` 생성: 이름·아이콘·색, 인원 1, 시즌 1, 거리 0, reachedAt 서버 시각, lastRunId null
 * 2. `teams/{teamId}/members/{uid}` 생성: 닉네임, 팀장, 가입 시각, 초대 코드, 기여 0, 본 도착 0, lastRunId null
 * 3. `inviteCodes/{inviteCode}` 생성: 팀 ID, 만든 시각
 * 4. `users/{uid}` 수정: 소속 팀
 */
fun teamCreatePlan(
    uid: String,
    nickname: String,
    teamId: String,
    name: String,
    icon: Int,
    color: Int,
    inviteCode: String,
): List<DocWrite> {
    val team = DocWrite(
        path = "${Collections.TEAMS}/$teamId",
        mode = WriteMode.SET,
        fields = mapOf(
            TeamFields.NAME to name,
            TeamFields.ICON to icon,
            TeamFields.COLOR to color,
            TeamFields.MEMBER_COUNT to 1,
            TeamFields.SEASON to NEW_TEAM_SEASON,
            TeamFields.DISTANCE_M to 0L,
            TeamFields.REACHED_AT to FieldOp.ServerTime,
            TeamFields.LAST_RUN_ID to null,
        ),
    )

    val member = DocWrite(
        path = "${Collections.TEAMS}/$teamId/${Collections.MEMBERS}/$uid",
        mode = WriteMode.SET,
        fields = mapOf(
            MemberFields.NICKNAME to nickname,
            MemberFields.ROLE to MemberFields.ROLE_LEADER,
            MemberFields.JOINED_AT to FieldOp.ServerTime,
            MemberFields.INVITE_CODE to inviteCode,
            MemberFields.CONTRIBUTION_M to 0L,
            MemberFields.SEEN_ARRIVALS to 0,
            MemberFields.LAST_RUN_ID to null,
        ),
    )

    val code = DocWrite(
        path = "${Collections.INVITE_CODES}/$inviteCode",
        mode = WriteMode.SET,
        fields = mapOf(
            InviteCodeFields.TEAM_ID to teamId,
            InviteCodeFields.CREATED_AT to FieldOp.ServerTime,
        ),
    )

    val user = DocWrite(
        path = "${Collections.USERS}/$uid",
        mode = WriteMode.UPDATE,
        fields = mapOf(UserFields.TEAM_ID to teamId),
    )

    return listOf(team, member, code, user)
}
