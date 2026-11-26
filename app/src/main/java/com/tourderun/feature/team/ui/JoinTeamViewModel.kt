package com.tourderun.feature.team.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.R
import com.tourderun.core.data.DataError
import com.tourderun.core.data.firestore.TeamDto
import com.tourderun.core.data.firestore.toDataError
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.SessionState
import com.tourderun.core.data.team.JoinOutcome
import com.tourderun.core.data.team.NEW_TEAM_SEASON
import com.tourderun.core.data.team.TeamRepository
import com.tourderun.core.designsystem.component.TeamAppearance
import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.designsystem.component.TeamIcon
import com.tourderun.domain.journey.locate
import com.tourderun.domain.route.routeFor
import com.tourderun.domain.team.JoinRejection
import com.tourderun.domain.team.TEAM_MAX_MEMBERS
import com.tourderun.domain.team.isInviteCodeFormat
import com.tourderun.domain.team.normalizeInviteCode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface JoinTeamAction {
    data class CodeChanged(val value: String) : JoinTeamAction
    data object Join : JoinTeamAction
}

sealed interface JoinTeamEvent {
    /** 참여했다 → 홈으로. */
    data object Joined : JoinTeamEvent
}

/**
 * S05 초대 코드로 참여 (TEAM-03).
 * 6자리가 되면 바로 팀을 찾아 미리 보기를 띄우고(D-09), 참가하기를 누르면 트랜잭션으로 참여한다.
 */
@HiltViewModel
class JoinTeamViewModel @Inject constructor(
    private val session: SessionRepository,
    private val teamRepository: TeamRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinTeamUiState())
    val uiState: StateFlow<JoinTeamUiState> = _uiState.asStateFlow()

    private val _events = Channel<JoinTeamEvent>(Channel.BUFFERED)
    val events: Flow<JoinTeamEvent> = _events.receiveAsFlow()

    private var lookupJob: Job? = null

    fun onAction(action: JoinTeamAction) {
        when (action) {
            is JoinTeamAction.CodeChanged -> onCodeChanged(action.value)
            JoinTeamAction.Join -> join()
        }
    }

    private fun onCodeChanged(raw: String) {
        val code = normalizeInviteCode(raw).take(JoinTeamUiState.CODE_LENGTH)
        if (code == _uiState.value.code) return
        lookupJob?.cancel()   // 이전 코드로 찾던 건 버린다
        _uiState.update { it.copy(code = code, preview = null, errorRes = null) }
        if (code.length < JoinTeamUiState.CODE_LENGTH) return
        if (!isInviteCodeFormat(code)) {
            _uiState.update { it.copy(errorRes = R.string.join_error_not_found) }
            return
        }
        lookupJob = viewModelScope.launch {
            teamRepository.findTeamByCode(code)
                .onSuccess { team ->
                    _uiState.update {
                        when {
                            team == null -> it.copy(errorRes = R.string.join_error_not_found)
                            team.memberCount >= TEAM_MAX_MEMBERS ->
                                it.copy(preview = joinPreviewUi(team), errorRes = R.string.join_error_full)
                            else -> it.copy(preview = joinPreviewUi(team))
                        }
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(errorRes = joinErrorRes(error.toDataError())) } }
        }
    }

    private fun join() {
        val current = _uiState.value
        if (!current.canJoin) return
        val user = (session.state.value as? SessionState.NoTeam)?.user
        if (user == null) {
            _uiState.update { it.copy(errorRes = R.string.join_error_already) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, errorRes = null) }
        viewModelScope.launch {
            teamRepository.joinTeam(uid = user.uid, nickname = user.nickname, code = current.code)
                .onSuccess { outcome ->
                    when (outcome) {
                        is JoinOutcome.Joined -> _events.trySend(JoinTeamEvent.Joined)
                        is JoinOutcome.Rejected -> _uiState.update {
                            it.copy(isSubmitting = false, errorRes = rejectionRes(outcome.reason))
                        }
                        JoinOutcome.NotFound -> _uiState.update {
                            it.copy(isSubmitting = false, preview = null, errorRes = R.string.join_error_not_found)
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSubmitting = false, errorRes = joinErrorRes(error.toDataError())) }
                }
        }
    }
}

/** 팀 문서 → S05 미리 보기. 스테이지는 팀 거리로 계산한다(저장하지 않음). */
internal fun joinPreviewUi(team: TeamDto): JoinTeamPreviewUi {
    val route = routeFor(NEW_TEAM_SEASON)
    val stageIndex = locate(team.distanceM, route).stageIndex
    return JoinTeamPreviewUi(
        name = team.name,
        appearance = TeamAppearance(
            color = TeamColor.entries.getOrNull(team.color) ?: TeamColor.BLUE,
            icon = TeamIcon.entries.getOrNull(team.icon) ?: TeamIcon.RUNNER,
        ),
        memberCount = team.memberCount,
        maxMembers = TEAM_MAX_MEMBERS,
        stageNumber = stageIndex + 1,
        fromCity = route.cities[stageIndex].nameKo,
        toCity = route.cities[stageIndex + 1].nameKo,
    )
}

internal fun rejectionRes(reason: JoinRejection): Int = when (reason) {
    JoinRejection.ALREADY_IN_TEAM -> R.string.join_error_already
    JoinRejection.FULL -> R.string.join_error_full
}

/** 찾기·참여 실패 → 문구. 트랜잭션은 온라인 필수라 연결 문제면 다시 시도를 권한다. */
internal fun joinErrorRes(error: DataError): Int = when (error) {
    DataError.Unavailable -> R.string.join_error_network
    DataError.PermissionDenied, DataError.Other -> R.string.join_error_failed
}
