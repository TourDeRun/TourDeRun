package com.tourderun.core.data.team

import com.tourderun.core.data.Loaded
import com.tourderun.core.data.firestore.TeamDto
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.teamId
import com.tourderun.domain.model.TeamStanding
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

/**
 * 진짜 내 팀 (TEAM-01). 세션의 `teamId`로 `teams/{teamId}`를 실시간으로 읽는다.
 * 홈·로드뷰·S15 순위·S16 주간 통계·러닝 저장(REC-01)이 이 값을 본다.
 *
 * - 팀이 없거나(NoTeam·로그아웃) 팀 문서가 없으면 null
 * - 읽기 실패(`Loaded.Failed`)는 내보내지 않는다. 마지막 값이 유지되고, 리스너가 다시 붙으면 이어서 온다
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionMyTeamSource @Inject constructor(
    private val session: SessionRepository,
    private val teamRepository: TeamRepository,
) : MyTeamSource {

    override fun observeMyTeam(): Flow<MyTeam?> = session.state
        .map { it.teamId }
        .distinctUntilChanged()
        .flatMapLatest<String?, MyTeam?> { teamId ->
            if (teamId == null) {
                flowOf(null)
            } else {
                teamRepository.observeTeam(teamId).transform { loaded ->
                    when (loaded) {
                        is Loaded.Ready -> emit(loaded.value?.toMyTeam())
                        is Loaded.Failed -> Unit
                    }
                }
            }
        }
        .distinctUntilChanged()
}

/** 팀 문서 → 내 팀. 시즌은 지금 1뿐이라 [NEW_TEAM_SEASON]을 쓴다(`TeamDto`에 시즌이 생기면 그 값으로). */
internal fun TeamDto.toMyTeam(): MyTeam = MyTeam(
    season = NEW_TEAM_SEASON,
    standing = TeamStanding(teamId = teamId, name = name, distanceM = distanceM, reachedAtMs = reachedAtMs),
)
