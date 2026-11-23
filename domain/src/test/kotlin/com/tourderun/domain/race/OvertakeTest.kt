package com.tourderun.domain.race

import com.tourderun.domain.model.TeamStanding
import com.tourderun.domain.route.routeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OvertakeTest {
    private val route = routeFor(1)

    private fun team(id: String, distanceM: Long, reachedAtMs: Long = 100L) =
        TeamStanding(teamId = id, name = id, distanceM = distanceM, reachedAtMs = reachedAtMs)

    /** 스테이지 3(48~85km) 팀들. 내 팀 me는 66,800m에서 출발 */
    private val orange = team("주황이들", 69_200)
    private val purple = team("보라돌이", 70_000)
    private val dawn = team("새벽러너스", 60_000)

    private fun overtake(teams: List<TeamStanding>, beforeM: Long, afterM: Long, beforeReachedAtMs: Long = 500L) =
        overtakeOf(teams, "me", beforeM, afterM, beforeReachedAtMs, route)

    @Test fun `앞 팀이 여전히 앞 → 추월 없음`() {
        assertNull(overtake(listOf(orange, dawn), beforeM = 66_800, afterM = 69_000))
    }

    @Test fun `한 팀 추월 → 2위에서 1위, 차이 2,400m, 지금 840m 앞`() {
        assertEquals(
            Overtake("주황이들", fromRank = 2, toRank = 1, gapBeforeM = 2_400, leadNowM = 840),
            overtake(listOf(orange, dawn), beforeM = 66_800, afterM = 70_040),
        )
    }

    @Test fun `두 팀 한 번에 추월 → 3위에서 1위, 보이는 팀은 바로 앞이던 주황이들`() {
        assertEquals(
            Overtake("주황이들", fromRank = 3, toRank = 1, gapBeforeM = 2_400, leadNowM = 1_800),
            overtake(listOf(purple, orange, dawn), beforeM = 66_800, afterM = 71_000),
        )
    }

    @Test fun `세 팀 중 하나만 넘음 → 3위에서 2위`() {
        assertEquals(
            Overtake("주황이들", fromRank = 3, toRank = 2, gapBeforeM = 2_400, leadNowM = 300),
            overtake(listOf(purple, orange, dawn), beforeM = 66_800, afterM = 69_500),
        )
    }

    @Test fun `스테이지에 내 팀뿐 → 추월 없음`() {
        assertNull(overtake(listOf(team("me", 70_040)), beforeM = 66_800, afterM = 70_040))
        assertNull(overtake(emptyList(), beforeM = 66_800, afterM = 70_040))
    }

    @Test fun `거리가 같아짐 → 추월 아님(내가 나중에 도달)`() {
        assertNull(overtake(listOf(orange, dawn), beforeM = 66_800, afterM = 69_200))
    }

    @Test fun `이미 1위 → 추월 없음`() {
        assertNull(overtake(listOf(dawn), beforeM = 66_800, afterM = 70_000))
    }

    @Test fun `스테이지를 넘어감(84km → 86km) → null`() {
        val ahead = team("앞팀", 84_500)
        assertNull(overtake(listOf(ahead), beforeM = 84_000, afterM = 86_000))
    }

    @Test fun `스테이지 끝 바로 앞까지(84,999m) → 같은 스테이지라 추월`() {
        val ahead = team("앞팀", 84_500)
        assertEquals(
            Overtake("앞팀", fromRank = 2, toRank = 1, gapBeforeM = 500, leadNowM = 499),
            overtake(listOf(ahead), beforeM = 84_000, afterM = 84_999),
        )
    }

    @Test fun `목록 속 내 팀 거리는 무시(캐시가 이미 저장 후 거리)`() {
        val cachedMe = team("me", 70_040, reachedAtMs = 9_999)
        assertEquals(
            Overtake("주황이들", fromRank = 2, toRank = 1, gapBeforeM = 2_400, leadNowM = 840),
            overtake(listOf(cachedMe, orange, dawn), beforeM = 66_800, afterM = 70_040),
        )
    }

    @Test fun `러닝 전 거리가 같았고 그 팀이 먼저 도달 → 넘으면 추월, 차이 0`() {
        val tied = team("동점팀", 66_800, reachedAtMs = 100)
        assertEquals(
            Overtake("동점팀", fromRank = 2, toRank = 1, gapBeforeM = 0, leadNowM = 1_000),
            overtake(listOf(tied), beforeM = 66_800, afterM = 67_800, beforeReachedAtMs = 500),
        )
    }

    @Test fun `러닝 전 거리가 같았고 내가 먼저 도달 → 이미 앞이라 추월 아님`() {
        val tied = team("동점팀", 66_800, reachedAtMs = 900)
        assertNull(overtake(listOf(tied), beforeM = 66_800, afterM = 67_800, beforeReachedAtMs = 500))
    }

    @Test fun `완주 뒤 마지막 스테이지 안에서 추월 → 같은 스테이지로 본다`() {
        val ahead = team("앞팀", 351_000)
        assertEquals(
            Overtake("앞팀", fromRank = 2, toRank = 1, gapBeforeM = 1_000, leadNowM = 1_000),
            overtake(listOf(ahead), beforeM = 350_000, afterM = 352_000),
        )
    }
}
