package com.tourderun.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.R
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.session.MyStats
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.SessionState
import com.tourderun.core.data.session.teamId
import com.tourderun.core.data.session.uid
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.domain.team.validateNickname
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** S13 위쪽 값. 아직 모르면 null·0. */
data class ProfileHeader(
    val nickname: String?,
    val teamName: String?,
    val totalM: Long = 0,
    val runCount: Long = 0,
)

/**
 * AUTH-03 닉네임 수정 창 상태. 창이 닫혀 있으면 null.
 *
 * @property errorRes 저장을 시작하지 못했을 때 문구
 */
data class NicknameEditState(
    val value: String,
    val errorRes: Int? = null,
) {
    /** S02와 같은 규칙(2~10자, 한글·영문·숫자). */
    val isValid: Boolean get() = validateNickname(value.trim()) == null
}

sealed interface ProfileAction {
    data object EditNickname : ProfileAction
    data class NicknameChanged(val value: String) : ProfileAction
    data object SaveNickname : ProfileAction
    data object DismissNickname : ProfileAction
}

/**
 * S13 프로필. 닉네임(세션)·팀 이름(내 팀)·누적 거리·러닝 횟수(users)는 진짜 값이다.
 * TODO(AUTH-03): 팀 역할(팀장/팀원)과 최근 기록 한 줄은 TEAM-04·RunRepository와 합친 뒤 붙인다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val session: SessionRepository,
    myTeamSource: MyTeamSource,
) : ViewModel() {

    private val myStats = session.state
        .map { it.uid }
        .distinctUntilChanged()
        .flatMapLatest<String?, MyStats> { uid ->
            if (uid == null) {
                flowOf(MyStats.EMPTY)
            } else {
                session.observeMyStats(uid).map { (it as? Loaded.Ready)?.value ?: MyStats.EMPTY }
            }
        }

    val header: StateFlow<ProfileHeader> =
        combine(session.state, myTeamSource.observeMyTeam(), myStats) { state, team, stats ->
            ProfileHeader(
                nickname = state.nickname,
                teamName = team?.standing?.name,
                totalM = stats.totalM,
                runCount = stats.runCount,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileHeader(null, null))

    private val _nicknameEdit = MutableStateFlow<NicknameEditState?>(null)
    val nicknameEdit: StateFlow<NicknameEditState?> = _nicknameEdit.asStateFlow()

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.EditNickname ->
                _nicknameEdit.value = NicknameEditState(value = session.state.value.nickname.orEmpty())
            is ProfileAction.NicknameChanged ->
                _nicknameEdit.update { it?.copy(value = action.value, errorRes = null) }
            ProfileAction.SaveNickname -> saveNickname()
            ProfileAction.DismissNickname -> _nicknameEdit.value = null
        }
    }

    private fun saveNickname() {
        val edit = _nicknameEdit.value ?: return
        if (!edit.isValid) return
        val state = session.state.value
        val uid = state.uid ?: return
        val nickname = edit.value.trim()
        if (nickname == state.nickname) {
            _nicknameEdit.value = null
            return
        }
        session.updateNickname(uid = uid, teamId = state.teamId, nickname = nickname)
            .onSuccess { _nicknameEdit.value = null }
            .onFailure { _nicknameEdit.update { it?.copy(errorRes = R.string.profile_nickname_error) } }
    }
}

private val SessionState.nickname: String?
    get() = when (this) {
        is SessionState.NoTeam -> user.nickname
        is SessionState.Ready -> user.nickname
        else -> null
    }
