package com.tourderun.domain.run

/** M-05 최소 저장 기준: 100m 미만 또는 1분 미만이면 저장하지 않는다. */
const val MIN_SAVE_DISTANCE_M = 100.0
const val MIN_SAVE_MOVING_SEC = 60L

/** RUN-08: 채택된 점이 이 시간 동안 없으면 GPS 신호가 약하다고 본다. */
const val GPS_WEAK_AFTER_MS = 10_000L

/** 거리 100m 미만 또는 움직인 시간 1분 미만이면 true (M-05, RUN-02). */
fun isTooShortToSave(distanceM: Double, movingSec: Long): Boolean =
    distanceM < MIN_SAVE_DISTANCE_M || movingSec < MIN_SAVE_MOVING_SEC

/**
 * RUN-08 GPS 신호 약함 판정.
 *
 * @param lastAcceptedMs 이번 구간(재개 이후)에 마지막으로 채택된 점의 시각. 아직 없으면 null
 * @param segmentStartedMs 측정을 시작하거나 재개한 시각. 채택된 점이 없으면 이 시각부터 센다
 * @param nowMs 현재 시각
 */
fun isGpsWeak(lastAcceptedMs: Long?, segmentStartedMs: Long, nowMs: Long): Boolean {
    val since = maxOf(lastAcceptedMs ?: segmentStartedMs, segmentStartedMs)
    return nowMs - since >= GPS_WEAK_AFTER_MS
}
