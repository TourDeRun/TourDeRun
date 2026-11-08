package com.tourderun.domain.model

/** GPS 점 하나 (RUN-05 입력). 시각은 점에 들어 있다. */
data class GpsPoint(
    val lat: Double,
    val lng: Double,
    val timeMs: Long,
    val accuracyM: Float,
)

/**
 * 러닝 종료 결과. 최한솔(RUN) → 오영훈(REC-01)으로 넘기는 값.
 * 11/08 회의에서 확정. 이후 변경은 PR + 두 사람 리뷰.
 */
data class RunResult(
    val runId: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val movingSec: Long,
    /** 측정 중에는 Double, 저장할 때 roundToLong()으로 Long 미터 */
    val distanceM: Double,
    val acceptedPoints: List<GpsPoint>,
)
