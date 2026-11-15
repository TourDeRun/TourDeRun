package com.tourderun.feature.race.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.Cached
import com.tourderun.core.data.race.RaceRepository
import com.tourderun.core.data.race.RaceSummary
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.domain.journey.locate
import com.tourderun.domain.model.Route
import com.tourderun.domain.route.routeFor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

/**
 * S15 스테이지 순위 (RACE-03).
 *
 * 내 팀의 시즌·팀 ID·스테이지가 바뀔 때만 `observeRace`를 다시 부른다(같은 스테이지 안 거리 변화는 리스너가 알려 준다).
 * 상태는 [kotlinx.coroutines.flow.scan]으로 이어 가서, 스테이지가 바뀌는 순간에도 직전 순위를 유지한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StageRankViewModel @Inject constructor(
    raceRepository: RaceRepository,
    myTeamSource: MyTeamSource,
) : ViewModel() {

    val uiState: StateFlow<StageRankUiState> = myTeamSource.observeMyTeam()
        .distinctUntilChangedBy { team ->
            team?.let { RaceKey(it.season, it.standing.teamId, locate(it.standing.distanceM, routeFor(it.season)).stageIndex) }
        }
        .flatMapLatest { team ->
            if (team == null) {
                flowOf(null)
            } else {
                val route = routeFor(team.season)
                raceRepository.observeRace(team.season, team.standing)
                    .map { RaceUpdate(it, team.standing.teamId, route) }
            }
        }
        .scan<RaceUpdate?, StageRankUiState>(StageRankUiState.Loading) { previous, update ->
            if (update == null) {
                StageRankUiState.NoTeam
            } else {
                nextStageRankState(previous, update.race, update.myTeamId, update.route)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StageRankUiState.Loading)

    private data class RaceKey(val season: Int, val teamId: String, val stageIndex: Int)

    private class RaceUpdate(val race: Cached<RaceSummary>, val myTeamId: String, val route: Route)
}
