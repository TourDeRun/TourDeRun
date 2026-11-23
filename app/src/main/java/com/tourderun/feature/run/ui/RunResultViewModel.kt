package com.tourderun.feature.run.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.race.RaceRepository
import com.tourderun.core.data.race.RaceSummary
import com.tourderun.core.ui.model.stageProgressUi
import com.tourderun.domain.journey.locate
import com.tourderun.domain.race.overtakeOf
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** S10 일회성 이벤트. */
sealed interface RunResultEvent {
    /** 저장할 러닝이 없거나 저장을 시작하지 못했거나 팀 없이 저장함. 결과 화면을 닫고 홈으로. */
    data object Close : RunResultEvent
}

/**
 * S10 러닝 결과 (REC-01, REC-06). 화면이 열리면 [RunSaveCoordinator]로 종료된 러닝을 저장하고 결과 값을 만든다.
 * 같은 러닝은 한 번만 저장되므로 화면 회전·재진입에서 다시 저장하지 않는다.
 *
 * 앞 팀 추월(RACE-02)은 결과를 먼저 보인 뒤 [loadOvertake]로 채운다. 순위 목록을 못 받으면 추월 카드 없이 그대로 둔다.
 */
@HiltViewModel
class RunResultViewModel @Inject constructor(
    private val coordinator: RunSaveCoordinator,
    private val raceRepository: RaceRepository,
) : ViewModel() {

    /** null이면 저장 중(아직 그릴 값이 없음). */
    private val mutableUiState = MutableStateFlow<RunResultUiState?>(null)
    val uiState: StateFlow<RunResultUiState?> = mutableUiState.asStateFlow()

    private val events = Channel<RunResultEvent>(Channel.BUFFERED)
    val eventFlow: Flow<RunResultEvent> = events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val saved = coordinator.saveFinished()
            val state = saved?.let { runResultUiState(it) }
            if (saved == null || state == null) {
                events.send(RunResultEvent.Close)
                return@launch
            }
            mutableUiState.value = state
            val overtake = loadOvertake(saved, raceRepository) ?: return@launch
            mutableUiState.update { it?.copy(overtake = overtake) }
        }
    }
}

/**
 * 저장한 러닝 → S10 상태. 거리는 저장한 정수 m, 페이스는 저장 값과 같은 계산(저장 거리 기준).
 * 팀 거리 전·후는 팀과 함께 저장한 결과([SavedRun.outcome])만 쓴다. 추월은 비워 두고 [loadOvertake]가 채운다.
 *
 * @return 거리가 0이거나 팀 없이 저장했으면(outcome null) null(화면을 닫고 홈으로)
 */
fun runResultUiState(saved: SavedRun): RunResultUiState? {
    val paceSecPerKm = averagePaceSecPerKm(saved.result.movingSec, saved.distanceM.toDouble()) ?: return null
    val team = saved.team ?: return null
    val outcome = saved.outcome ?: return null
    return RunResultUiState(
        title = saved.title,
        distanceM = saved.distanceM,
        movingSec = saved.result.movingSec,
        paceSecPerKm = paceSecPerKm.toLong(),
        teamBeforeM = outcome.beforeTeamM,
        stageAfter = stageProgressUi(outcome.afterTeamM, routeFor(team.season)),
        overtake = null,
    )
}

/** 순위 목록 첫 값을 기다리는 최대 시간. 결과 화면은 먼저 보이고, 이 안에 못 받으면 추월 카드 없이 둔다. */
internal const val RACE_WAIT_MS = 1_000L

/**
 * 러닝 전 내 스테이지의 순위 목록을 한 번 받아 추월을 계산한다(RACE-02).
 *
 * - 리스너는 러닝 전 팀 값(거리 = outcome의 before)으로 연다 → 러닝 전 스테이지 목록. 첫 값만 받고 닫는다.
 *   보통 홈 순위 리스너가 같은 쿼리를 캐시에 두고 있어 바로 온다.
 * - [timeoutMs] 안에 못 받거나, 값 없이 끝나거나, 실패([Loaded.Failed])면 null.
 */
internal suspend fun loadOvertake(saved: SavedRun, race: RaceRepository, timeoutMs: Long = RACE_WAIT_MS): OvertakeUi? {
    val team = saved.team ?: return null
    val outcome = saved.outcome ?: return null
    val before = team.standing.copy(distanceM = outcome.beforeTeamM)
    val first = withTimeoutOrNull(timeoutMs) { race.observeRace(team.season, before).firstOrNull() }
    return overtakeUi(saved, first)
}

/**
 * 저장 결과 + 러닝 전 스테이지 순위 목록 → 추월 카드. 추월이 없거나 목록이 없으면 null.
 *
 * - 내 거리는 목록 값을 믿지 않고 [SavedRun.outcome]의 전·후를 쓴다(저장하면 기기 캐시의 내 팀 거리가 바로 오른다).
 * - 러닝 전 도달 시각은 저장 직전에 읽은 팀 값([SavedRun.team])을 쓴다(러닝 전 동점 팀과의 앞뒤).
 * - 목록이 러닝 전 스테이지의 것이 아니면 null. 스테이지를 넘어간 러닝도 null([overtakeOf]).
 */
fun overtakeUi(saved: SavedRun, race: Loaded<RaceSummary>?): OvertakeUi? {
    val team = saved.team ?: return null
    val outcome = saved.outcome ?: return null
    val summary = (race as? Loaded.Ready)?.value ?: return null
    val route = routeFor(team.season)
    if (summary.stageIndex != locate(outcome.beforeTeamM, route).stageIndex) return null
    val overtake = overtakeOf(
        stageTeams = summary.stageTeams.map { it.standing },
        myTeamId = team.standing.teamId,
        beforeM = outcome.beforeTeamM,
        afterM = outcome.afterTeamM,
        beforeReachedAtMs = team.standing.reachedAtMs,
        route = route,
    ) ?: return null
    return OvertakeUi(
        teamName = overtake.teamName,
        fromRank = overtake.fromRank,
        toRank = overtake.toRank,
        gapBeforeM = overtake.gapBeforeM,
        leadNowM = overtake.leadNowM,
    )
}
