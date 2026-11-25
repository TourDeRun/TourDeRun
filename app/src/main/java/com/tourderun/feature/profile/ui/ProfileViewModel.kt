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
import com.tourderun.core.data.team.TeamRepository
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
    /** 아직 모르거나 읽기에 실패하면 null → 화면은 "–" (0과 구분). */
    val totalM: Long? = null,
    val runCount: Long? = null,
    /** 내 members 문서의 역할이 팀장인지. 팀이 없거나 아직 모르면 null → 화면은 역할을 숨긴다. */
    val isLeader: Boolean? = null,
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
 * S13 프로필. 닉네임(세션)·팀 이름(내 팀)·팀 역할(members)·누적 거리·러닝 횟수(users)는 진짜 값이다.
 * 최근 기록 한 줄은 RunRepository 담당(오영훈)이 붙인다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val session: SessionRepository,
    myTeamSource: MyTeamSource,
    teamRepository: TeamRepository,
) : ViewModel() {

    /** 내가 팀장인지. 팀원 목록에서 내 줄을 찾는다(팀장 넘기기·승계로 바뀌면 바로 반영). */
    private val isLeader = session.state
        .map { state -> state.uid?.let { uid -> state.teamId?.let { teamId -> uid to teamId } } }
        .distinctUntilChanged()
        .flatMapLatest<Pair<String, String>?, Boolean?> { key ->
            if (key == null) {
                flowOf(null)
            } else {
                val (uid, teamId) = key
                teamRepository.observeMembers(teamId).map { loaded ->
                    // 읽기 실패면 모름(null)
                    (loaded as? Loaded.Ready)?.value?.firstOrNull { it.uid == uid }?.isLeader
                }
            }
        }

    private val myStats = session.state
        .map { it.uid }
        .distinctUntilChanged()
        .flatMapLatest<String?, MyStats?> { uid ->
            if (uid == null) {
                flowOf(null)
            } else {
                // 읽기 실패면 null → 화면은 0이 아니라 "–"
                session.observeMyStats(uid).map { (it as? Loaded.Ready)?.value }
            }
        }

    val header: StateFlow<ProfileHeader> =
        combine(session.state, myTeamSource.observeMyTeam(), myStats, isLeader) { state, team, stats, leader ->
            ProfileHeader(
                nickname = state.nickname,
                teamName = team?.standing?.name,
                totalM = stats?.totalM,
                runCount = stats?.runCount,
                isLeader = leader,
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
