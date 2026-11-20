package com.tourderun.core.data.team

import com.tourderun.core.data.Loaded
import com.tourderun.core.data.firestore.TeamDto
import javax.inject.Inject
import javax.inject.Singleton
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

    companion object {
        const val FAKE_INVITE_CODE = "ABC234"
    }
}
