package com.tourderun.feature.record.ui

import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.record.FakeRunRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** S12: 리스너 값 → 화면 상태. 닫히는 것은 Ready(null)뿐이다. */
class RecordDetailLoadTest {

    private val record = FakeRunRepository.SAMPLE_RUNS.first()

    @Test fun `Failed → 닫히지 않고 불러오는 중`() {
        val load = recordDetailLoad(Loaded.Failed(DataError.Unavailable), null, null, false)

        assertEquals(RecordDetailLoad.Loading, load)
    }

    @Test fun `Ready(null) → 닫힘`() {
        val load = recordDetailLoad(Loaded.Ready(null, isFromCache = false), null, null, false)

        assertEquals(RecordDetailLoad.Gone, load)
    }

    @Test fun `Ready(기록) → 내용 표시`() {
        val load = recordDetailLoad(Loaded.Ready(record, isFromCache = true), null, null, false)

        assertTrue(load is RecordDetailLoad.Ready)
    }
}
