package com.tourderun.feature.race.ui

import com.tourderun.core.data.Cached
import com.tourderun.core.data.race.RaceSummary
import com.tourderun.core.data.race.RaceTeam
import com.tourderun.domain.model.TeamStanding
import com.tourderun.domain.race.rankInStage
import com.tourderun.domain.route.routeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StageRankMapperTest {
    private val route = routeFor(1)

    private fun team(id: String, distanceM: Long, reachedAtMs: Long = 0L, icon: Int = 0, color: Int = 0, members: Int = 2) =
        RaceTeam(TeamStanding(id, "팀$id", distanceM, reachedAtMs), icon, color, members)

    /** 저장소처럼 순위순(거리↓ → reachedAt↑ → teamId)으로 정렬하고 rankInStage로 순위를 채운다. */
    private fun race(teams: List<RaceTeam>, myId: String, stageIndex: Int = 2, fromCache: Boolean = false): Cached<RaceSummary> {
        val ranking = rankInStage(teams.map { it.standing }, myId)
        val order = teams.sortedWith(
            compareByDescending<RaceTeam> { it.standing.distanceM }
                .thenBy { it.standing.reachedAtMs }
                .thenBy { it.standing.teamId },
        )
        return Cached(RaceSummary(stageIndex, order, ranking, null, null), isFromCache = fromCache)
    }

    private fun content(race: Cached<RaceSummary>, myId: String) =
        requireNotNull(stageRankContent(race, myId, route)) { "Content가 나와야 함" }

    @Test fun `1위면 첫 줄이 우리 팀이고 앞 팀 차이는 없음`() {
        val c = content(race(listOf(team("me", 74_100), team("a", 69_200), team("b", 65_700)), "me"), "me")
        assertEquals(1, c.ourRank)
        assertEquals(3, c.totalTeams)
        assertTrue(c.rows[0].isOurTeam)
        assertNull(c.rows[0].gapToAheadM)
        assertEquals(4_900L, c.rows[1].gapToAheadM)
        assertFalse(c.rows[1].isOurTeam)
    }

    @Test fun `꼴찌면 마지막 줄이 우리 팀이고 앞 팀과의 차이가 있음`() {
        val c = content(race(listOf(team("a", 69_200), team("b", 58_300), team("me", 49_200)), "me"), "me")
        assertEquals(3, c.ourRank)
        assertTrue(c.rows.last().isOurTeam)
        assertEquals(9_100L, c.rows.last().gapToAheadM)
        assertEquals(listOf(1, 2, 3), c.rows.map { it.rank })
    }

    @Test fun `스테이지에 혼자면 1위 1팀이고 isAlone`() {
        val c = content(race(listOf(team("me", 66_800)), "me"), "me")
        assertEquals(1, c.ourRank)
        assertEquals(1, c.totalTeams)
        assertTrue(c.isAlone)
        assertNull(c.rows.single().gapToAheadM)
    }

    @Test fun `동점이면 먼저 도달한 팀이 앞이고 차이는 0`() {
        val c = content(
            race(listOf(team("me", 60_000, reachedAtMs = 2_000), team("a", 60_000, reachedAtMs = 1_000)), "me"),
            "me",
        )
        assertEquals(2, c.ourRank)
        assertEquals("팀a", c.rows[0].name)
        assertEquals(0L, c.rows[1].gapToAheadM)
        assertEquals(listOf(1, 2), c.rows.map { it.rank })
    }

    @Test fun `헤더는 스테이지 번호와 구간, 줄에는 icon·color 번호와 인원`() {
        val c = content(race(listOf(team("me", 66_800, icon = 7, color = 5, members = 4)), "me", stageIndex = 2), "me")
        assertEquals(3, c.stageNumber)
        assertEquals("오산", c.fromCity)
        assertEquals("천안", c.toCity)
        assertEquals(48, c.startKm)
        assertEquals(85, c.endKm)
        val row = c.rows.single()
        assertEquals(7, row.iconIndex)
        assertEquals(5, row.colorIndex)
        assertEquals(4, row.memberCount)
        assertEquals(66_800L, row.distanceM)
    }

    @Test fun `캐시 값이면 오프라인`() {
        assertTrue(content(race(listOf(team("me", 66_800)), "me", fromCache = true), "me").isOffline)
        assertFalse(content(race(listOf(team("me", 66_800)), "me"), "me").isOffline)
    }

    @Test fun `스테이지가 바뀌는 순간 ranking이 null이면 직전 상태를 그대로 둔다`() {
        val before = content(race(listOf(team("a", 69_200), team("me", 66_800)), "me"), "me")
        val switching = race(listOf(team("a", 69_200)), "me") // 옛 목록에 내 팀이 없음 → ranking null
        assertNull(switching.value.ranking)
        assertSame(before, nextStageRankState(before, switching, "me", route))
    }

    @Test fun `직전 값이 없을 때 ranking이 null이면 불러오는 중`() {
        val switching = race(listOf(team("a", 69_200)), "me")
        assertEquals(StageRankUiState.Loading, nextStageRankState(StageRankUiState.Loading, switching, "me", route))
    }

    @Test fun `새 값이 오면 직전 상태 대신 새 순위`() {
        val before = content(race(listOf(team("a", 69_200), team("me", 66_800)), "me"), "me")
        val next = nextStageRankState(before, race(listOf(team("me", 70_000), team("a", 69_200)), "me"), "me", route)
        assertEquals(1, (next as StageRankUiState.Content).ourRank)
    }
}
