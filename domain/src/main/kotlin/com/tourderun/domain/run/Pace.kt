package com.tourderun.domain.run

import com.tourderun.domain.model.GpsPoint
import kotlin.math.roundToInt

/** 움직인 시간과 거리로 평균 페이스(초/km)를 구한다. 거리가 없으면 null이다. */
fun averagePaceSecPerKm(movingSec: Long, distanceM: Double): Int? {
    require(movingSec >= 0) { "움직인 시간은 음수일 수 없다" }
    require(distanceM.isFinite() && distanceM >= 0) { "거리는 유한한 0 이상의 값이어야 한다" }
    if (distanceM == 0.0) return null
    return (movingSec.toDouble() * 1_000.0 / distanceM).coerceAtMost(Int.MAX_VALUE.toDouble()).roundToInt()
}

/**
 * 최근 위치의 채택된 이동 거리와 관측 시간으로 현재 페이스를 계산한다.
 * 일시정지한 구간은 호출자가 목록에서 제외하거나 목록을 비운다.
 */
fun currentPaceSecPerKm(recent: List<GpsPoint>, windowSec: Int = 30): Int? {
    require(windowSec > 0) { "페이스 관측 시간은 0보다 커야 한다" }
    // 좌표 자체가 잘못된 점은 최근 시각을 결정하는 데에도 사용하지 않는다.
    val valid = recent.filter { GpsDistanceFilter().accept(it) != FilterDecision.InvalidPoint }
    val newest = valid.maxOfOrNull { it.timeMs } ?: return null
    val windowMs = windowSec.toLong() * 1_000L
    val start = if (newest < Long.MIN_VALUE + windowMs) Long.MIN_VALUE else newest - windowMs
    val filter = GpsDistanceFilter()
    var firstTime: Long? = null
    var lastTime: Long? = null
    for (point in valid) {
        if (point.timeMs < start || point.timeMs > newest) continue
        // 늦게 도착하거나 같은 시각인 점은 관측 시간을 바꾸지 않는다.
        if (lastTime?.let { point.timeMs <= it } == true) continue
        when (filter.accept(point)) {
            FilterDecision.FirstPoint -> { firstTime = point.timeMs; lastTime = point.timeMs }
            FilterDecision.TooFast, FilterDecision.Jitter, FilterDecision.LowAccuracy,
            is FilterDecision.Accepted -> if (firstTime != null) lastTime = point.timeMs
            FilterDecision.InvalidPoint -> Unit
        }
    }
    val elapsedMs = (lastTime ?: return null) - (firstTime ?: return null)
    if (filter.totalM <= 0.0 || elapsedMs <= 0L) return null
    return (elapsedMs.toDouble() / filter.totalM).coerceAtMost(Int.MAX_VALUE.toDouble()).roundToInt()
}
