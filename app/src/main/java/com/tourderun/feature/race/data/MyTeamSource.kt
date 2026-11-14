package com.tourderun.feature.race.data

import com.tourderun.domain.model.TeamStanding
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** S15 순위의 기준이 되는 내 팀. [standing]은 `RaceRepository.observeRace`에 그대로 넘긴다. */
data class MyRaceTeam(
    val season: Int,
    val standing: TeamStanding,
)

/**
 * 내 팀 정보 (S15).
 *
 * TODO(RACE-03): `TeamRepository`(권민찬)가 생기면 그 값으로 바꾸고 이 인터페이스와 [FixedMyTeamSource]는 지운다.
 */
interface MyTeamSource {
    /** 내 팀. 팀이 없으면 null. */
    fun observeMyTeam(): Flow<MyRaceTeam?>
}

/**
 * 고정값 내 팀(PR #2 Preview의 "뛰뛰크루", 시즌 1, 66.8km). `FakeRaceRepository`가 목록의 우리 팀 자리를
 * 이 팀의 ID·이름으로 바꿔서 낸다.
 */
class FixedMyTeamSource @Inject constructor() : MyTeamSource {
    override fun observeMyTeam(): Flow<MyRaceTeam?> = flowOf(FIXED_TEAM)

    private companion object {
        val FIXED_TEAM = MyRaceTeam(
            season = 1,
            standing = TeamStanding(teamId = "fake-my-team", name = "뛰뛰크루", distanceM = 66_800, reachedAtMs = 0L),
        )
    }
}
