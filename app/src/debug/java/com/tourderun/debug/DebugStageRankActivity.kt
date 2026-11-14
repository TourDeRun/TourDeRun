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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tourderun.R
import com.tourderun.core.data.race.FakeRaceRepository
import com.tourderun.core.data.race.RaceScenario
import com.tourderun.core.data.team.FixedMyTeamSource
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.feature.race.ui.StageRankScreen
import com.tourderun.feature.race.ui.StageRankViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * S15 스테이지 순위 확인용(RACE-03). 디버그 빌드에만 있고, 내비게이션(AppNavHost)이 생기기 전까지 adb로 연다.
 *
 * 아래 버튼으로 가짜 경쟁 상황을 고른다(기본 = 5팀 중 2위, 혼자, 오프라인 = 기본 + 캐시 출처).
 * Hilt 바인딩은 바꾸지 않고, 고른 상황마다 [FakeRaceRepository]로 ViewModel을 따로 만든다. Firebase를 쓰지 않는다.
 */
@AndroidEntryPoint
class DebugStageRankActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TdrTheme {
                var mode by rememberSaveable { mutableStateOf(DebugRaceMode.DEFAULT) }
                Column(Modifier.fillMaxSize()) {
                    // 상황마다 key가 달라서 ViewModel도 따로 만들어진다.
                    val viewModel = viewModel(key = mode.name) {
                        StageRankViewModel(FakeRaceRepository(mode.scenario, mode.fromCache), FixedMyTeamSource())
                    }
                    StageRankScreen(onBack = ::finish, modifier = Modifier.weight(1f), viewModel = viewModel)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = TdrSpacing.s5, vertical = TdrSpacing.s2),
                        horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s2),
                    ) {
                        DebugRaceMode.entries.forEach { m ->
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

private enum class DebugRaceMode(val scenario: RaceScenario, val fromCache: Boolean, @StringRes val labelRes: Int) {
    DEFAULT(RaceScenario.MIDDLE, fromCache = false, R.string.debug_mode_default),
    ALONE(RaceScenario.ALONE, fromCache = false, R.string.debug_stage_rank_alone),
    OFFLINE(RaceScenario.MIDDLE, fromCache = true, R.string.debug_mode_offline),
}
