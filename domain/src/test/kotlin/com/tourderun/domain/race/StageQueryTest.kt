package com.tourderun.domain.race

import com.tourderun.domain.model.TeamStanding
import com.tourderun.domain.route.routeFor
import org.junit.Assert.assertEquals
import org.junit.Test

class StageQueryTest {
    private val route = routeFor(1)

    private fun team(id: String, distanceM: Long) =
        TeamStanding(teamId = id, name = id, distanceM = distanceM, reachedAtMs = 0L)

    @Test fun `0m는 스테이지 1 범위 0 이상 19000 미만`() {
        assertEquals(StageRange(0, 0, 19_000), stageRange(0, route))
    }

    @Test fun `18999m는 스테이지 1 범위`() {
        assertEquals(StageRange(0, 0, 19_000), stageRange(18_999, route))
    }

    @Test fun `19000m는 스테이지 2 범위 19000 이상 48000 미만`() {
        assertEquals(StageRange(1, 19_000, 48_000), stageRange(19_000, route))
    }

    @Test fun `66800m는 스테이지 3 범위 48000 이상 85000 미만`() {
        assertEquals(StageRange(2, 48_000, 85_000), stageRange(66_800, route))
    }

    @Test fun `262000m는 마지막 스테이지 7이고 상한 없음`() {
        assertEquals(StageRange(6, 262_000, null), stageRange(262_000, route))
    }

    @Test fun `349999m는 마지막 스테이지 상한 없음`() {
        assertEquals(StageRange(6, 262_000, null), stageRange(349_999, route))
    }

    @Test fun `완주 팀 350000m 이상도 마지막 스테이지 상한 없음`() {
        assertEquals(StageRange(6, 262_000, null), stageRange(350_000, route))
        assertEquals(StageRange(6, 262_000, null), stageRange(412_300, route))
    }

    @Test fun `1위면 앞 팀만 대체 조회`() {
        val ranking = requireNotNull(rankInStage(listOf(team("me", 74_100), team("b", 69_200)), "me"))
        assertEquals(FallbackQuery(aboveM = 74_100, belowM = null), fallbackQuery(ranking, 74_100))
    }

    @Test fun `꼴찌면 뒤 팀만 대체 조회`() {
        val ranking = requireNotNull(rankInStage(listOf(team("a", 69_200), team("me", 49_200)), "me"))
        assertEquals(FallbackQuery(aboveM = null, belowM = 49_200), fallbackQuery(ranking, 49_200))
    }

    @Test fun `혼자면 앞뒤 모두 대체 조회`() {
        val ranking = requireNotNull(rankInStage(listOf(team("me", 66_800)), "me"))
        assertEquals(FallbackQuery(aboveM = 66_800, belowM = 66_800), fallbackQuery(ranking, 66_800))
    }

    @Test fun `중간이면 대체 조회 없음`() {
        val teams = listOf(team("a", 69_200), team("me", 66_800), team("c", 65_700))
        val ranking = requireNotNull(rankInStage(teams, "me"))
        assertEquals(FallbackQuery.NONE, fallbackQuery(ranking, 66_800))
    }

    @Test fun `순위가 없으면 대체 조회 없음`() {
        assertEquals(FallbackQuery.NONE, fallbackQuery(null, 66_800))
    }
}
