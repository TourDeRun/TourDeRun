package com.tourderun.domain.race

import com.tourderun.domain.model.TeamStanding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RankInStageTest {
    private fun team(id: String, distanceM: Long, reachedAtMs: Long = 0L) =
        TeamStanding(teamId = id, name = id, distanceM = distanceM, reachedAtMs = reachedAtMs)

    private val a = team("a", 70_000, 100)
    private val b = team("b", 66_800, 200)
    private val c = team("c", 60_000, 300)

    @Test fun `내 팀뿐이면 1위 1팀 중 앞뒤 없음`() {
        val r = rankInStage(listOf(b), "b")!!
        assertEquals(1, r.rank)
        assertEquals(1, r.total)
        assertNull(r.ahead)
        assertNull(r.behind)
    }

    @Test fun `1위면 앞 팀 없음`() {
        val r = rankInStage(listOf(a, b, c), "a")!!
        assertEquals(1, r.rank)
        assertEquals(3, r.total)
        assertNull(r.ahead)
        assertEquals(b, r.behind)
    }

    @Test fun `중간이면 바로 위아래 팀`() {
        val r = rankInStage(listOf(a, b, c), "b")!!
        assertEquals(2, r.rank)
        assertEquals(a, r.ahead)
        assertEquals(c, r.behind)
    }

    @Test fun `꼴찌면 뒤 팀 없음`() {
        val r = rankInStage(listOf(a, b, c), "c")!!
        assertEquals(3, r.rank)
        assertEquals(b, r.ahead)
        assertNull(r.behind)
    }

    @Test fun `거리 동점이면 먼저 도달한 팀이 앞`() {
        val early = team("z", 50_000, reachedAtMs = 1_000)
        val late = team("y", 50_000, reachedAtMs = 2_000)
        val r = rankInStage(listOf(late, early), "y")!!
        assertEquals(2, r.rank)
        assertEquals(early, r.ahead)
    }

    @Test fun `거리와 시각이 모두 같으면 teamId 순`() {
        val x = team("x", 50_000, 1_000)
        val y = team("y", 50_000, 1_000)
        assertEquals(1, rankInStage(listOf(y, x), "x")!!.rank)
        assertEquals(2, rankInStage(listOf(y, x), "y")!!.rank)
    }

    @Test fun `내 팀이 목록에 없으면 null`() {
        assertNull(rankInStage(listOf(a, c), "b"))
        assertNull(rankInStage(emptyList(), "b"))
    }

    @Test fun `입력 순서가 섞여 있어도 결과 같음`() {
        val expected = rankInStage(listOf(a, b, c), "b")
        assertEquals(expected, rankInStage(listOf(c, a, b), "b"))
        assertEquals(expected, rankInStage(listOf(b, c, a), "b"))
    }
}
