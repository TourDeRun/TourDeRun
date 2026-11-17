package com.tourderun.feature.race.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.race.MyRace
import com.tourderun.core.data.race.RaceRepository
import com.tourderun.core.data.race.observeMyRace
import com.tourderun.core.data.team.MyTeamSource
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

/**
 * S15 스테이지 순위 (RACE-03).
 *
 * 경쟁 상황은 홈·로드뷰와 같은 [observeMyRace]로 받는다(내 팀의 시즌·팀 ID·스테이지가 바뀔 때만 다시 연결).
 * 상태는 [kotlinx.coroutines.flow.scan]으로 이어 가서, 스테이지가 바뀌는 순간에도 직전 순위를 유지한다.
 * 불러오기에 실패하면 [StageRankUiState.Failed]를 보이고, [retry]로 처음부터 다시 구독한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StageRankViewModel @Inject constructor(
    raceRepository: RaceRepository,
    myTeamSource: MyTeamSource,
) : ViewModel() {

    /** 다시 시도할 때마다 1씩 늘린다. 값이 바뀌면 리스너를 새로 붙인다. */
    private val attempt = MutableStateFlow(0)

    val uiState: StateFlow<StageRankUiState> = attempt
        .flatMapLatest {
            observeMyRace(myTeamSource, raceRepository)
                .scan<MyRace?, StageRankUiState>(StageRankUiState.Loading) { previous, myRace ->
                    if (myRace == null) {
                        StageRankUiState.NoTeam
                    } else {
                        nextStageRankState(previous, myRace.race, myRace.team.standing.teamId, routeFor(myRace.team.season))
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StageRankUiState.Loading)

    /** 불러오기 실패 후 다시 시도. */
    fun retry() {
        attempt.update { it + 1 }
    }
}
