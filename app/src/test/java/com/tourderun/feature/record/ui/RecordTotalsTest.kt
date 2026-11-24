package com.tourderun.feature.record.ui

import com.tourderun.core.data.session.MyStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RecordTotalsTest {

    private val loaded = RecordsUiState(totalRuns = 20, totalDistanceM = 80_000, runs = emptyList(), canLoadMore = true)

    @Test fun `users 누적 값이 있으면 총계를 그 값으로`() {
        val state = loaded.withUserTotals(MyStats(totalM = 123_400, runCount = 35))
        assertEquals(35, state.totalRuns)
        assertEquals(123_400L, state.totalDistanceM)
    }

    @Test fun `누적 값을 모르면 불러온 기록의 합 그대로`() {
        assertSame(loaded, loaded.withUserTotals(null))
    }

    @Test fun `불러오는 중에는 숫자를 채우지 않는다`() {
        assertSame(RecordsUiState.Loading, RecordsUiState.Loading.withUserTotals(MyStats(totalM = 1_000, runCount = 1)))
    }

    @Test fun `목록과 더 불러오기 여부는 그대로`() {
        val state = loaded.withUserTotals(MyStats(totalM = 1, runCount = 1))
        assertEquals(loaded.runs, state.runs)
        assertEquals(true, state.canLoadMore)
    }
}
