package com.tourderun.core.data.team

import com.tourderun.core.data.firestore.DocWrite
import com.tourderun.core.data.firestore.FieldOp
import com.tourderun.core.data.firestore.InviteCodeFields
import com.tourderun.core.data.firestore.MemberFields
import com.tourderun.core.data.firestore.TeamFields
import com.tourderun.core.data.firestore.UserFields
import com.tourderun.core.data.firestore.WriteMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** TEAM-01 팀 만들기 배치. firebase/test/rules.test.mjs의 시나리오 1과 같은 모양이어야 규칙을 통과한다. */
class TeamCreatePlanTest {

    private val plan = teamCreatePlan(
        uid = "u1", nickname = "민찬", teamId = "t1", name = "뛰뛰크루", icon = 2, color = 5, inviteCode = "ABC234",
    )

    private fun write(path: String): DocWrite = plan.single { it.path == path }

    @Test fun `문서 4개를 teams·members·inviteCodes·users 순서로 쓴다`() {
        assertEquals(
            listOf("teams/t1", "teams/t1/members/u1", "inviteCodes/ABC234", "users/u1"),
            plan.map { it.path },
        )
    }

    @Test fun `users만 수정이고 나머지는 새로 만든다`() {
        assertEquals(
            listOf(WriteMode.SET, WriteMode.SET, WriteMode.SET, WriteMode.UPDATE),
            plan.map { it.mode },
        )
    }

    @Test fun `teams는 인원 1·시즌 1·거리 0·서버 시각·lastRunId null`() {
        assertEquals(
            mapOf(
                TeamFields.NAME to "뛰뛰크루",
                TeamFields.ICON to 2,
                TeamFields.COLOR to 5,
                TeamFields.MEMBER_COUNT to 1,
                TeamFields.SEASON to 1,
                TeamFields.DISTANCE_M to 0L,
                TeamFields.REACHED_AT to FieldOp.ServerTime,
                TeamFields.LAST_RUN_ID to null,
            ),
            write("teams/t1").fields,
        )
    }

    @Test fun `members는 팀장·초대 코드·기여 0·본 도착 0`() {
        assertEquals(
            mapOf(
                MemberFields.NICKNAME to "민찬",
                MemberFields.ROLE to MemberFields.ROLE_LEADER,
                MemberFields.JOINED_AT to FieldOp.ServerTime,
                MemberFields.INVITE_CODE to "ABC234",
                MemberFields.CONTRIBUTION_M to 0L,
                MemberFields.SEEN_ARRIVALS to 0,
                MemberFields.LAST_RUN_ID to null,
            ),
            write("teams/t1/members/u1").fields,
        )
    }

    @Test fun `inviteCodes는 팀 ID와 만든 시각`() {
        assertEquals(
            mapOf(InviteCodeFields.TEAM_ID to "t1", InviteCodeFields.CREATED_AT to FieldOp.ServerTime),
            write("inviteCodes/ABC234").fields,
        )
    }

    @Test fun `users는 소속 팀만 바꾼다`() {
        assertEquals(mapOf(UserFields.TEAM_ID to "t1"), write("users/u1").fields)
    }

    @Test fun `거리 필드는 정수 m(Long)`() {
        assertEquals(0L, write("teams/t1").fields[TeamFields.DISTANCE_M])
        assertEquals(0L, write("teams/t1/members/u1").fields[MemberFields.CONTRIBUTION_M])
    }
}
