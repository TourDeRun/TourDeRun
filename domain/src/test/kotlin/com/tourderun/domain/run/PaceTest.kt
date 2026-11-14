package com.tourderun.domain.run

import com.tourderun.domain.journey.EARTH_RADIUS_M
import com.tourderun.domain.model.GpsPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaceTest {
    private fun point(distanceM: Double, timeMs: Long) =
        GpsPoint(0.0, Math.toDegrees(distanceM / EARTH_RADIUS_M), timeMs, 5f)

    @Test fun `1800초에 5km면 평균 페이스는 6분`() {
        assertEquals(360, averagePaceSecPerKm(1_800, 5_000.0))
    }

    @Test fun `이동 거리가 없으면 평균 페이스 없음`() {
        assertNull(averagePaceSecPerKm(30, 0.0))
    }

    @Test fun `6분 페이스 점을 1초마다 받아도 현재 페이스는 6분`() {
        val points = (0..10).map { point(it * (1_000.0 / 360.0), it * 1_000L) }
        assertEquals(360, currentPaceSecPerKm(points))
    }

    @Test fun `차량 속도의 거리는 제외하되 관측 시간은 유지`() {
        val points = listOf(point(0.0, 0), point(100.0, 1_000), point(110.0, 5_000))
        assertEquals(500, currentPaceSecPerKm(points))
    }

    @Test fun `움직임이 멈춘 관측 시간도 현재 페이스에 포함`() {
        val points = listOf(point(0.0, 0), point(10.0, 10_000), point(10.0, 20_000), point(10.0, 30_000))
        assertEquals(3_000, currentPaceSecPerKm(points))
    }

    @Test fun `30초 밖의 이전 위치는 현재 페이스에서 제외`() {
        val points = listOf(point(0.0, 0), point(10.0, 10_000), point(40.0, 40_000), point(50.0, 50_000))
        assertEquals(1_000, currentPaceSecPerKm(points))
    }

    @Test fun `잘못된 좌표는 현재 페이스의 최근 시각을 바꾸지 않음`() {
        val points = listOf(point(0.0, 0), point(10.0, 10_000), point(100.0, 100_000).copy(lat = Double.NaN))
        assertEquals(1_000, currentPaceSecPerKm(points))
    }

    @Test fun `뒤늦게 도착한 점은 관측 시간과 거리에 영향을 주지 않음`() {
        val points = listOf(point(0.0, 0), point(10.0, 10_000), point(15.0, 5_000), point(20.0, 20_000))
        assertEquals(1_000, currentPaceSecPerKm(points))
    }

    @Test fun `정확도가 낮은 위치도 경과 시간에는 포함`() {
        val points = listOf(point(0.0, 0), point(10.0, 10_000), point(100.0, 20_000).copy(accuracyM = 20f))
        assertEquals(2_000, currentPaceSecPerKm(points))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `페이스 관측 시간이 0 이하면 예외`() {
        currentPaceSecPerKm(emptyList(), windowSec = 0)
    }

    @Test fun `채택된 이동이 없으면 현재 페이스 없음`() {
        assertNull(currentPaceSecPerKm(listOf(point(0.0, 0), point(1.0, 1_000))))
    }
}
