package com.tourderun.feature.run.session

import com.tourderun.domain.model.GpsPoint
import com.tourderun.domain.model.RunResult

enum class RunPhase { IDLE, COUNTDOWN, RUNNING, PAUSED, FINISHED }

data class RunState(
    val phase: RunPhase = RunPhase.IDLE,
    val runId: String? = null,
    val countdownSec: Int = 0,
    val distanceM: Double = 0.0,
    val movingSec: Long = 0,
    val averagePaceSecPerKm: Int? = null,
    val currentPaceSecPerKm: Int? = null,
    /** RUN-08: 채택된 점이 10초 동안 없음. 측정 중일 때만 true가 된다. */
    val gpsWeak: Boolean = false,
    val acceptedPoints: List<GpsPoint> = emptyList(),
    val result: RunResult? = null,
)

/** RUN-07 복구 안내에 보여 줄 값. [endMs]는 "지금까지 저장"할 때 [RunSessionManager.finish]에 넘기는 종료 시각. */
data class RecoveredRun(
    val runId: String,
    val distanceM: Double,
    val movingSec: Long,
    val endMs: Long,
    /** 종료까지 누른 뒤 저장 전에 앱이 꺼진 경우. 이어하기 대신 저장·버리기만 고른다. */
    val finished: Boolean,
    /** 저장을 시작했지만 서버가 받아들이지 않은 러닝(#99). [finished]도 true다. */
    val saveRejected: Boolean = false,
)
