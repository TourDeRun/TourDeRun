package com.tourderun.feature.run.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.Clock
import com.tourderun.core.data.record.RunSaver
import com.tourderun.core.data.team.MyTeam
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.core.ui.model.stageProgressUi
import com.tourderun.domain.model.LatLngValue
import com.tourderun.domain.model.TeamStanding
import com.tourderun.domain.route.routeFor
import com.tourderun.domain.run.isTooShortToSave
import com.tourderun.feature.run.session.RunPhase
import com.tourderun.feature.run.session.RunSessionManager
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * S09 러닝 (RUN-02·03·08, EXT-02).
 *
 * 측정 상태는 [RunSessionManager] 하나가 갖고, 이 ViewModel은 사용자 동작을 매니저에 넘기고 화면 상태로 바꾸기만 한다.
 * 위치 수신과 1초 tick은 RunTrackingService가 하므로, 화면을 떠나도 측정은 계속된다.
 */
@HiltViewModel
class RunViewModel @Inject constructor(
    private val manager: RunSessionManager,
    private val clock: Clock,
    // Firestore는 google-services.json이 없으면 만들 수 없어서 필요할 때만 꺼낸다.
    private val runSaver: Lazy<RunSaver>,
    myTeamSource: MyTeamSource,
) : ViewModel() {

    private val events = Channel<RunEvent>(Channel.BUFFERED)
    val eventFlow: Flow<RunEvent> = events.receiveAsFlow()

    /** EXT-02: 미니 지도 선은 5초마다만 갱신한다. */
    private val path: Flow<List<LatLngValue>> = flow {
        while (true) {
            emit(miniMapPath(manager.state.value.acceptedPoints))
            delay(MINI_MAP_REFRESH_MS)
        }
    }

    val uiState: StateFlow<RunScreenUiState> = combine(manager.state, myTeamSource.observeMyTeam(), path) { state, team, path ->
        when {
            state.phase == RunPhase.IDLE && team == null -> RunScreenUiState.NoTeam
            state.phase == RunPhase.IDLE -> RunScreenUiState.Ready(
                stageProgressUi(requireNotNull(team).standing.distanceM, routeFor(team.season)),
            )
            state.phase == RunPhase.COUNTDOWN -> RunScreenUiState.Countdown(state.countdownSec)
            state.phase == RunPhase.FINISHED -> RunScreenUiState.Finished(
                distanceM = state.distanceM,
                movingSec = state.movingSec,
                tooShort = isTooShortToSave(state.distanceM, state.movingSec),
            )
            // 측정 중에 팀 정보가 사라지면(팀에서 내보내짐 등) 이번 러닝 거리만 보인다.
            team == null -> RunScreenUiState.Running(runningUiState(state, NO_TEAM, path))
            else -> RunScreenUiState.Running(runningUiState(state, team, path))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RunScreenUiState.Loading)

    /** 권한 확인 뒤 시작 버튼. 카운트다운을 시작하고 GPS 준비를 위해 서비스를 바로 띄운다. */
    fun start() {
        if (uiState.value !is RunScreenUiState.Ready) return
        viewModelScope.launch {
            if (manager.beginCountdown(newRunId(), clock.nowMs())) events.send(RunEvent.StartService)
        }
    }

    fun cancelCountdown() {
        viewModelScope.launch {
            if (manager.cancelCountdown()) events.send(RunEvent.Close)
        }
    }

    fun togglePause() {
        viewModelScope.launch {
            when (manager.state.value.phase) {
                RunPhase.RUNNING -> manager.pause(clock.nowMs())
                RunPhase.PAUSED -> events.send(RunEvent.RequestResume)
                else -> Unit
            }
        }
    }

    /** 길게 눌러 종료. 기준 미달(M-05)이면 화면에 선택지를 띄우고, 아니면 결과 화면으로 간다. */
    fun stop() {
        viewModelScope.launch {
            val result = manager.finish(clock.nowMs()) ?: return@launch
            if (!isTooShortToSave(result.distanceM, result.movingSec)) events.send(RunEvent.OpenResult)
        }
    }

    /** 짧은 러닝 안내에서 "계속하기". 종료 시각부터 지금까지는 일시정지로 본다. */
    fun continueRun() {
        viewModelScope.launch { events.send(RunEvent.RequestResume) }
    }

    /**
     * 화면이 위치 권한을 확인한 뒤에만 부른다. 권한 없이 재개하면 서비스가 시작되지 않아
     * 화면만 측정 중으로 남기 때문이다(PR #87 리뷰).
     */
    fun resume() {
        viewModelScope.launch {
            if (manager.resume(clock.nowMs())) events.send(RunEvent.StartService)
        }
    }

    /** 시스템이 측정 서비스 시작을 거절했을 때. 측정 중이면 일시정지, 카운트다운이면 취소로 되돌린다. */
    fun onServiceStartFailed() {
        viewModelScope.launch { manager.stopForServiceFailure(clock.nowMs()) }
    }

    /** 종료된 러닝을 저장하지 않고 버린다. */
    fun discard() {
        val runId = manager.state.value.runId ?: return
        viewModelScope.launch {
            if (manager.discard(runId)) events.send(RunEvent.Close)
        }
    }

    fun openResult() {
        viewModelScope.launch { events.send(RunEvent.OpenResult) }
    }

    /**
     * 러닝 ID는 Firestore 문서 ID와 같게 [RunSaver.newRunId]로 만든다.
     * Firebase 설정 파일이 없는 빌드(google-services.json 없음)에서는 같은 길이의 임의 ID로 대신해 측정만은 되게 한다.
     */
    private fun newRunId(): String = runCatching { runSaver.get().newRunId() }.getOrElse { error ->
        Log.w(TAG, "Firestore 없이 임시 러닝 ID 사용", error)
        UUID.randomUUID().toString().replace("-", "").take(RUN_ID_LENGTH)
    }

    private companion object {
        const val TAG = "TDR/run"
        const val MINI_MAP_REFRESH_MS = 5_000L
        const val RUN_ID_LENGTH = 20
        val NO_TEAM = MyTeam(
            season = 1,
            standing = TeamStanding(teamId = "", name = "", distanceM = 0, reachedAtMs = 0),
        )
    }
}
