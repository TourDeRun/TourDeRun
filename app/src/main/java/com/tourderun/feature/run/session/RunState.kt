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
    val acceptedPoints: List<GpsPoint> = emptyList(),
    val result: RunResult? = null,
)
