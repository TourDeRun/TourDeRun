package com.tourderun.feature.stat.data

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** S16 통계의 기준이 되는 내 팀. */
data class MyStatsTeam(
    val season: Int,
    val teamId: String,
)

/**
 * 내 팀 정보 (S16). S15의 `feature/race/data/MyTeamSource`(PR #36)와 같은 형태다.
 *
 * TODO(STAT-01): #36이 머지되면 하나로 합치고, `TeamRepository`(권민찬)가 생기면 그 값으로 바꾼 뒤
 * 이 인터페이스와 [FixedMyTeamSource]는 지운다.
 */
interface MyTeamSource {
    /** 내 팀. 팀이 없으면 null. */
    fun observeMyTeam(): Flow<MyStatsTeam?>
}

/** 고정값 내 팀(S15 고정값과 같은 팀 ID, 시즌 1). */
class FixedMyTeamSource @Inject constructor() : MyTeamSource {
    override fun observeMyTeam(): Flow<MyStatsTeam?> = flowOf(MyStatsTeam(season = 1, teamId = "fake-my-team"))
}
