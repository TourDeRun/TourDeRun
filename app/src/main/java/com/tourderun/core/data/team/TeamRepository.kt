package com.tourderun.core.data.team

import com.tourderun.core.data.Loaded
import com.tourderun.core.data.firestore.TeamDto
import com.tourderun.domain.team.JoinRejection
import com.tourderun.core.data.firestore.TeamMemberDto
import kotlinx.coroutines.flow.Flow

/**
 * 팀 (TEAM-01~08). 담당: 권민찬.
 *
 * uid·닉네임·teamId는 부르는 ViewModel이 `SessionRepository.state`에서 읽어 넘긴다.
 * 이 Repository는 로그인 상태를 모른다(#62 리뷰에서 정한 원칙).
 */
interface TeamRepository {

    /** `teams/{teamId}` 실시간. 문서가 없으면 `Ready(null)`. */
    fun observeTeam(teamId: String): Flow<Loaded<TeamDto?>>

    /**
     * TEAM-01 팀 만들기. teams·members(팀장)·inviteCodes·users.teamId를 배치 한 번으로 쓴다.
     * 이미 팀이 있는지는 부르는 쪽이 세션 상태로 먼저 확인한다(보안 규칙도 막는다). 온라인 필수.
     *
     * @param icon 대표 아이콘 번호 0~7 (`TeamIcon.ordinal`)
     * @param color 팀 색 번호 0~7 (`TeamColor.ordinal`)
     * @return 새 팀 ID와 초대 코드 (S06에 바로 보여 준다)
     */
    suspend fun createTeam(uid: String, nickname: String, name: String, icon: Int, color: Int): Result<CreatedTeam>

    /**
     * TEAM-02 내 팀의 초대 코드. `teams/{teamId}/members/{uid}`의 `inviteCode`(팀장은 만든 코드, 팀원은 참여한 코드)를
     * 실시간으로 읽는다. 코드는 팀원만 볼 수 있다(보안 규칙). 문서가 없으면 `Ready(null)`.
     */
    fun observeInviteCode(teamId: String, uid: String): Flow<Loaded<String?>>

    /** TEAM-04 팀원 목록 실시간 (`teams/{teamId}/members`). 같은 팀원만 읽을 수 있다(보안 규칙). 순서는 정하지 않는다. */
    fun observeMembers(teamId: String): Flow<Loaded<List<TeamMemberDto>>>

    /**
     * TEAM-03 코드 → 들어갈 팀 미리 보기 (S05). `inviteCodes/{code}` → `teams/{teamId}`를 **서버에서** 읽는다.
     * 없는 코드면 `success(null)`. 오프라인·시간 초과는 failure.
     *
     * @param code [com.tourderun.domain.team.normalizeInviteCode]로 정리한 6자리
     */
    suspend fun findTeamByCode(code: String): Result<TeamDto?>

    /**
     * TEAM-03 초대 코드로 참여. 트랜잭션(온라인 필수) 안에서 코드·팀 인원·내 teamId를 다시 읽고
     * `teamJoinPlan`(members 생성, teams.memberCount +1, users.teamId)을 쓴다.
     * 실패(오프라인·시간 초과·규칙 거절)는 failure, 참여할 수 없는 이유는 [JoinOutcome]으로 돌려준다.
     */
    suspend fun joinTeam(uid: String, nickname: String, code: String): Result<JoinOutcome>

    /**
     * TEAM-07 팀 나가기. 온라인 필수 트랜잭션에서 팀 인원·내 문서·다음 팀장을 다시 읽고 [teamLeavePlan]을 쓴다.
     * 팀장이면 가장 먼저 가입한 팀원이 팀장이 되고, 마지막 1명이면 팀 문서들을 지운다.
     */
    suspend fun leaveTeam(uid: String, teamId: String): Result<Unit>

    /**
     * TEAM-06 팀장이 팀원을 내보낸다. 온라인 필수 트랜잭션에서 내가 팀장인지·그 사람이 아직 팀원인지·인원을
     * 다시 읽고 [teamKickPlan]을 쓴다.
     */
    suspend fun kickMember(leaderUid: String, teamId: String, targetUid: String): Result<Unit>
}

/** TEAM-03 참여 결과. */
sealed interface JoinOutcome {
    data class Joined(val teamId: String) : JoinOutcome
    data class Rejected(val reason: JoinRejection) : JoinOutcome
    /** 코드 문서나 팀 문서가 없다 (팀이 없어졌거나 잘못 입력). */
    data object NotFound : JoinOutcome
}

/** 방금 만든 팀. */
data class CreatedTeam(val teamId: String, val inviteCode: String)
