package com.tourderun.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tourderun.R
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.race.RaceSummary
import com.tourderun.core.data.race.RaceTeam
import com.tourderun.core.data.team.MyTeam
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.domain.journey.locate
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.model.TeamStanding
import com.tourderun.domain.record.saveOutcome
import com.tourderun.domain.route.routeFor
import com.tourderun.feature.run.data.SavedRun
import com.tourderun.feature.run.ui.RunResultScreen
import com.tourderun.feature.run.ui.overtakeUi
import com.tourderun.feature.run.ui.runResultUiState

/**
 * S10 러닝 결과의 앞 팀 추월(RACE-02) 확인용. 디버그 빌드에만 있고 adb로 연다.
 *
 * 저장하지 않는다. 고정한 러닝 전 팀 거리·이번 거리·스테이지 목록으로 실제 변환 함수
 * (`runResultUiState`, `overtakeUi`)를 불러 [RunResultScreen]에 그린다. Firebase·Hilt를 쓰지 않는다.
 * "확인"은 아무 일도 하지 않는다.
 */
class DebugRunResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TdrTheme {
                var mode by rememberSaveable { mutableStateOf(DebugOvertakeMode.ONE_TEAM) }
                Column(Modifier.fillMaxSize()) {
                    val state = debugResultState(mode)
                    if (state != null) RunResultScreen(uiState = state, onConfirm = {}, modifier = Modifier.weight(1f))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = TdrSpacing.s5, vertical = TdrSpacing.s2),
                        horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s2),
                    ) {
                        DebugOvertakeMode.entries.forEach { m ->
                            FilterChip(
                                selected = m == mode,
                                onClick = { mode = m },
                                label = { Text(stringResource(m.labelRes)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 뛰뛰크루(66,800m) 러닝. 같은 스테이지(3)에 번개팀 84,500m, 보라돌이 70,000m, 주황이들 69,200m, 새벽러너스 60,000m(4위에서 출발). */
private enum class DebugOvertakeMode(@StringRes val labelRes: Int, val beforeM: Long, val runM: Long) {
    /** 2,800m → 69,600m: 주황이들만 넘음, 4위 → 3위 */
    ONE_TEAM(R.string.debug_overtake_one, 66_800, 2_800),

    /** 4,200m → 71,000m: 두 팀 넘음, 4위 → 2위 */
    TWO_TEAMS(R.string.debug_overtake_two, 66_800, 4_200),

    /** 1,500m → 68,300m: 못 넘음 */
    NONE(R.string.debug_overtake_none, 66_800, 1_500),

    /** 84,000m에서 2,000m → 번개팀을 넘지만 스테이지 4로 넘어가서 표시 안 함 */
    NEXT_STAGE(R.string.debug_overtake_next_stage, 84_000, 2_000),
}

private fun debugResultState(mode: DebugOvertakeMode) = run {
    val route = routeFor(1)
    val me = MyTeam(season = 1, standing = TeamStanding("me", "뛰뛰크루", mode.beforeM, reachedAtMs = 500L))
    val saved = SavedRun(
        title = "11월 23일 저녁 러닝",
        result = RunResult(
            runId = "debug-run",
            startedAtMs = 0L,
            endedAtMs = mode.runM * 360L,
            movingSec = mode.runM * 360L / 1_000,
            distanceM = mode.runM.toDouble(),
            acceptedPoints = emptyList(),
        ),
        distanceM = mode.runM,
        team = me,
        outcome = saveOutcome(mode.beforeM, mode.runM, route),
    )
    val others = listOf(
        TeamStanding("o", "주황이들", 69_200, 100L),
        TeamStanding("p", "보라돌이", 70_000, 100L),
        TeamStanding("d", "새벽러너스", 60_000, 100L),
        TeamStanding("b", "번개팀", 84_500, 100L),
    )
    val stageIndex = locate(mode.beforeM, route).stageIndex
    val stageTeams = (others + me.standing).filter { locate(it.distanceM, route).stageIndex == stageIndex }
    val summary = RaceSummary(
        stageIndex = stageIndex,
        stageTeams = stageTeams.map { RaceTeam(it, icon = 0, color = 0, memberCount = 2) },
        ranking = null,
        aheadOtherStage = null,
        behindOtherStage = null,
    )
    runResultUiState(saved)?.copy(overtake = overtakeUi(saved, Loaded.Ready(summary, isFromCache = false)))
}
