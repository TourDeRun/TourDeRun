package com.tourderun.core.data.team

import com.tourderun.core.data.firestore.WriteMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamLeavePlanTest {

    @Test fun `팀원이 나가면 내 문서 삭제·인원 −1·users의 팀 비우기`() {
        val plan = teamLeavePlan("u2", "t1", LeaveKind.Member(newMemberCount = 2))
        assertEquals(listOf("teams/t1/members/u2", "teams/t1", "users/u2"), plan.map { it.path })
        assertEquals(listOf(WriteMode.DELETE, WriteMode.UPDATE, WriteMode.UPDATE), plan.map { it.mode })
        assertEquals(mapOf("memberCount" to 2), plan[1].fields)
        assertEquals(mapOf("teamId" to null), plan[2].fields)
    }

    @Test fun `팀장이 나가면 다음 팀장의 역할을 먼저 바꾼다`() {
        val plan = teamLeavePlan("u1", "t1", LeaveKind.Leader(newMemberCount = 1, successorUid = "u2"))
        assertEquals("teams/t1/members/u2", plan.first().path)
        assertEquals(mapOf("role" to "leader"), plan.first().fields)
        assertTrue(plan.any { it.path == "teams/t1/members/u1" && it.mode == WriteMode.DELETE })
    }

    @Test fun `마지막 1명이면 팀·초대 코드·주간 통계를 지운다`() {
        val plan = teamLeavePlan("u1", "t1", LeaveKind.Last("ABC234", listOf("t1_2026-W47", "t1_2026-W48")))
        val deleted = plan.filter { it.mode == WriteMode.DELETE }.map { it.path }
        assertEquals(
            listOf("teams/t1/members/u1", "teams/t1", "inviteCodes/ABC234", "teamStats/t1_2026-W47", "teamStats/t1_2026-W48"),
            deleted,
        )
        assertEquals("users/u1", plan.last().path)
    }

    @Test fun `숫자 필드(거리·기여)는 계획에 없다`() {
        val kinds = listOf(LeaveKind.Member(1), LeaveKind.Leader(1, "u2"), LeaveKind.Last(null, emptyList()))
        kinds.forEach { kind ->
            val keys = teamLeavePlan("u1", "t1", kind).flatMap { it.fields.keys }
            assertTrue(keys.none { it in setOf("distanceM", "contributionM", "memberM") })
        }
    }
}
