package com.tourderun.domain.record

import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class RunTitleTest {
    private val seoul = ZoneId.of("Asia/Seoul")

    private fun seoulMs(mo: Int, d: Int, h: Int, mi: Int = 0, s: Int = 0): Long =
        ZonedDateTime.of(2026, mo, d, h, mi, s, 0, seoul).toInstant().toEpochMilli()

    @Test fun `11월 3일 19시 시작은 11월 3일 저녁 러닝`() {
        assertEquals("11월 3일 저녁 러닝", defaultRunTitle(seoulMs(11, 3, 19), seoul))
    }

    @Test fun `11월 3일 7시 시작은 11월 3일 아침 러닝`() {
        assertEquals("11월 3일 아침 러닝", defaultRunTitle(seoulMs(11, 3, 7), seoul))
    }

    @Test fun `시간대 경계는 시작 이상 끝 미만`() {
        assertEquals(TimeOfDay.DAWN, TimeOfDay.of(0))
        assertEquals(TimeOfDay.DAWN, TimeOfDay.of(5))
        assertEquals(TimeOfDay.MORNING, TimeOfDay.of(6))
        assertEquals(TimeOfDay.MORNING, TimeOfDay.of(10))
        assertEquals(TimeOfDay.LUNCH, TimeOfDay.of(11))
        assertEquals(TimeOfDay.LUNCH, TimeOfDay.of(13))
        assertEquals(TimeOfDay.AFTERNOON, TimeOfDay.of(14))
        assertEquals(TimeOfDay.AFTERNOON, TimeOfDay.of(17))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.of(18))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.of(21))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.of(22))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.of(23))
    }

    @Test fun `17시 59분 59초는 오후 18시 0분은 저녁`() {
        assertEquals("11월 3일 오후 러닝", defaultRunTitle(seoulMs(11, 3, 17, 59, 59), seoul))
        assertEquals("11월 3일 저녁 러닝", defaultRunTitle(seoulMs(11, 3, 18), seoul))
    }

    @Test fun `날짜와 시간대는 한국 시간 기준`() {
        // UTC 11월 2일 15:30 = 서울 11월 3일 00:30
        val ms = ZonedDateTime.of(2026, 11, 2, 15, 30, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals("11월 3일 새벽 러닝", defaultRunTitle(ms, seoul))
    }

    @Test fun `한 자리 월일은 0을 붙이지 않음`() {
        assertEquals("1월 5일 밤 러닝", defaultRunTitle(
            ZonedDateTime.of(2027, 1, 5, 23, 0, 0, 0, seoul).toInstant().toEpochMilli(), seoul,
        ))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `시가 범위를 벗어나면 예외`() {
        TimeOfDay.of(24)
    }
}
