package com.tourderun.core.navigation

import androidx.lifecycle.ViewModel
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** 최초 진입 흐름(S01~S06)이 보는 세션 상태. Composable은 Repository를 직접 주입받지 못해서 이 ViewModel을 거친다. */
@HiltViewModel
class EntryViewModel @Inject constructor(
    private val session: SessionRepository,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = session.state

    /** AUTH-04 로그아웃. 세션 상태가 SignedOut이 되면 화면 이동은 [followEntryStage]가 한다. */
    fun signOut() = session.signOut()
}

/** 세션 상태 → 진입 단계. 아직 모르면(Loading) null. 팀 여부는 `users.teamId`로만 본다. */
fun SessionState.toEntryStage(): EntryStage? {
    return when (this) {
        SessionState.Loading -> null
        SessionState.SignedOut -> EntryStage.SIGNED_OUT
        is SessionState.NeedsNickname -> EntryStage.NEEDS_NICKNAME
        is SessionState.NoTeam -> EntryStage.NEEDS_TEAM
        is SessionState.Ready -> EntryStage.READY
    }
}
