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
import com.tourderun.core.data.run.RunBusyReason
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
import com.tourderun.domain.team.TEAM_MAX_MEMBERS
import com.tourderun.domain.team.contributionShares
import com.tourderun.domain.team.weeklyMvps
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    runActivity: RunActivity,
) : ViewModel() {

    /** RUN-02 팀 나가기를 막는 이유. null이면 막지 않는다. */
    private val runBusy: StateFlow<RunBusyReason?> = runActivity.busyReason
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _leave = MutableStateFlow(LeaveUiState())
    /** TEAM-07 나가기 확인 창·진행 상태. */
    val leave: StateFlow<LeaveUiState> = _leave.asStateFlow()

    private val _events = Channel<TeamEvent>(Channel.BUFFERED)
    val events: Flow<TeamEvent> = _events.receiveAsFlow()

    fun onAction(action: TeamAction) {
        when (action) {
            TeamAction.LeaveClick -> {
                val reason = runBusy.value
                if (reason != null) _events.trySend(TeamEvent.ShowMessage(reason.blockedMessageRes()))
                else _leave.update { it.copy(showConfirm = true, errorRes = null) }
            }
            TeamAction.LeaveDismiss -> _leave.update { if (it.isLeaving) it else LeaveUiState() }
            TeamAction.LeaveConfirm -> leaveTeam()
        }
    }

    private fun leaveTeam() {
        if (_leave.value.isLeaving) return
        val state = session.state.value
        val uid = state.uid ?: return
        val teamId = state.teamId ?: return
        _leave.update { it.copy(isLeaving = true, errorRes = null) }
        viewModelScope.launch {
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

    /** null이면 불러오는 중(팀이 없거나 팀·팀원 값이 아직 없음). */
    val uiState: StateFlow<TeamUiState?> = session.state
        .map { state -> state.uid?.let { uid -> state.teamId?.let { teamId -> uid to teamId } } }
        .distinctUntilChanged()
        .flatMapLatest<Pair<String, String>?, TeamUiState?> { key ->
            if (key == null) {
                flowOf(null)
            } else {
                val (uid, teamId) = key
                combine(
                    teamRepository.observeTeam(teamId),
                    teamRepository.observeMembers(teamId),
                    teamRepository.observeInviteCode(teamId, uid),
                    statsRepository.observeWeekly(teamId, clock.nowMs()),
                ) { team, members, code, weekly ->
                    val teamDto = (team as? Loaded.Ready)?.value
                    val memberList = (members as? Loaded.Ready)?.value
                    if (teamDto == null || memberList == null) {
                        null
                    } else {
                        teamUiState(
                            team = teamDto,
                            members = memberList,
                            inviteCode = (code as? Loaded.Ready)?.value.orEmpty(),
                            weeklyMemberM = (weekly as? Loaded.Ready)?.value?.memberM.orEmpty(),
                            myUid = uid,
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/**
 * 팀 + 팀원 + 이번 주 거리 → S14 상태.
 *
 * - 팀원 순서·비율은 `contributionShares`(기여 거리 내림차순, 합 100%). 나간 팀원 몫은 [TeamUiState.formerM]
 * - MVP는 `weeklyMvps`(1인 팀·모두 0km면 없음)
 * - 팀원 아바타 색은 가입 순서대로 `TeamColor` 순서를 돌려 쓴다
 */
internal fun teamUiState(
    team: TeamDto,
    members: List<TeamMemberDto>,
    inviteCode: String,
    weeklyMemberM: Map<String, Long>,
    myUid: String,
): TeamUiState {
    val shares = contributionShares(team.distanceM, members.associate { it.uid to it.contributionM })
    val byUid = members.associateBy { it.uid }
    val colorOf = members.sortedWith(compareBy<TeamMemberDto> { it.joinedAtMs }.thenBy { it.uid })
        .mapIndexed { i, m -> m.uid to TeamColor.entries[i % TeamColor.entries.size] }
        .toMap()
    val mvps = weeklyMvps(weeklyMemberM, byUid.keys)
    val former = shares.firstOrNull { it.uid == null }

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
        members = shares.mapNotNull { share ->
            val member = share.uid?.let { byUid[it] } ?: return@mapNotNull null
            MemberUi(
                uid = member.uid,
                nickname = member.nickname,
                color = colorOf[member.uid] ?: TeamColor.BLUE,
                contributionM = share.distanceM,
                isLeader = member.isLeader,
                isMvp = member.uid in mvps,
                percent = share.percent,
            )
        },
        inviteCode = inviteCode,
        isLeader = byUid[myUid]?.isLeader == true,
        myUid = myUid,
        formerM = former?.distanceM ?: 0L,
        formerPercent = former?.percent ?: 0,
    )
}

/** TEAM-07 나가기 확인 창 상태. */
data class LeaveUiState(
    val showConfirm: Boolean = false,
    val isLeaving: Boolean = false,
    val errorRes: Int? = null,
)

sealed interface TeamAction {
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
