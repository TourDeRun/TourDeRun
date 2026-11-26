package com.tourderun.feature.run.ui

import com.tourderun.core.data.team.MyTeam
import com.tourderun.core.ui.model.StageProgressUi
import com.tourderun.core.ui.model.stageProgressUi
import com.tourderun.domain.model.GpsPoint
import com.tourderun.domain.model.LatLngValue
import com.tourderun.domain.model.TeamStanding
import com.tourderun.domain.record.thinEvenly
import com.tourderun.domain.route.routeFor
import com.tourderun.domain.run.isTooShortToSave
import com.tourderun.feature.run.session.RunPhase
import com.tourderun.feature.run.session.RunState
import kotlin.math.roundToLong

/** S09 러닝 화면 전체 상태. 측정 중 화면은 [RunningUiState]가 그린다. */
sealed interface RunScreenUiState {
    data object Loading : RunScreenUiState

    /** 팀이 있어야 러닝을 시작할 수 있다(기능 명세 3장). */
    data object NoTeam : RunScreenUiState

    /** 시작 전: 권한 안내와 시작 버튼. */
    data class Ready(val stage: StageProgressUi, val season: Int = 1) : RunScreenUiState

    data class Countdown(val sec: Int) : RunScreenUiState

    /**
     * @property tooShortPrompt 종료를 눌렀는데 M-05 기준 미달이라 "계속 달리기 / 저장 없이 종료" 확인 창을 띄운다.
     */
    data class Running(val running: RunningUiState, val tooShortPrompt: Boolean = false) : RunScreenUiState

    /** 종료했지만 아직 저장·정리되지 않은 러닝(결과 화면을 닫았거나 앱을 다시 연 경우). 기준 미달은 [Running.tooShortPrompt]로 보인다. */
    data class Finished(val distanceM: Double, val movingSec: Long) : RunScreenUiState
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

/**
 * 매니저 상태 + 내 팀 + 미니 지도 경로 + 짧은 러닝 확인 창 여부 → 러닝 화면 상태.
 * 확인 창은 일시정지 중에만 보인다. 기준 미달인 채 종료돼 있으면(앱 재시작 복구 등) 항상 확인 창을 띄운다.
 */
fun runScreenUiState(state: RunState, team: MyTeam?, path: List<LatLngValue>, tooShortPrompt: Boolean): RunScreenUiState {
    val runTeam = team ?: NO_TEAM
    return when {
        state.phase == RunPhase.IDLE && team == null -> RunScreenUiState.NoTeam
        state.phase == RunPhase.IDLE -> RunScreenUiState.Ready(
            stageProgressUi(requireNotNull(team).standing.distanceM, routeFor(team.season)),
            team.season,
        )
        state.phase == RunPhase.COUNTDOWN -> RunScreenUiState.Countdown(state.countdownSec)
        // 앱을 다시 열었더니 기준 미달인 채 종료돼 있으면(복구 등) 측정 화면 위에 같은 확인 창을 띄운다.
        state.phase == RunPhase.FINISHED && isTooShortToSave(state.distanceM, state.movingSec) ->
            RunScreenUiState.Running(runningUiState(state, runTeam, path).copy(isPaused = true), tooShortPrompt = true)
        state.phase == RunPhase.FINISHED -> RunScreenUiState.Finished(state.distanceM, state.movingSec)
        // 측정 중에 팀 정보가 사라지면(팀에서 내보내짐 등) 이번 러닝 거리만 보인다.
        else -> RunScreenUiState.Running(
            runningUiState(state, runTeam, path),
            tooShortPrompt = tooShortPrompt && state.phase == RunPhase.PAUSED,
        )
    }
}

/** 측정 중에 팀 정보가 사라졌을 때 쓰는 빈 팀(이번 러닝 거리만 보인다). */
private val NO_TEAM = MyTeam(season = 1, standing = TeamStanding(teamId = "", name = "", distanceM = 0, reachedAtMs = 0))
