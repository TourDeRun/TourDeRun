package com.tourderun.core.data.team

import com.tourderun.domain.model.TeamStanding
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 내 팀. S15 순위·S16 주간 통계의 기준 팀이고, 러닝 저장(REC-01)의 팀 ID·시즌·저장 전 팀 거리로도 쓴다.
 *
 * @property standing 팀 ID·이름·**기기에 캐시된 팀 거리**(`distanceM`)·도달 시각. `RaceRepository.observeRace`에 그대로 넘긴다
 */
data class MyTeam(
    val season: Int,
    val standing: TeamStanding,
)

/**
 * 내 팀 정보 (S15, S16, REC-01). 앱에서는 [SessionMyTeamSource](세션 teamId + `teams` 문서)가 바인딩된다.
 */
interface MyTeamSource {
    /** 내 팀. 팀이 없으면 null. */
    fun observeMyTeam(): Flow<MyTeam?>
}

/**
 * 고정값 내 팀(PR #2 Preview의 "뛰뛰크루", 시즌 1, 66.8km). **디버그 화면·테스트 전용**이고 앱에는 바인딩하지 않는다.
 * `FakeRaceRepository`가 목록의 우리 팀 자리를 이 팀의 ID·이름으로 바꿔서 낸다.
 * `fake-my-team` 문서는 Firestore에 없으므로 이 값으로 러닝을 저장하면 배치가 거부된다.
 */
class FixedMyTeamSource @Inject constructor() : MyTeamSource {
    override fun observeMyTeam(): Flow<MyTeam?> = flowOf(FIXED_TEAM)

    private companion object {
        val FIXED_TEAM = MyTeam(
            season = 1,
            standing = TeamStanding(teamId = "fake-my-team", name = "뛰뛰크루", distanceM = 66_800, reachedAtMs = 0L),
        )
    }
}
