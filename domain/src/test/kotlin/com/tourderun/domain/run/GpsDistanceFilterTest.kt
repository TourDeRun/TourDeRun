package com.tourderun.domain.run

import com.tourderun.domain.journey.EARTH_RADIUS_M
import com.tourderun.domain.model.GpsPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsDistanceFilterTest {
    // 적도에서 동쪽으로 distanceM만큼 떨어진 점을 만든다.
    private fun point(distanceM: Double, timeMs: Long, accuracyM: Float = 5f) = GpsPoint(
        lat = 0.0,
        lng = Math.toDegrees(distanceM / EARTH_RADIUS_M),
        timeMs = timeMs,
        accuracyM = accuracyM,
    )

    @Test fun `오차가 20m 이상인 점은 기준점이 되지 않음`() {
        val filter = GpsDistanceFilter()
        assertEquals(FilterDecision.LowAccuracy, filter.accept(point(0.0, 0, 20f)))
        assertEquals(FilterDecision.FirstPoint, filter.accept(point(0.0, 1_000, 19f)))
        assertEquals(0.0, filter.totalM, 0.000_001)
    }

    @Test fun `3m 미만 점은 기준점을 유지하고 다음 이동을 합산`() {
        val filter = GpsDistanceFilter()
        assertEquals(FilterDecision.FirstPoint, filter.accept(point(0.0, 0)))
        assertEquals(FilterDecision.Jitter, filter.accept(point(2.8, 1_000)))

        val decision = filter.accept(point(5.6, 2_000))
        assertTrue(decision is FilterDecision.Accepted)
        assertEquals(5.6, filter.totalM, 0.000_001)
    }

    @Test fun `정확도가 낮은 점은 기준점과 누적 거리를 바꾸지 않음`() {
        val filter = GpsDistanceFilter()
        filter.accept(point(0.0, 0))
        assertEquals(FilterDecision.LowAccuracy, filter.accept(point(4.0, 1_000, 25f)))
        filter.accept(point(5.0, 2_000))
        assertEquals(5.0, filter.totalM, 0.000_001)
    }

    @Test fun `3m 경계에서 미만은 제외하고 이상은 더함`() {
        val filter = GpsDistanceFilter()
        filter.accept(point(0.0, 0))
        assertEquals(FilterDecision.Jitter, filter.accept(point(2.999, 1_000)))
        assertTrue(filter.accept(point(3.001, 2_000)) is FilterDecision.Accepted)
        assertEquals(3.001, filter.totalM, 0.000_001)
    }

    @Test fun `시속 25km 기준 아래는 더하고 초과는 제외`() {
        val below = GpsDistanceFilter()
        below.accept(point(0.0, 0))
        assertTrue(below.accept(point(6.94, 1_000)) is FilterDecision.Accepted)

        val above = GpsDistanceFilter()
        above.accept(point(0.0, 0))
        assertEquals(FilterDecision.TooFast, above.accept(point(6.95, 1_000)))
        assertEquals(0.0, above.totalM, 0.000_001)
    }

    @Test fun `시속 25km 초과 구간은 제외하고 새 위치를 기준점으로 삼음`() {
        val filter = GpsDistanceFilter()
        filter.accept(point(0.0, 0))
        assertEquals(FilterDecision.TooFast, filter.accept(point(30.0, 1_000)))
        assertEquals(0.0, filter.totalM, 0.000_001)

        assertTrue(filter.accept(point(35.0, 3_000)) is FilterDecision.Accepted)
        assertEquals(5.0, filter.totalM, 0.000_001)
    }

    @Test fun `재개하면 누적 거리를 유지하고 이동한 공백 구간은 제외`() {
        val filter = GpsDistanceFilter()
        filter.accept(point(0.0, 0))
        filter.accept(point(5.0, 2_000))
        filter.resetAnchor()

        assertEquals(FilterDecision.FirstPoint, filter.accept(point(100.0, 12_000)))
        assertEquals(5.0, filter.totalM, 0.000_001)
        filter.accept(point(105.0, 14_000))
        assertEquals(10.0, filter.totalM, 0.000_001)
    }

    @Test fun `기준점과 시각이 같거나 더 오래된 점은 무시`() {
        val filter = GpsDistanceFilter()
        filter.accept(point(0.0, 1_000))
        assertEquals(FilterDecision.InvalidPoint, filter.accept(point(5.0, 1_000)))
        assertEquals(FilterDecision.InvalidPoint, filter.accept(point(5.0, 500)))

        assertTrue(filter.accept(point(5.0, 3_000)) is FilterDecision.Accepted)
        assertEquals(5.0, filter.totalM, 0.000_001)
    }

    @Test fun `잘못된 좌표나 오차는 무시`() {
        val filter = GpsDistanceFilter()
        assertEquals(FilterDecision.InvalidPoint, filter.accept(point(0.0, 0).copy(lat = Double.NaN)))
        assertEquals(FilterDecision.InvalidPoint, filter.accept(point(0.0, 0).copy(lng = 181.0)))
        assertEquals(FilterDecision.InvalidPoint, filter.accept(point(0.0, 0, -1f)))
        assertEquals(FilterDecision.FirstPoint, filter.accept(point(0.0, 1_000)))
    }
}
