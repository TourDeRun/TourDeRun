package com.tourderun.feature.team.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tourderun.R
import com.tourderun.core.data.DataError
import com.tourderun.core.data.firestore.toDataError
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.SessionState
import com.tourderun.core.data.team.TeamRepository
import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.designsystem.component.TeamIcon
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** S04에서 화면이 ViewModel에 알리는 일. */
sealed interface CreateTeamAction {
    data class NameChanged(val value: String) : CreateTeamAction
    data class ColorSelected(val color: TeamColor) : CreateTeamAction
    data class IconSelected(val icon: TeamIcon) : CreateTeamAction
    data object Submit : CreateTeamAction
}

/** 한 번만 일어나는 일. */
sealed interface CreateTeamEvent {
    /** 팀을 만들었다 → 홈 위에 S06 초대 코드 화면을 연다. */
    data object Created : CreateTeamEvent
}

/**
 * S04 팀 만들기 (TEAM-01). 만든 팀의 정보(이름·코드)는 S06이 Firestore에서 직접 읽으므로 여기서는 넘기지 않는다.
 */
@HiltViewModel
class CreateTeamViewModel @Inject constructor(
    private val session: SessionRepository,
    private val teamRepository: TeamRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTeamUiState())
    val uiState: StateFlow<CreateTeamUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreateTeamEvent>(Channel.BUFFERED)
    val events: Flow<CreateTeamEvent> = _events.receiveAsFlow()

    fun onAction(action: CreateTeamAction) {
        when (action) {
            is CreateTeamAction.NameChanged -> _uiState.update { it.copy(name = action.value, errorRes = null) }
            is CreateTeamAction.ColorSelected -> _uiState.update { it.copy(color = action.color) }
            is CreateTeamAction.IconSelected -> _uiState.update { it.copy(icon = action.icon) }
            CreateTeamAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _uiState.value
        if (current.isSubmitting || !current.isValid) return

        // 팀이 없는 로그인 사용자만 만들 수 있다 (TEAM-01: 이미 팀이 있으면 만들 수 없다. 보안 규칙도 막는다)
        val user = (session.state.value as? SessionState.NoTeam)?.user
        if (user == null) {
            _uiState.update { it.copy(errorRes = R.string.create_team_error_already) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorRes = null) }
        viewModelScope.launch {
            teamRepository.createTeam(
                uid = user.uid,
                nickname = user.nickname,
                name = current.name,
                icon = current.icon.ordinal,     // enum의 순서 번호(0~7)가 곧 저장값 (M-06)
                color = current.color.ordinal,
            )
                .onSuccess { _events.trySend(CreateTeamEvent.Created) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSubmitting = false, errorRes = createTeamErrorRes(error.toDataError()))
                    }
                }
        }
    }
}

/** 팀 만들기 실패 → 문구. 연결 문제면 다시 시도를 권한다. */
internal fun createTeamErrorRes(error: DataError): Int = when (error) {
    DataError.Unavailable -> R.string.create_team_error_network
    DataError.PermissionDenied, DataError.Other -> R.string.create_team_error_failed
}
