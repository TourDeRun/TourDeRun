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

    @Test fun `시속 40km 구간은 누적 거리에 더하지 않음`() {
        val filter = GpsDistanceFilter()
        filter.accept(point(0.0, 0))
        assertTrue(filter.accept(point(10.0, 4_000)) is FilterDecision.Accepted)

        // 1초 동안 약 11.11m를 이동하면 시속 40km다.
        val fortyKmhStepM = 40_000.0 / 3_600.0
        assertEquals(FilterDecision.TooFast, filter.accept(point(10.0 + fortyKmhStepM, 5_000)))
        assertEquals(10.0, filter.totalM, 0.000_001)
    }

    @Test fun `6분 페이스로 1초마다 온 점은 이동 거리를 합산`() {
        val filter = GpsDistanceFilter()
        assertEquals(FilterDecision.FirstPoint, filter.accept(point(0.0, 0)))
        val metersPerSecond = 1_000.0 / 360.0
        var acceptedCount = 0
        var jitterCount = 0

        for (second in 1..10) {
            when (filter.accept(point(metersPerSecond * second, second * 1_000L))) {
                is FilterDecision.Accepted -> acceptedCount++
                FilterDecision.Jitter -> jitterCount++
                else -> throw AssertionError("정상 속도 위치가 예상과 다르게 처리됨")
            }
        }

        assertEquals(5, acceptedCount)
        assertEquals(5, jitterCount)
        assertEquals(metersPerSecond * 10, filter.totalM, 0.000_001)
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

    @Test fun `제자리에서 2m 안쪽으로 1분 동안 떨리면 거리는 0`() {
        val filter = GpsDistanceFilter()
        // 기준점 주변을 ±2m로 오가는 점 60개
        (0..60).forEach { filter.accept(point(if (it % 2 == 0) 0.0 else 2.0, it * 1_000L)) }
        assertEquals(0.0, filter.totalM, 0.000_001)
    }

    @Test fun `오차 19_9m 점은 쓰고 20m 점은 버린다`() {
        val filter = GpsDistanceFilter()
        assertEquals(FilterDecision.FirstPoint, filter.accept(point(0.0, 0, 19.9f)))
        assertEquals(FilterDecision.LowAccuracy, filter.accept(point(10.0, 2_000, 20f)))
        assertTrue(filter.accept(point(10.0, 3_000, 19.9f)) is FilterDecision.Accepted)
        assertEquals(10.0, filter.totalM, 0.01)
    }

    @Test fun `차를 타고 이동한 뒤 다시 달리면 차 구간만 빠진다`() {
        val filter = GpsDistanceFilter()
        filter.accept(point(0.0, 0))
        filter.accept(point(10.0, 4_000)) // 9km/h 달리기
        assertEquals(FilterDecision.TooFast, filter.accept(point(1_010.0, 64_000))) // 60km/h 차량
        assertTrue(filter.accept(point(1_020.0, 68_000)) is FilterDecision.Accepted)
        assertEquals(20.0, filter.totalM, 0.01)
    }
}
