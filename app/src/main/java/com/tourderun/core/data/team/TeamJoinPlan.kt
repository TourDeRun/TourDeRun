package com.tourderun.core.data.team

import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.DocWrite
import com.tourderun.core.data.firestore.FieldOp
import com.tourderun.core.data.firestore.MemberFields
import com.tourderun.core.data.firestore.TeamFields
import com.tourderun.core.data.firestore.UserFields
import com.tourderun.core.data.firestore.WriteMode

/**
 * TEAM-03 초대 코드로 참여할 때 쓰는 문서 3개. 트랜잭션 안에서 팀 인원을 다시 읽은 뒤 이 계획대로 쓴다.
 *
 * 1. `teams/{teamId}/members/{uid}` 생성: 닉네임, 팀원, 가입 시각, 쓴 초대 코드, 기여 0, 본 도착 = [seenArrivals], lastRunId null
 * 2. `teams/{teamId}` 수정: 인원 = [newMemberCount] (읽은 인원 + 1. 보안 규칙이 정확히 +1인지, 4 이하인지 확인한다)
 * 3. `users/{uid}` 수정: 소속 팀
 *
 * @param seenArrivals 참여 시점에 팀이 이미 도착한 도시 수 (`arrivals(팀 거리, 경로)`). 가입 전에 지나온 도시는
 *   클리어 연출·지역 카드 대상이 아니다
 */
fun teamJoinPlan(
    uid: String,
    nickname: String,
    teamId: String,
    inviteCode: String,
    newMemberCount: Int,
    seenArrivals: Int,
): List<DocWrite> {
    val member = DocWrite(
        path = "${Collections.TEAMS}/$teamId/${Collections.MEMBERS}/$uid",
        mode = WriteMode.SET,
        fields = mapOf(
            MemberFields.NICKNAME to nickname,
            MemberFields.ROLE to MemberFields.ROLE_MEMBER,
            MemberFields.JOINED_AT to FieldOp.ServerTime,
            MemberFields.INVITE_CODE to inviteCode,
            MemberFields.CONTRIBUTION_M to 0L,
            MemberFields.SEEN_ARRIVALS to seenArrivals,
            MemberFields.LAST_RUN_ID to null,
        ),
    )

    val team = DocWrite(
        path = "${Collections.TEAMS}/$teamId",
        mode = WriteMode.UPDATE,
        fields = mapOf(TeamFields.MEMBER_COUNT to newMemberCount),
    )

    val user = DocWrite(
        path = "${Collections.USERS}/$uid",
        mode = WriteMode.UPDATE,
        fields = mapOf(UserFields.TEAM_ID to teamId),
    )

    return listOf(member, team, user)
}
