package com.tourderun.feature.journey.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.race.MyRace
import com.tourderun.core.data.race.RaceRepository
import com.tourderun.core.data.race.observeMyRace
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.core.ui.model.MyRaceUi
import com.tourderun.core.ui.model.myRaceUi
import com.tourderun.domain.route.routeFor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** S08이 지금 무엇을 보여 줄지. */
sealed interface RoadViewLoad {
    /** 받기 전이거나 팀이 없다(지도 바탕색만). */
    data object Loading : RoadViewLoad

    /** 경쟁 상황 리스너가 첫 값 전에 실패했다. */
    data class Failed(val error: DataError) : RoadViewLoad

    data class Content(val ui: RoadViewUiState) : RoadViewLoad
}

/**
 * 리스너 값 → S08 상태. 팀이 없으면 [RoadViewLoad.Loading], 첫 값 전 실패면 [RoadViewLoad.Failed],
 * 스테이지 전환 중(순위 없음)이면 직전 내용을 그대로 둔다(직전이 없으면 Loading).
 */
internal fun nextRoadViewLoad(previous: RoadViewLoad, myRace: MyRace?): RoadViewLoad {
    if (myRace == null) return RoadViewLoad.Loading
    val race = myRace.race
    if (race is Loaded.Failed) return RoadViewLoad.Failed(race.error)
    return myRaceUi(myRace)?.let { RoadViewLoad.Content(roadViewUiState(it, myRace.team.season)) }
        ?: previous as? RoadViewLoad.Content
        ?: RoadViewLoad.Loading
}

/**
 * S08 로드뷰 (JRN-04). 홈과 같은 경쟁 상황으로 지도에 그릴 값을 만든다.
 * 불러오지 못하면 [RoadViewLoad.Failed], [retry]로 처음부터 다시 구독한다(JRN-03).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RoadViewViewModel @Inject constructor(
    raceRepository: RaceRepository,
    myTeamSource: MyTeamSource,
) : ViewModel() {

    /** 다시 시도할 때마다 1씩 늘린다. 값이 바뀌면 리스너를 새로 붙인다. */
    private val attempt = MutableStateFlow(0)

    val uiState: StateFlow<RoadViewLoad> = attempt
        .flatMapLatest {
            observeMyRace(myTeamSource, raceRepository).scan<_, RoadViewLoad>(RoadViewLoad.Loading, ::nextRoadViewLoad)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoadViewLoad.Loading)

    /** 불러오기 실패 후 다시 시도. */
    fun retry() {
        attempt.update { it + 1 }
    }
}

/** 경쟁 상황 → S08 상태. 다른 스테이지의 앞·뒤 팀도 지도에 올린다. */
fun roadViewUiState(race: MyRaceUi, season: Int): RoadViewUiState = RoadViewUiState(
    team = race.team,
    stage = race.stage,
    rank = race.rank,
    totalTeams = race.totalTeams,
    map = roadMapUi(routeFor(season), race.team.distanceM, race.rivalsOnMap),
)
