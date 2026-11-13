package com.tourderun.domain.stats

import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekIdTest {
    private fun seoulMs(y: Int, mo: Int, d: Int, h: Int = 0, mi: Int = 0, s: Int = 0): Long =
        ZonedDateTime.of(y, mo, d, h, mi, s, 0, SEOUL).toInstant().toEpochMilli()

    @Test fun `2026-11-15 일요일 23시 59분 59초는 W46`() {
        assertEquals("2026-W46", weekId(seoulMs(2026, 11, 15, 23, 59, 59)))
    }

    @Test fun `2026-11-16 월요일 0시는 W47`() {
        assertEquals("2026-W47", weekId(seoulMs(2026, 11, 16)))
    }

    @Test fun `2026-12-31은 2026-W53`() {
        assertEquals("2026-W53", weekId(seoulMs(2026, 12, 31, 12)))
    }

    @Test fun `2027-01-01은 2026-W53`() {
        assertEquals("2026-W53", weekId(seoulMs(2027, 1, 1, 12)))
    }

    @Test fun `2027-01-04 월요일은 2027-W01`() {
        assertEquals("2027-W01", weekId(seoulMs(2027, 1, 4)))
    }

    @Test fun `UTC 일요일 15시 30분은 서울 월요일 0시 30분이라 다음 주`() {
        val utcSunday = ZonedDateTime.of(2026, 11, 15, 15, 30, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals("2026-W47", weekId(utcSunday))
        // 같은 순간을 UTC 기준으로 보면 아직 일요일
        assertEquals("2026-W46", weekId(utcSunday, ZoneOffset.UTC))
    }

    @Test fun `주차는 두 자리로 채움`() {
        assertEquals("2027-W02", weekId(seoulMs(2027, 1, 11)))
    }

    @Test fun `2027-01-04 월요일 0시의 지난주는 2026-W53`() {
        assertEquals("2026-W53", lastWeekId(seoulMs(2027, 1, 4)))
    }

    @Test fun `2027-01-01의 지난주는 2026-W52`() {
        assertEquals("2026-W52", lastWeekId(seoulMs(2027, 1, 1, 12)))
    }

    @Test fun `2026-11-16 월요일 0시의 지난주는 W46`() {
        assertEquals("2026-W46", lastWeekId(seoulMs(2026, 11, 16)))
    }

    @Test fun `2026-11-15 일요일 23시 59분 59초의 지난주는 W45`() {
        assertEquals("2026-W45", lastWeekId(seoulMs(2026, 11, 15, 23, 59, 59)))
    }
}
