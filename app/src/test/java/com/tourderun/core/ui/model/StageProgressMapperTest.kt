package com.tourderun.core.ui.model

import com.tourderun.core.ui.Format
import com.tourderun.domain.route.routeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StageProgressMapperTest {
    private val route = routeFor(1)

    private fun at(m: Long) = stageProgressUi(m, route)

    @Test fun `0m는 스테이지 1 서울에서 출발, 도착 0`() {
        val s = at(0)
        assertEquals(1, s.stageNumber)
        assertEquals(7, s.stageCount)
        assertEquals("서울", s.fromCity)
        assertEquals("안양", s.toCity)
        assertEquals(0, s.startKm)
        assertEquals(19, s.endKm)
        assertEquals(0L, s.doneInStageM)
        assertEquals(19_000L, s.toNextM)
        assertEquals(0f, s.progress, 0f)
        assertEquals(0, s.arrivals)
        assertFalse(s.finished)
    }

    @Test fun `18999m는 스테이지 1의 끝, 안양까지 1m`() {
        val s = at(18_999)
        assertEquals(1, s.stageNumber)
        assertEquals(1L, s.toNextM)
        assertEquals(18_999L, s.doneInStageM)
        assertEquals(0, s.arrivals)
        assertFalse(s.finished)
    }

    @Test fun `19000m는 스테이지 2의 시작, 도착 1`() {
        val s = at(19_000)
        assertEquals(2, s.stageNumber)
        assertEquals("안양", s.fromCity)
        assertEquals("오산", s.toCity)
        assertEquals(19, s.startKm)
        assertEquals(48, s.endKm)
        assertEquals(0L, s.doneInStageM)
        assertEquals(29_000L, s.toNextM)
        assertEquals(0f, s.progress, 0f)
        assertEquals(1, s.arrivals)
    }

    @Test fun `84960m는 스테이지 3, 천안까지 40m이고 표시는 0점1km`() {
        val s = at(84_960)
        assertEquals(3, s.stageNumber)
        assertEquals("천안", s.toCity)
        assertEquals(40L, s.toNextM)
        assertEquals(36_960L, s.doneInStageM)
        assertEquals(2, s.arrivals)
        assertTrue(s.progress < 1f)
        assertEquals("0.1", Format.kmUp(s.toNextM))
        assertEquals("36.9", Format.kmDown(s.doneInStageM))
    }

    @Test fun `85000m는 스테이지 4의 시작, 도착 3`() {
        val s = at(85_000)
        assertEquals(4, s.stageNumber)
        assertEquals("천안", s.fromCity)
        assertEquals("세종", s.toCity)
        assertEquals(85, s.startKm)
        assertEquals(125, s.endKm)
        assertEquals(40_000L, s.toNextM)
        assertEquals(3, s.arrivals)
    }

    @Test fun `349999m는 스테이지 7, 완주 전`() {
        val s = at(349_999)
        assertEquals(7, s.stageNumber)
        assertEquals(1L, s.toNextM)
        assertEquals(6, s.arrivals)
        assertFalse(s.finished)
        assertEquals("0.1", Format.kmUp(s.toNextM))
    }

    @Test fun `350000m는 완주, 도착 7, 부산에 멈춤`() {
        val s = at(350_000)
        assertEquals(7, s.stageNumber)
        assertEquals("대구", s.fromCity)
        assertEquals("부산", s.toCity)
        assertEquals(0L, s.toNextM)
        assertEquals(88_000L, s.doneInStageM)
        assertEquals(1f, s.progress, 0f)
        assertEquals(7, s.arrivals)
        assertTrue(s.finished)
    }

    @Test fun `360000m는 위치는 완주 그대로, 팀 거리는 계속 쌓임`() {
        val s = at(360_000)
        assertEquals(7, s.stageNumber)
        assertEquals(360_000L, s.teamDistanceM)
        assertEquals(88_000L, s.doneInStageM)
        assertEquals(1f, s.progress, 0f)
        assertTrue(s.finished)
    }

    @Test fun `음수 거리는 0m로 본다`() {
        val s = at(-5)
        assertEquals(1, s.stageNumber)
        assertEquals(0L, s.teamDistanceM)
        assertEquals(0L, s.doneInStageM)
    }

    @Test fun `progressOf는 같은 스테이지 안 위치, 밖이면 0 또는 1`() {
        val s = at(66_800)
        assertEquals(0f, s.progressOf(40_000), 0f)
        assertEquals(0.5f, s.progressOf(66_500), 0.0001f)
        assertEquals(1f, s.progressOf(90_000), 0f)
    }
}
