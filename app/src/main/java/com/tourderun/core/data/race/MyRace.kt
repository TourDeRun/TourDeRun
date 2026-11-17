package com.tourderun.core.data.race

import com.tourderun.core.data.Loaded
import com.tourderun.core.data.team.MyTeam
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.domain.journey.locate
import com.tourderun.domain.route.routeFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** 내 팀과 그 팀이 있는 스테이지의 경쟁 상황. [race]가 [Loaded.Failed]면 첫 값을 받기 전에 리스너가 실패한 것이다. */
data class MyRace(val team: MyTeam, val race: Loaded<RaceSummary>)

/**
 * 내 팀의 경쟁 상황을 계속 받는다 (S07 홈, S08 로드뷰, S15 스테이지 순위). 팀이 없으면 null.
 *
 * [RaceRepository] 규칙대로 내 팀의 시즌·팀 ID·스테이지가 바뀔 때만 리스너를 다시 연결한다.
 * 그래서 [MyRace.team]의 거리는 연결할 때 값이고, 그 뒤 거리는 [MyRace.race]의 목록에서 읽어야 한다.
 * 실패([Loaded.Failed]) 뒤 다시 시도하려면 이 Flow를 다시 구독한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun observeMyRace(myTeamSource: MyTeamSource, raceRepository: RaceRepository): Flow<MyRace?> =
    myTeamSource.observeMyTeam()
        .distinctUntilChangedBy { team ->
            team?.let {
                Triple(it.season, it.standing.teamId, locate(it.standing.distanceM, routeFor(it.season)).stageIndex)
            }
        }
        .flatMapLatest { team ->
            if (team == null) {
                flowOf(null)
            } else {
                raceRepository.observeRace(team.season, team.standing).map { MyRace(team, it) }
            }
        }
