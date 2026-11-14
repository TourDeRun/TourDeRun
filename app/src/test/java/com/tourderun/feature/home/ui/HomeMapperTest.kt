package com.tourderun.feature.home.ui

import com.tourderun.core.data.Cached
import com.tourderun.core.data.stats.TopPercent
import com.tourderun.core.data.stats.WeeklyStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeMapperTest {
    private val top = TopPercent(percent = 11, moreCount = 3, totalTeams = 38)

    private fun weekly(thisM: Long, lastM: Long, runs: Long = 9, fromCache: Boolean = false) =
        Cached(WeeklyStats(thisM, runs, lastM, 7, emptyMap()), isFromCache = fromCache)

    @Test fun `이번 주와 지난주가 있으면 차이와 상위 퍼센트를 보인다`() {
        val ui = weeklySummaryUi(weekly(42_500, 36_400), top)
        assertEquals(42_500L, ui.distanceM)
        assertEquals(9, ui.runCount)
        assertEquals(6_100L, ui.diffFromLastWeekM)
        assertEquals(38, ui.totalTeams)
        assertEquals(11, ui.topPercent)
    }

    @Test fun `지난주보다 덜 달렸으면 차이는 음수`() {
        assertEquals(-1_400L, weeklySummaryUi(weekly(35_000, 36_400), top).diffFromLastWeekM)
    }

    @Test fun `지난주 기록이 없으면 차이를 보이지 않는다`() {
        assertNull(weeklySummaryUi(weekly(42_500, 0), top).diffFromLastWeekM)
    }

    @Test fun `이번 주 0km면 상위 퍼센트를 숨긴다`() {
        val ui = weeklySummaryUi(weekly(0, 36_400, runs = 0), top)
        assertNull(ui.topPercent)
        assertNull(ui.totalTeams)
    }

    @Test fun `오프라인이면 상위 퍼센트를 숨긴다`() {
        val ui = weeklySummaryUi(weekly(42_500, 36_400, fromCache = true), top)
        assertNull(ui.topPercent)
        assertNull(ui.totalTeams)
    }

    @Test fun `상위 퍼센트를 아직 못 받았으면 숨긴다`() {
        val ui = weeklySummaryUi(weekly(42_500, 36_400), top = null)
        assertNull(ui.topPercent)
        assertEquals(6_100L, ui.diffFromLastWeekM)
    }
}
