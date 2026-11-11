package com.tourderun.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class TopPercentTest {
    @Test fun `38팀 중 3팀이 더 달렸으면 상위 11퍼센트`() {
        assertEquals(11, topPercent(moreCount = 3, totalTeams = 38))
    }

    @Test fun `38팀 중 1위면 상위 3퍼센트`() {
        assertEquals(3, topPercent(moreCount = 0, totalTeams = 38))
    }

    @Test fun `팀이 하나뿐이면 상위 100퍼센트`() {
        assertEquals(100, topPercent(moreCount = 0, totalTeams = 1))
    }

    @Test fun `10팀 중 꼴찌면 상위 100퍼센트`() {
        assertEquals(100, topPercent(moreCount = 9, totalTeams = 10))
    }

    @Test fun `나누어떨어지면 올림하지 않음`() {
        // (1 + 1) / 20 × 100 = 10
        assertEquals(10, topPercent(moreCount = 1, totalTeams = 20))
    }

    @Test fun `moreCount가 전체 팀 수 이상이어도 100퍼센트`() {
        assertEquals(100, topPercent(moreCount = 10, totalTeams = 10))
        assertEquals(100, topPercent(moreCount = 50, totalTeams = 10))
    }

    @Test fun `같은 moreCount면 같은 퍼센트`() {
        assertEquals(topPercent(5, 38), topPercent(5, 38))
        assertEquals(16, topPercent(5, 38))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `전체 팀 수가 0이면 예외`() {
        topPercent(moreCount = 0, totalTeams = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `전체 팀 수가 음수면 예외`() {
        topPercent(moreCount = 0, totalTeams = -1)
    }
}
