package com.tourderun.feature.team.ui

import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.firestore.TeamDto
import com.tourderun.core.data.firestore.TeamMemberDto
import com.tourderun.core.designsystem.component.TeamColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamUiStateTest {

    private fun team(distanceM: Long, memberCount: Int) =
        TeamDto("t1", "뛰뛰크루", icon = 0, color = 0, memberCount = memberCount, distanceM = distanceM, reachedAtMs = 0L)

    private val leader = TeamMemberDto("a", "민찬", isLeader = true, joinedAtMs = 1L, contributionM = 3_000)
    private val member = TeamMemberDto("b", "영훈", isLeader = false, joinedAtMs = 2L, contributionM = 7_000)

    @Test fun `팀원은 기여 거리 내림차순이고 비율 합은 100`() {
        val state = teamUiState(team(10_000, 2), listOf(leader, member), "ABC234", emptyMap(), myUid = "a")
        assertEquals(listOf("b", "a"), state.members.map { it.uid })
        assertEquals(listOf(70, 30), state.members.map { it.percent })
        assertEquals(0L, state.formerM)
    }

    @Test fun `나간 팀원 몫은 이전 팀원으로 따로 나온다`() {
        val state = teamUiState(team(12_000, 2), listOf(leader, member), "ABC234", emptyMap(), myUid = "a")
        assertEquals(2_000L, state.formerM)
        assertEquals(100, state.members.sumOf { it.percent } + state.formerPercent)
    }

    @Test fun `이번 주 가장 많이 달린 팀원이 MVP`() {
        val state = teamUiState(team(10_000, 2), listOf(leader, member), "", mapOf("a" to 5_000L, "b" to 1_000L), myUid = "a")
        assertTrue(state.members.first { it.uid == "a" }.isMvp)
        assertFalse(state.members.first { it.uid == "b" }.isMvp)
    }

    @Test fun `혼자 있는 팀은 MVP가 없다`() {
        val state = teamUiState(team(3_000, 1), listOf(leader), "", mapOf("a" to 3_000L), myUid = "a")
        assertFalse(state.members.single().isMvp)
    }

    @Test fun `팀장 여부는 내 members 문서의 역할로 정한다`() {
        assertTrue(teamUiState(team(0, 2), listOf(leader, member), "", emptyMap(), myUid = "a").isLeader)
        assertFalse(teamUiState(team(0, 2), listOf(leader, member), "", emptyMap(), myUid = "b").isLeader)
    }

    @Test fun `아바타 색은 가입 순서대로 정해진다`() {
        val state = teamUiState(team(10_000, 2), listOf(member, leader), "", emptyMap(), myUid = "a")
        assertEquals(TeamColor.BLUE, state.members.first { it.uid == "a" }.color)
        assertEquals(TeamColor.NAVY, state.members.first { it.uid == "b" }.color)
    }

    @Test fun `팀원 목록을 못 읽으면 실패로 보인다`() {
        val state = teamLoadState(
            team = Loaded.Ready(team(0, 1), isFromCache = false),
            members = Loaded.Failed(DataError.PermissionDenied),
            inviteCode = "",
            weeklyMemberM = emptyMap(),
            myUid = "a",
        )
        assertEquals(TeamLoadState.Failed(DataError.PermissionDenied), state)
    }

    @Test fun `팀 문서가 없으면 불러오는 중으로 둔다`() {
        val state = teamLoadState(
            team = Loaded.Ready(null, isFromCache = false),
            members = Loaded.Ready(listOf(leader), isFromCache = false),
            inviteCode = "",
            weeklyMemberM = emptyMap(),
            myUid = "a",
        )
        assertEquals(TeamLoadState.Loading, state)
    }

    @Test fun `팀과 팀원이 오면 화면 값을 만든다`() {
        val state = teamLoadState(
            team = Loaded.Ready(team(10_000, 2), isFromCache = true),
            members = Loaded.Ready(listOf(leader, member), isFromCache = true),
            inviteCode = "ABC234",
            weeklyMemberM = emptyMap(),
            myUid = "a",
        )
        assertTrue(state is TeamLoadState.Ready)
    }
}
