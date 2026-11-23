package com.tourderun.core.data.team

import com.tourderun.core.data.firestore.DocWrite
import com.tourderun.core.data.firestore.FieldOp
import com.tourderun.core.data.firestore.MemberFields
import com.tourderun.core.data.firestore.TeamFields
import com.tourderun.core.data.firestore.UserFields
import com.tourderun.core.data.firestore.WriteMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** TEAM-03 참여 쓰기. firebase/test/rules.test.mjs의 시나리오 2(joinBatch)와 같은 모양이어야 규칙을 통과한다. */
class TeamJoinPlanTest {

    private val plan = teamJoinPlan(
        uid = "u4", nickname = "새러너", teamId = "t1", inviteCode = "ABC234", newMemberCount = 4, seenArrivals = 2,
    )

    private fun write(path: String): DocWrite = plan.single { it.path == path }

    @Test fun `문서 3개를 members·teams·users 순서로 쓴다`() {
        assertEquals(listOf("teams/t1/members/u4", "teams/t1", "users/u4"), plan.map { it.path })
    }

    @Test fun `members만 새로 만들고 팀·사용자는 수정`() {
        assertEquals(listOf(WriteMode.SET, WriteMode.UPDATE, WriteMode.UPDATE), plan.map { it.mode })
    }

    @Test fun `members는 팀원·쓴 코드·기여 0·참여 시점 도착 수`() {
        assertEquals(
            mapOf(
                MemberFields.NICKNAME to "새러너",
                MemberFields.ROLE to MemberFields.ROLE_MEMBER,
                MemberFields.JOINED_AT to FieldOp.ServerTime,
                MemberFields.INVITE_CODE to "ABC234",
                MemberFields.CONTRIBUTION_M to 0L,
                MemberFields.SEEN_ARRIVALS to 2,
                MemberFields.LAST_RUN_ID to null,
            ),
            write("teams/t1/members/u4").fields,
        )
    }

    @Test fun `팀은 인원만 바꾼다`() {
        assertEquals(mapOf(TeamFields.MEMBER_COUNT to 4), write("teams/t1").fields)
    }

    @Test fun `users는 소속 팀만 바꾼다`() {
        assertEquals(mapOf(UserFields.TEAM_ID to "t1"), write("users/u4").fields)
    }
}
