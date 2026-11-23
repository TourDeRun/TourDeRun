package com.tourderun.core.data.team

import com.tourderun.core.data.Loaded
import com.tourderun.core.data.firestore.TeamDto
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
}

/** 방금 만든 팀. */
data class CreatedTeam(val teamId: String, val inviteCode: String)
