package com.tourderun.feature.run.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.team.MyTeam
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.core.ui.model.stageProgressUi
import com.tourderun.domain.record.SaveOutcome
import com.tourderun.domain.record.saveOutcome
import com.tourderun.domain.route.routeFor
import com.tourderun.domain.run.averagePaceSecPerKm
import com.tourderun.feature.run.data.RunSaveCoordinator
import com.tourderun.feature.run.data.SavedRun
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** S10 일회성 이벤트. */
sealed interface RunResultEvent {
    /** 저장할 러닝이 없거나 저장을 시작하지 못함. 결과 화면을 닫고 홈으로. */
    data object Close : RunResultEvent
}

/**
 * S10 러닝 결과 (REC-01, REC-06). 화면이 열리면 [RunSaveCoordinator]로 종료된 러닝을 저장하고 결과 값을 만든다.
 * 같은 러닝은 한 번만 저장되므로 화면 회전·재진입에서 다시 저장하지 않는다.
 */
@HiltViewModel
class RunResultViewModel @Inject constructor(
    private val coordinator: RunSaveCoordinator,
    private val myTeamSource: MyTeamSource,
) : ViewModel() {

    /** null이면 저장 중(아직 그릴 값이 없음). */
    private val mutableUiState = MutableStateFlow<RunResultUiState?>(null)
    val uiState: StateFlow<RunResultUiState?> = mutableUiState.asStateFlow()

    private val events = Channel<RunResultEvent>(Channel.BUFFERED)
    val eventFlow: Flow<RunResultEvent> = events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val saved = coordinator.saveFinished()
            val state = saved?.let { runResultUiState(it, myTeamSource.observeMyTeam().first()) }
            if (state == null) events.send(RunResultEvent.Close) else mutableUiState.value = state
        }
    }
}

/**
 * 저장한 러닝 → S10 상태. 거리는 저장한 정수 m, 페이스는 저장 값과 같은 계산(저장 거리 기준).
 * 팀과 함께 저장했으면 그 결과([SavedRun.outcome])를, 아니면 [displayTeamResult]로 화면 값만 만든다.
 *
 * @param myTeam 팀 없이 저장했을 때 화면에 쓸 팀(`MyTeamSource`의 값)
 * @return 거리가 0이거나 보여 줄 팀이 없으면 null(화면을 닫는다)
 */
fun runResultUiState(saved: SavedRun, myTeam: MyTeam?): RunResultUiState? {
    val paceSecPerKm = averagePaceSecPerKm(saved.result.movingSec, saved.distanceM.toDouble()) ?: return null
    val teamResult = if (saved.team != null && saved.outcome != null) {
        TeamResult(saved.team.season, saved.outcome)
    } else {
        displayTeamResult(saved.distanceM, myTeam) ?: return null
    }
    return RunResultUiState(
        title = saved.title,
        distanceM = saved.distanceM,
        movingSec = saved.result.movingSec,
        paceSecPerKm = paceSecPerKm.toLong(),
        teamBeforeM = teamResult.outcome.beforeTeamM,
        stageAfter = stageProgressUi(teamResult.outcome.afterTeamM, routeFor(teamResult.season)),
        // TODO(RACE-04): 추월 판정을 붙인다. 지금은 표시하지 않는다.
        overtake = null,
    )
}

/** 결과 화면의 팀 거리 값. */
data class TeamResult(val season: Int, val outcome: SaveOutcome)

/**
 * 팀 없이 저장했을 때(지금의 실제 경로) 화면 값만 [myTeam]의 팀 거리로 계산한다. 팀 거리는 서버에 더해지지 않는다.
 *
 * TODO(#45): TeamRepository로 실제 팀과 함께 저장하게 되면 이 분기는 지운다(그때는 항상 [SavedRun.outcome]이 있다).
 */
fun displayTeamResult(distanceM: Long, myTeam: MyTeam?): TeamResult? {
    val team = myTeam ?: return null
    return TeamResult(team.season, saveOutcome(team.standing.distanceM, distanceM, routeFor(team.season)))
}
