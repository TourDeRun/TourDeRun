package com.tourderun.feature.stat.ui

import com.tourderun.core.data.Cached
import com.tourderun.core.data.stats.TopPercent
import com.tourderun.core.data.stats.WeeklyStats
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyStatsMapperTest {
    private val period = WeekPeriodUi("2026-W46", 11, 9, 11, 15)
    private val top = TopPercent(percent = 11, moreCount = 3, totalTeams = 38)

    private fun stats(thisM: Long, lastM: Long, thisRuns: Long = 9, lastRuns: Long = 7) =
        WeeklyStats(thisM, thisRuns, lastM, lastRuns, emptyMap())

    private fun content(thisM: Long, lastM: Long, top: TopPercent? = this.top, fromCache: Boolean = false) =
        weeklyStatsContent(Cached(stats(thisM, lastM), isFromCache = fromCache), top, period)

    private fun seoulMs(y: Int, mo: Int, d: Int, h: Int = 12, mi: Int = 0) =
        LocalDateTime.of(y, mo, d, h, mi).atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()

    @Test fun `지난주보다 늘면 양수 차이`() {
        assertEquals(WeeklyChangeUi.Diff(6_100), content(42_500, 36_400).change)
    }

    @Test fun `지난주보다 줄면 음수 차이`() {
        assertEquals(WeeklyChangeUi.Diff(-2_000), content(34_400, 36_400).change)
    }

    @Test fun `지난주와 같으면 Same`() {
        assertEquals(WeeklyChangeUi.Same, content(36_400, 36_400).change)
    }

    @Test fun `지난주 0km면 기록 없음`() {
        assertEquals(WeeklyChangeUi.NoLastWeek, content(42_500, 0).change)
        assertEquals(WeeklyChangeUi.NoLastWeek, content(0, 0).change)
    }

    @Test fun `이번 주 0km면 상위 % 대신 첫 러닝 안내`() {
        val c = content(0, 36_400, top = null)
        assertEquals(WeeklyRankUi.FirstRun, c.rank)
        assertEquals(WeeklyChangeUi.Diff(-36_400), c.change)
        assertEquals(0L, c.distanceM)
    }

    @Test fun `이번 주 0km면 상위 % 값이 와도 첫 러닝 안내`() {
        assertEquals(WeeklyRankUi.FirstRun, content(0, 36_400, top = top).rank)
    }

    @Test fun `상위 %가 null이면 카드를 숨긴다`() {
        assertEquals(WeeklyRankUi.Hidden, content(42_500, 36_400, top = null).rank)
    }

    @Test fun `상위 %가 있으면 퍼센트·전체 팀·더 달린 팀`() {
        assertEquals(WeeklyRankUi.Shown(percent = 11, totalTeams = 38, moreCount = 3), content(42_500, 36_400).rank)
    }

    @Test fun `오프라인이면 캐시 값을 보여 주고 상위 %는 숨긴다`() {
        val c = content(42_500, 36_400, top = top, fromCache = true)
        assertTrue(c.isOffline)
        assertEquals(WeeklyRankUi.Hidden, c.rank)
        assertEquals(42_500L, c.distanceM)
        assertEquals(9L, c.runCount)
        assertEquals(7L, c.lastWeekRunCount)
    }

    @Test fun `상위 %는 온라인이고 이번 주 거리가 있을 때만 조회`() {
        assertTrue(needsTopPercent(Cached(stats(42_500, 0), isFromCache = false)))
        assertFalse(needsTopPercent(Cached(stats(42_500, 0), isFromCache = true)))
        assertFalse(needsTopPercent(Cached(stats(0, 36_400), isFromCache = false)))
    }

    @Test fun `기간은 한국 시간 월요일부터 일요일`() {
        assertEquals(WeekPeriodUi("2026-W46", 11, 9, 11, 15), weekPeriod(seoulMs(2026, 11, 12)))
    }

    @Test fun `월요일 0시는 그 주, 일요일 23시 59분은 앞 주`() {
        assertEquals("2026-W46", weekPeriod(seoulMs(2026, 11, 9, 0, 0)).weekId)
        assertEquals("2026-W45", weekPeriod(seoulMs(2026, 11, 8, 23, 59)).weekId)
    }

    @Test fun `2027-01-01은 2026-W53이고 달이 바뀌는 기간`() {
        assertEquals(WeekPeriodUi("2026-W53", 12, 28, 1, 3), weekPeriod(seoulMs(2027, 1, 1)))
    }
}
