package com.tourderun.domain.team

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LeaveTest {

    @Test fun `가장 먼저 가입한 팀원이 다음 팀장`() {
        val members = listOf(MemberJoin("leader", 1), MemberJoin("c", 30), MemberJoin("b", 20))
        assertEquals("b", nextLeader(members, leavingUid = "leader"))
    }

    @Test fun `가입 시각이 같으면 uid 순`() {
        val members = listOf(MemberJoin("leader", 1), MemberJoin("z", 20), MemberJoin("a", 20))
        assertEquals("a", nextLeader(members, leavingUid = "leader"))
    }

    @Test fun `나가는 사람은 후보가 아니다`() {
        val members = listOf(MemberJoin("leader", 1), MemberJoin("b", 2))
        assertEquals("b", nextLeader(members, leavingUid = "leader"))
    }

    @Test fun `혼자 남은 팀장이면 다음 팀장이 없다`() {
        assertNull(nextLeader(listOf(MemberJoin("leader", 1)), leavingUid = "leader"))
    }

    @Test fun `팀원 목록이 비어 있으면 없다`() {
        assertNull(nextLeader(emptyList(), leavingUid = "leader"))
    }
}
