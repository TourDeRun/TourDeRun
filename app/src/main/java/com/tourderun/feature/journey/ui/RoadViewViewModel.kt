package com.tourderun.feature.journey.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.race.RaceRepository
import com.tourderun.core.data.race.observeMyRace
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.core.ui.model.MyRaceUi
import com.tourderun.core.ui.model.myRaceUi
import com.tourderun.domain.route.routeFor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

/**
 * S08 로드뷰 (JRN-04). 홈과 같은 경쟁 상황으로 지도에 그릴 값을 만든다.
 * 상태가 null이면 아직 못 받았거나 팀이 없는 것이다. 스테이지가 바뀌는 순간에는 직전 화면을 그대로 둔다.
 */
@HiltViewModel
class RoadViewViewModel @Inject constructor(
    raceRepository: RaceRepository,
    myTeamSource: MyTeamSource,
) : ViewModel() {

    val uiState: StateFlow<RoadViewUiState?> = observeMyRace(myTeamSource, raceRepository)
        .scan<_, RoadViewUiState?>(null) { previous, myRace ->
            when {
                myRace == null -> null
                else -> myRaceUi(myRace)?.let { roadViewUiState(it, myRace.team.season) } ?: previous
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** 경쟁 상황 → S08 상태. 다른 스테이지의 앞·뒤 팀도 지도에 올린다. */
fun roadViewUiState(race: MyRaceUi, season: Int): RoadViewUiState = RoadViewUiState(
    team = race.team,
    stage = race.stage,
    rank = race.rank,
    totalTeams = race.totalTeams,
    map = roadMapUi(routeFor(season), race.team.distanceM, race.rivalsOnMap),
)
