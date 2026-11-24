package com.tourderun.core.data.team

import com.tourderun.core.data.Loaded
import com.tourderun.core.data.firestore.TeamDto
import com.tourderun.core.data.firestore.TeamMemberDto
import javax.inject.Inject
import javax.inject.Singleton
import com.tourderun.domain.team.joinRejection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Firebase 없이 쓰는 가짜 팀 저장소. 만든 팀을 메모리에만 둔다 (Preview·ViewModel 테스트용). */
@Singleton
class FakeTeamRepository @Inject constructor() : TeamRepository {

    private val teams = MutableStateFlow<Map<String, TeamDto>>(emptyMap())

    override fun observeTeam(teamId: String): Flow<Loaded<TeamDto?>> =
        teams.map { Loaded.Ready(it[teamId], isFromCache = false) }

    override suspend fun createTeam(uid: String, nickname: String, name: String, icon: Int, color: Int): Result<CreatedTeam> {
        val teamId = "fake-team-${teams.value.size + 1}"
        teams.update {
            it + (teamId to TeamDto(teamId, name, icon, color, memberCount = 1, distanceM = 0L, reachedAtMs = 0L))
        }
        return Result.success(CreatedTeam(teamId, inviteCode = FAKE_INVITE_CODE))
    }

    override fun observeInviteCode(teamId: String, uid: String): Flow<Loaded<String?>> =
        teams.map { Loaded.Ready(if (teamId in it) FAKE_INVITE_CODE else null, isFromCache = false) }

    override fun observeMembers(teamId: String): Flow<Loaded<List<TeamMemberDto>>> =
        teams.map { Loaded.Ready(emptyList(), isFromCache = false) }

    override suspend fun findTeamByCode(code: String): Result<TeamDto?> =
        Result.success(if (code == FAKE_INVITE_CODE) teams.value.values.firstOrNull() else null)

    override suspend fun joinTeam(uid: String, nickname: String, code: String): Result<JoinOutcome> {
        val team = findTeamByCode(code).getOrNull() ?: return Result.success(JoinOutcome.NotFound)
        joinRejection(myTeamId = null, memberCount = team.memberCount)?.let { return Result.success(JoinOutcome.Rejected(it)) }
        teams.update { it + (team.teamId to team.copy(memberCount = team.memberCount + 1)) }
        return Result.success(JoinOutcome.Joined(team.teamId))
    }

    override suspend fun leaveTeam(uid: String, teamId: String): Result<Unit> {
        teams.update { all ->
            val team = all[teamId] ?: return@update all
            if (team.memberCount <= 1) all - teamId else all + (teamId to team.copy(memberCount = team.memberCount - 1))
        }
        return Result.success(Unit)
    }

    override suspend fun kickMember(leaderUid: String, teamId: String, targetUid: String): Result<Unit> {
        teams.update { all ->
            val team = all[teamId] ?: return@update all
            all + (teamId to team.copy(memberCount = (team.memberCount - 1).coerceAtLeast(1)))
        }
        return Result.success(Unit)
    }

    companion object {
        const val FAKE_INVITE_CODE = "ABC234"
    }
}
