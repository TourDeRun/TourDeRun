package com.tourderun.domain.run

import com.tourderun.domain.journey.haversineM
import com.tourderun.domain.model.GpsPoint

/** GPS 오차·거리·속도 판정에 사용하는 기준. */
data class FilterConfig(
    val maxAccuracyM: Float = 20f,
    val minStepM: Double = 3.0,
    val maxSpeedKmh: Double = 25.0,
) {
    init {
        require(maxAccuracyM.isFinite() && maxAccuracyM > 0f)
        require(minStepM.isFinite() && minStepM >= 0.0)
        require(maxSpeedKmh.isFinite() && maxSpeedKmh > 0.0)
    }
}

sealed interface FilterDecision {
    data class Accepted(val addedM: Double) : FilterDecision
    data object Jitter : FilterDecision
    data object LowAccuracy : FilterDecision
    data object TooFast : FilterDecision
    data object FirstPoint : FilterDecision

    /** 유효하지 않은 좌표·오차 또는 기준점보다 이전인 시각. */
    data object InvalidPoint : FilterDecision
}

/** 마지막 기준점에 비해 유효하게 이동한 거리만 누적한다. */
class GpsDistanceFilter(private val config: FilterConfig = FilterConfig()) {
    private var anchor: GpsPoint? = null

    var totalM: Double = 0.0
        private set

    fun accept(point: GpsPoint): FilterDecision {
        if (!point.lat.isFinite() || point.lat !in -90.0..90.0 ||
            !point.lng.isFinite() || point.lng !in -180.0..180.0 ||
            !point.accuracyM.isFinite() || point.accuracyM < 0f
        ) {
            return FilterDecision.InvalidPoint
        }

        // RUN-05 기준: 오차가 20m 이상인 점은 기준점으로도 사용하지 않는다.
        if (point.accuracyM >= config.maxAccuracyM) return FilterDecision.LowAccuracy

        val previous = anchor
        if (previous == null) {
            anchor = point
            return FilterDecision.FirstPoint
        }

        // 지연 도착한 점 때문에 속도 계산이나 기준점이 잘못 바뀌지 않게 한다.
        if (point.timeMs <= previous.timeMs) return FilterDecision.InvalidPoint

        val distanceM = haversineM(previous.lat, previous.lng, point.lat, point.lng)
        if (distanceM < config.minStepM) return FilterDecision.Jitter

        val elapsedMs = point.timeMs - previous.timeMs
        val speedKmh = distanceM * 3_600.0 / elapsedMs
        if (speedKmh > config.maxSpeedKmh) {
            // 제외한 구간이 다음 점에서 다시 더해지지 않도록 기준점을 옮긴다.
            anchor = point
            return FilterDecision.TooFast
        }

        totalM += distanceM
        anchor = point
        return FilterDecision.Accepted(distanceM)
    }

    /** 일시정지·재개 시 누적 거리는 유지하고 기준점만 지운다. */
    fun resetAnchor() {
        anchor = null
    }
}
