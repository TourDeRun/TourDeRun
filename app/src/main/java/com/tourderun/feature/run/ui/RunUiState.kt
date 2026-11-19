package com.tourderun.feature.run.ui

import com.tourderun.core.data.team.MyTeam
import com.tourderun.core.ui.model.StageProgressUi
import com.tourderun.core.ui.model.stageProgressUi
import com.tourderun.domain.model.GpsPoint
import com.tourderun.domain.model.LatLngValue
import com.tourderun.domain.record.thinEvenly
import com.tourderun.domain.route.routeFor
import com.tourderun.feature.run.session.RunPhase
import com.tourderun.feature.run.session.RunState
import kotlin.math.roundToLong

/** S09 러닝 화면 전체 상태. 측정 중 화면은 [RunningUiState]가 그린다. */
sealed interface RunScreenUiState {
    data object Loading : RunScreenUiState

    /** 팀이 있어야 러닝을 시작할 수 있다(기능 명세 3장). */
    data object NoTeam : RunScreenUiState

    /** 시작 전: 권한 안내와 시작 버튼. */
    data class Ready(val stage: StageProgressUi) : RunScreenUiState

    data class Countdown(val sec: Int) : RunScreenUiState

    data class Running(val running: RunningUiState) : RunScreenUiState

    /**
     * 종료했지만 아직 저장·정리되지 않은 러닝.
     * [tooShort]이면 M-05 기준 미달이라 "계속하기 / 저장 없이 종료"를 고르게 한다.
     */
    data class Finished(val distanceM: Double, val movingSec: Long, val tooShort: Boolean) : RunScreenUiState
}

/** 화면이 처리하는 일회성 이벤트. 서비스 시작은 Context가 필요해 화면에서 한다. */
sealed interface RunEvent {
    data object StartService : RunEvent

    /** 다시 시작 전에 화면이 위치 권한을 확인한다. 있으면 [RunViewModel.resume], 없으면 권한 안내. */
    data object RequestResume : RunEvent
    data object OpenResult : RunEvent
    data object Close : RunEvent
}

/** 미니 지도(EXT-02)에 그리는 점 수 상한. 저장되는 점에는 영향이 없다. */
const val MINI_MAP_MAX_POINTS = 500

/** 채택된 점 → 미니 지도 경로. 너무 많으면 고르게 솎아낸다. */
fun miniMapPath(points: List<GpsPoint>): List<LatLngValue> =
    thinEvenly(points.map { LatLngValue(it.lat, it.lng) }, MINI_MAP_MAX_POINTS)

/**
 * 매니저 상태 + 내 팀 → 측정 중 화면 상태.
 * 이번 러닝 거리를 저장 전 팀 거리(캐시)에 더해 스테이지 진행을 보여준다.
 */
fun runningUiState(state: RunState, team: MyTeam, path: List<LatLngValue>): RunningUiState {
    val runM = state.distanceM.roundToLong()
    return RunningUiState(
        distanceM = state.distanceM,
        movingSec = state.movingSec,
        avgPaceSecPerKm = state.averagePaceSecPerKm?.toLong() ?: 0,
        currentPaceSecPerKm = state.currentPaceSecPerKm?.toLong() ?: 0,
        gpsGood = !state.gpsWeak,
        isPaused = state.phase == RunPhase.PAUSED,
        stageWithRun = stageProgressUi(team.standing.distanceM + runM, routeFor(team.season)),
        path = path,
    )
}
