package com.tourderun.feature.team.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.R
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.firestore.TeamDto
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.teamId
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
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TeamEditAction {
    data class NameChanged(val value: String) : TeamEditAction
    data class ColorSelected(val color: TeamColor) : TeamEditAction
    data class IconSelected(val icon: TeamIcon) : TeamEditAction
    data object Save : TeamEditAction
}

sealed interface TeamEditEvent {
    /** 저장을 시작했다(또는 바뀐 게 없다) → S14로 돌아간다. */
    data object Saved : TeamEditEvent
}

/**
 * S14 팀 정보 수정 (TEAM-05). 화면을 열 때 지금 팀 값을 한 번 읽어 입력칸을 채우고,
 * 그 뒤로는 사용자가 고친 값을 유지한다(다른 팀원 기기의 변경으로 입력이 덮이지 않게).
 */
@HiltViewModel
class TeamEditViewModel @Inject constructor(
    private val session: SessionRepository,
    private val teamRepository: TeamRepository,
) : ViewModel() {

    /** null이면 지금 팀 값을 읽는 중. */
    private val _uiState = MutableStateFlow<TeamEditUiState?>(null)
    val uiState: StateFlow<TeamEditUiState?> = _uiState.asStateFlow()

    private val _events = Channel<TeamEditEvent>(Channel.BUFFERED)
    val events: Flow<TeamEditEvent> = _events.receiveAsFlow()

    /** 처음 읽은 값. 바뀐 게 없으면 쓰지 않는다. */
    private var original: TeamEditUiState? = null

    init {
        val teamId = session.state.value.teamId
        if (teamId != null) {
            viewModelScope.launch {
                val team = teamRepository.observeTeam(teamId)
                    .filterIsInstance<Loaded.Ready<TeamDto?>>()
                    .first()
                    .value ?: return@launch
                val initial = teamEditUiState(team)
                original = initial
                _uiState.value = initial
            }
        }
    }

    fun onAction(action: TeamEditAction) {
        when (action) {
            is TeamEditAction.NameChanged -> _uiState.update { it?.copy(name = action.value, errorRes = null) }
            is TeamEditAction.ColorSelected -> _uiState.update { it?.copy(color = action.color) }
            is TeamEditAction.IconSelected -> _uiState.update { it?.copy(icon = action.icon) }
            TeamEditAction.Save -> save()
        }
    }

    private fun save() {
        val current = _uiState.value ?: return
        if (!current.canSave) return
        val teamId = session.state.value.teamId ?: return
        val edited = current.copy(name = current.name.trim())
        if (edited.sameAs(original)) {
            _events.trySend(TeamEditEvent.Saved)
            return
        }
        teamRepository.updateTeam(teamId, edited.name, icon = edited.icon.ordinal, color = edited.color.ordinal)
            .onSuccess { _events.trySend(TeamEditEvent.Saved) }
            .onFailure { _uiState.update { it?.copy(errorRes = R.string.team_edit_error_failed) } }
    }
}

/** 팀 문서 → 수정 화면 첫 값. 색·아이콘 번호가 범위 밖이면 기본값(M-06). */
internal fun teamEditUiState(team: TeamDto) = TeamEditUiState(
    name = team.name,
    color = TeamColor.entries.getOrNull(team.color) ?: TeamColor.BLUE,
    icon = TeamIcon.entries.getOrNull(team.icon) ?: TeamIcon.RUNNER,
)

private fun TeamEditUiState.sameAs(other: TeamEditUiState?) =
    other != null && name == other.name && color == other.color && icon == other.icon
