package com.tourderun.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.SessionState
import com.tourderun.core.data.team.MyTeamSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** S13 위쪽 닉네임·팀 이름. 아직 모르면 null. */
data class ProfileHeader(val nickname: String?, val teamName: String?)

/**
 * S13 프로필. 지금은 닉네임(세션)과 팀 이름(내 팀)만 진짜 값이고 나머지는 임시 상태다.
 * TODO(AUTH-03): 닉네임 수정, 누적 거리·러닝 수·최근 기록을 붙이면서 `ProfileUiState` 전체를 여기서 만든다.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    session: SessionRepository,
    myTeamSource: MyTeamSource,
) : ViewModel() {

    val header: StateFlow<ProfileHeader> = combine(session.state, myTeamSource.observeMyTeam()) { state, team ->
        ProfileHeader(nickname = state.nickname, teamName = team?.standing?.name)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileHeader(null, null))
}

private val SessionState.nickname: String?
    get() = when (this) {
        is SessionState.NoTeam -> user.nickname
        is SessionState.Ready -> user.nickname
        else -> null
    }
