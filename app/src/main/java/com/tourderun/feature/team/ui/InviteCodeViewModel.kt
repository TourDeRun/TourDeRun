package com.tourderun.feature.team.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.firestore.TeamDto
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.teamId
import com.tourderun.core.data.session.uid
import com.tourderun.core.data.team.NEW_TEAM_SEASON
import com.tourderun.core.data.team.TeamRepository
import com.tourderun.core.designsystem.component.TeamAppearance
import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.designsystem.component.TeamIcon
import com.tourderun.domain.route.routeFor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * S06 초대 코드 (TEAM-02). 내 팀 문서와 내 members 문서의 `inviteCode`를 실시간으로 읽는다.
 * 팀을 만든 직후에도 기기 캐시에 바로 있으므로 기다리지 않는다. 팀 정보 화면(S14)에서 다시 열어도 같은 값.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InviteCodeViewModel @Inject constructor(
    session: SessionRepository,
    teamRepository: TeamRepository,
) : ViewModel() {

    /** null이면 불러오는 중(팀이 없거나 아직 값이 없음). */
    val uiState: StateFlow<InviteCodeUiState?> = session.state
        .map { state -> state.uid?.let { uid -> state.teamId?.let { teamId -> uid to teamId } } }
        .distinctUntilChanged()
        .flatMapLatest { key ->
            if (key == null) {
                flowOf(null)
            } else {
                val (uid, teamId) = key
                combine(teamRepository.observeTeam(teamId), teamRepository.observeInviteCode(teamId, uid)) { team, code ->
                    val teamDto = (team as? Loaded.Ready)?.value
                    val inviteCode = (code as? Loaded.Ready)?.value
                    if (teamDto == null || inviteCode == null) null else inviteCodeUiState(teamDto, inviteCode)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** 팀 문서 + 초대 코드 → S06 상태. 아이콘·색 번호가 범위 밖이면 기본값(M-06). */
internal fun inviteCodeUiState(team: TeamDto, code: String): InviteCodeUiState {
    val route = routeFor(NEW_TEAM_SEASON)
    return InviteCodeUiState(
        teamName = team.name,
        appearance = TeamAppearance(
            color = TeamColor.entries.getOrNull(team.color) ?: TeamColor.BLUE,
            icon = TeamIcon.entries.getOrNull(team.icon) ?: TeamIcon.RUNNER,
        ),
        code = code,
        memberCount = team.memberCount,
        season = NEW_TEAM_SEASON,
        routeFrom = route.cities.first().nameKo,
        routeTo = route.cities.last().nameKo,
        routeKm = (route.totalM / 1_000).toInt(),
    )
}
