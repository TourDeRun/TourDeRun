package com.tourderun.feature.team.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.Clock
import com.tourderun.R
import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.firestore.toDataError
import com.tourderun.core.data.run.RunActivity
import com.tourderun.core.ui.blockedMessageRes
import com.tourderun.core.data.firestore.TeamDto
import com.tourderun.core.data.firestore.TeamMemberDto
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.teamId
import com.tourderun.core.data.session.uid
import com.tourderun.core.data.stats.StatsRepository
import com.tourderun.core.data.team.NEW_TEAM_SEASON
import com.tourderun.core.data.team.TeamRepository
import com.tourderun.core.designsystem.component.TeamAppearance
import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.designsystem.component.TeamIcon
import com.tourderun.core.ui.model.MemberUi
import com.tourderun.core.ui.model.TeamUi
import com.tourderun.core.ui.model.teamMembersUi
import com.tourderun.domain.team.TEAM_MAX_MEMBERS
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * S14 팀 정보 (TEAM-04, EXT-03). 팀 문서·팀원 목록·내 초대 코드·이번 주 통계를 실시간으로 합친다.
 * 팀원 문서가 바뀌면(참여·러닝 저장) 화면이 바로 바뀐다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TeamViewModel @Inject constructor(
    private val session: SessionRepository,
    private val teamRepository: TeamRepository,
    statsRepository: StatsRepository,
    clock: Clock,
    private val runActivity: RunActivity,
) : ViewModel() {

    private val _leave = MutableStateFlow(LeaveUiState())
    /** TEAM-07 나가기 확인 창·진행 상태. */
    val leave: StateFlow<LeaveUiState> = _leave.asStateFlow()

    private val _kick = MutableStateFlow(KickUiState())
    /** TEAM-06 내보내기 확인 창·진행 상태. */
    val kick: StateFlow<KickUiState> = _kick.asStateFlow()

    private val _events = Channel<TeamEvent>(Channel.BUFFERED)
    val events: Flow<TeamEvent> = _events.receiveAsFlow()

    fun onAction(action: TeamAction) {
        when (action) {
            // RUN-02: 저장소를 지금 읽은 값으로 판단한다(미리 받아 둔 값은 첫 조회 전 null일 수 있다, #132 리뷰)
            TeamAction.LeaveClick -> viewModelScope.launch {
                val reason = runActivity.busyReason.first()
                if (reason != null) _events.send(TeamEvent.ShowMessage(reason.blockedMessageRes()))
                else _leave.update { it.copy(showConfirm = true, errorRes = null) }
            }
            TeamAction.LeaveDismiss -> _leave.update { if (it.isLeaving) it else LeaveUiState() }
            TeamAction.LeaveConfirm -> leaveTeam()
            is TeamAction.KickClick -> _kick.value = KickUiState(target = action.member)
            TeamAction.KickDismiss -> _kick.update { if (it.isKicking) it else KickUiState() }
            TeamAction.KickConfirm -> kickMember()
        }
    }

    private fun kickMember() {
        val current = _kick.value
        val target = current.target ?: return
        if (current.isKicking) return
        val state = session.state.value
        val uid = state.uid ?: return
        val teamId = state.teamId ?: return
        _kick.update { it.copy(isKicking = true, errorRes = null) }
        viewModelScope.launch {
            teamRepository.kickMember(leaderUid = uid, teamId = teamId, targetUid = target.uid)
                .onSuccess { _kick.value = KickUiState() }   // 목록은 리스너가 바로 바꾼다
                .onFailure { error ->
                    _kick.update { it.copy(isKicking = false, errorRes = kickErrorRes(error.toDataError())) }
                }
        }
    }

    private fun leaveTeam() {
        if (_leave.value.isLeaving) return
        val state = session.state.value
        val uid = state.uid ?: return
        val teamId = state.teamId ?: return
        _leave.update { it.copy(isLeaving = true, errorRes = null) }
        viewModelScope.launch {
            // 확인 창이 열린 사이 러닝이 시작·저장 대기가 됐을 수 있어 실행 직전에 다시 본다
            val reason = runActivity.busyReason.first()
            if (reason != null) {
                _leave.value = LeaveUiState()
                _events.send(TeamEvent.ShowMessage(reason.blockedMessageRes()))
                return@launch
            }
            teamRepository.leaveTeam(uid, teamId)
                .onSuccess {
                    _leave.value = LeaveUiState()
                    _events.trySend(TeamEvent.Left)
                }
                .onFailure { error ->
                    _leave.update { it.copy(isLeaving = false, errorRes = leaveErrorRes(error.toDataError())) }
                }
        }
    }

    /** 다시 시도할 때마다 1씩 늘린다. 값이 바뀌면 리스너를 새로 붙인다. */
    private val attempt = MutableStateFlow(0)

    /** 불러오기 실패 후 다시 시도. 다시 붙는 동안은 [TeamLoadState.Loading]. */
    fun retry() {
        attempt.update { it + 1 }
    }

    val uiState: StateFlow<TeamLoadState> = combine(
        session.state
            .map { state -> state.uid?.let { uid -> state.teamId?.let { teamId -> uid to teamId } } }
            .distinctUntilChanged(),
        attempt,
    ) { key, _ -> key }
        .flatMapLatest<Pair<String, String>?, TeamLoadState> { key ->
            if (key == null) {
                flowOf(TeamLoadState.Loading)
            } else {
                val (uid, teamId) = key
                combine(
                    teamRepository.observeTeam(teamId),
                    teamRepository.observeMembers(teamId),
                    teamRepository.observeInviteCode(teamId, uid),
                    statsRepository.observeWeekly(teamId, clock.nowMs()),
                ) { team, members, code, weekly ->
                    teamLoadState(
                        team = team,
                        members = members,
                        inviteCode = (code as? Loaded.Ready)?.value.orEmpty(),
                        weeklyMemberM = (weekly as? Loaded.Ready)?.value?.memberM.orEmpty(),
                        myUid = uid,
                    )
                }.onStart { emit(TeamLoadState.Loading) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeamLoadState.Loading)
}

/** S14 불러오기 상태. */
sealed interface TeamLoadState {
    /** 팀·팀원 값을 기다리는 중(팀 문서가 없어 곧 S03으로 옮겨질 때도). */
    data object Loading : TeamLoadState

    /** 팀이나 팀원 목록을 못 읽었다 → 다시 시도. */
    data class Failed(val error: DataError) : TeamLoadState

    data class Ready(val state: TeamUiState) : TeamLoadState
}

/**
 * 리스너 값 → S14 불러오기 상태. 팀·팀원 중 하나라도 실패면 [TeamLoadState.Failed].
 * 초대 코드·이번 주 통계는 보조 값이라 실패해도 빈 값으로 보인다.
 */
internal fun teamLoadState(
    team: Loaded<TeamDto?>,
    members: Loaded<List<TeamMemberDto>>,
    inviteCode: String,
    weeklyMemberM: Map<String, Long>,
    myUid: String,
): TeamLoadState {
    if (team is Loaded.Failed) return TeamLoadState.Failed(team.error)
    if (members is Loaded.Failed) return TeamLoadState.Failed(members.error)
    val teamDto = (team as Loaded.Ready).value ?: return TeamLoadState.Loading
    val memberList = (members as Loaded.Ready).value
    return TeamLoadState.Ready(teamUiState(teamDto, memberList, inviteCode, weeklyMemberM, myUid))
}


/**
 * 팀 + 팀원 + 이번 주 거리 → S14 상태. 팀원 순서·비율·MVP·아바타 색은 홈과 같은 [teamMembersUi]로 만든다.
 */
internal fun teamUiState(
    team: TeamDto,
    members: List<TeamMemberDto>,
    inviteCode: String,
    weeklyMemberM: Map<String, Long>,
    myUid: String,
): TeamUiState {
    val membersUi = teamMembersUi(team.distanceM, members, weeklyMemberM)
    return TeamUiState(
        team = TeamUi(
            name = team.name,
            appearance = TeamAppearance(
                color = TeamColor.entries.getOrNull(team.color) ?: TeamColor.BLUE,
                icon = TeamIcon.entries.getOrNull(team.icon) ?: TeamIcon.RUNNER,
            ),
            season = NEW_TEAM_SEASON,
            memberCount = team.memberCount,
            maxMembers = TEAM_MAX_MEMBERS,
            distanceM = team.distanceM,
        ),
        members = membersUi.members,
        inviteCode = inviteCode,
        isLeader = members.firstOrNull { it.uid == myUid }?.isLeader == true,
        myUid = myUid,
        formerM = membersUi.formerM,
        formerPercent = membersUi.formerPercent,
    )
}

/** TEAM-07 나가기 확인 창 상태. */
data class LeaveUiState(
    val showConfirm: Boolean = false,
    val isLeaving: Boolean = false,
    val errorRes: Int? = null,
)

sealed interface TeamAction {
    data class KickClick(val member: MemberUi) : TeamAction
    data object KickConfirm : TeamAction
    data object KickDismiss : TeamAction
    data object LeaveClick : TeamAction
    data object LeaveConfirm : TeamAction
    data object LeaveDismiss : TeamAction
}

sealed interface TeamEvent {
    /** 팀에서 나갔다 → S03 팀 시작으로. */
    data object Left : TeamEvent

    /** 한 번만 보여 줄 안내 (RUN-02 러닝 중이라 막힘 등). */
    data class ShowMessage(val messageRes: Int) : TeamEvent
}

/** 나가기 실패 → 문구. 트랜잭션이라 연결 문제면 다시 시도를 권한다. */
internal fun leaveErrorRes(error: DataError): Int = when (error) {
    DataError.Unavailable -> R.string.team_leave_error_network
    DataError.PermissionDenied, DataError.Other -> R.string.team_leave_error_failed
}

/** TEAM-06 내보내기 확인 창 상태. [target]이 null이면 창이 닫혀 있다. */
data class KickUiState(
    val target: MemberUi? = null,
    val isKicking: Boolean = false,
    val errorRes: Int? = null,
)

internal fun kickErrorRes(error: DataError): Int = when (error) {
    DataError.Unavailable -> R.string.team_kick_error_network
    DataError.PermissionDenied, DataError.Other -> R.string.team_kick_error_failed
}
