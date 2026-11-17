package com.tourderun.domain.team

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContributionTest {

    // ---- 기여 비율 ----

    @Test fun `두 명이 6km·4km면 60%·40%`() {
        val shares = contributionShares(10_000, mapOf("u1" to 6_000L, "u2" to 4_000L))
        assertEquals(listOf("u1" to 60, "u2" to 40), shares.map { it.uid to it.percent })
    }

    @Test fun `기여 거리 순으로 정렬`() {
        val shares = contributionShares(10_000, mapOf("u1" to 2_000L, "u2" to 5_000L, "u3" to 3_000L))
        assertEquals(listOf("u2", "u3", "u1"), shares.map { it.uid })
    }

    @Test fun `같은 거리면 uid 순`() {
        val shares = contributionShares(2_000, mapOf("b" to 1_000L, "a" to 1_000L))
        assertEquals(listOf("a", "b"), shares.map { it.uid })
    }

    @Test fun `나간 팀원 몫은 이전 팀원 한 줄로 맨 끝`() {
        val shares = contributionShares(10_000, mapOf("u1" to 3_000L, "u2" to 2_000L))
        assertEquals(listOf("u1" to 30, "u2" to 20, null to 50), shares.map { it.uid to it.percent })
        assertEquals(5_000L, shares.last().distanceM)
    }

    @Test fun `이전 팀원 몫이 가장 커도 맨 끝`() {
        val shares = contributionShares(10_000, mapOf("u1" to 1_000L))
        assertEquals(listOf("u1", null), shares.map { it.uid })
    }

    @Test fun `나간 팀원이 없으면 이전 팀원 줄 없음`() {
        val shares = contributionShares(5_000, mapOf("u1" to 5_000L))
        assertEquals(listOf("u1" to 100), shares.map { it.uid to it.percent })
    }

    @Test fun `세 명이 똑같으면 34·33·33으로 합 100`() {
        val shares = contributionShares(3_000, mapOf("u1" to 1_000L, "u2" to 1_000L, "u3" to 1_000L))
        assertEquals(listOf(34, 33, 33), shares.map { it.percent })
    }

    @Test fun `퍼센트 합은 항상 100`() {
        val shares = contributionShares(
            100_000,
            mapOf("u1" to 33_333L, "u2" to 16_667L, "u3" to 9_999L, "u4" to 1L),
        )
        assertEquals(100, shares.sumOf { it.percent })
    }

    @Test fun `팀 거리 0이면 모두 0%·이전 팀원 줄 없음`() {
        val shares = contributionShares(0, mapOf("u1" to 0L, "u2" to 0L))
        assertEquals(listOf(0, 0), shares.map { it.percent })
        assertTrue(shares.all { it.uid != null })
    }

    // ---- 이번 주 MVP ----

    @Test fun `가장 많이 달린 한 명이 MVP`() {
        assertEquals(setOf("u2"), weeklyMvps(mapOf("u1" to 3_000L, "u2" to 7_000L), setOf("u1", "u2")))
    }

    @Test fun `동률이면 모두 MVP`() {
        assertEquals(
            setOf("u1", "u2"),
            weeklyMvps(mapOf("u1" to 5_000L, "u2" to 5_000L, "u3" to 1_000L), setOf("u1", "u2", "u3")),
        )
    }

    @Test fun `모두 0km면 MVP 없음`() {
        assertEquals(emptySet<String>(), weeklyMvps(mapOf("u1" to 0L), setOf("u1", "u2")))
    }

    @Test fun `이번 주 아무도 안 달렸으면 MVP 없음`() {
        assertEquals(emptySet<String>(), weeklyMvps(emptyMap(), setOf("u1", "u2")))
    }

    @Test fun `1인 팀이면 MVP 없음`() {
        assertEquals(emptySet<String>(), weeklyMvps(mapOf("u1" to 9_000L), setOf("u1")))
    }

    @Test fun `나간 팀원이 가장 많이 달렸어도 현재 팀원 중에서 고른다`() {
        assertEquals(
            setOf("u2"),
            weeklyMvps(mapOf("gone" to 20_000L, "u1" to 1_000L, "u2" to 4_000L), setOf("u1", "u2")),
        )
    }

    @Test fun `이번 주 기록이 없는 팀원은 0km로 본다`() {
        assertEquals(setOf("u1"), weeklyMvps(mapOf("u1" to 2_000L), setOf("u1", "u2", "u3")))
    }
}
