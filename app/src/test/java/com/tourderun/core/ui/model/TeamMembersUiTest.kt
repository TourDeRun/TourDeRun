package com.tourderun.core.ui.model

import com.tourderun.core.data.firestore.TeamMemberDto
import com.tourderun.core.designsystem.component.TeamColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamMembersUiTest {

    private val leader = TeamMemberDto("a", "민찬", isLeader = true, joinedAtMs = 1L, contributionM = 3_000)
    private val member = TeamMemberDto("b", "영훈", isLeader = false, joinedAtMs = 2L, contributionM = 7_000)

    @Test fun `기여 거리 내림차순이고 비율 합은 100`() {
        val ui = teamMembersUi(10_000, listOf(leader, member), emptyMap())
        assertEquals(listOf("b", "a"), ui.members.map { it.uid })
        assertEquals(listOf(70, 30), ui.members.map { it.percent })
        assertEquals(0L, ui.formerM)
    }

    @Test fun `나간 팀원 몫은 이전 팀원으로 따로`() {
        val ui = teamMembersUi(12_000, listOf(leader, member), emptyMap())
        assertEquals(2_000L, ui.formerM)
        assertEquals(100, ui.members.sumOf { it.percent } + ui.formerPercent)
    }

    @Test fun `이번 주 가장 많이 달린 팀원이 MVP`() {
        val ui = teamMembersUi(10_000, listOf(leader, member), mapOf("a" to 5_000L, "b" to 1_000L))
        assertTrue(ui.members.first { it.uid == "a" }.isMvp)
        assertFalse(ui.members.first { it.uid == "b" }.isMvp)
    }

    @Test fun `혼자 있는 팀은 MVP가 없다`() {
        assertFalse(teamMembersUi(3_000, listOf(leader), mapOf("a" to 3_000L)).members.single().isMvp)
    }

    @Test fun `아바타 색은 가입 순서대로`() {
        val ui = teamMembersUi(10_000, listOf(member, leader), emptyMap())
        assertEquals(TeamColor.BLUE, ui.members.first { it.uid == "a" }.color)
        assertEquals(TeamColor.NAVY, ui.members.first { it.uid == "b" }.color)
    }

    @Test fun `팀원 문서가 없으면 빈 목록`() {
        assertTrue(teamMembersUi(0, emptyList(), emptyMap()).members.isEmpty())
    }
}
