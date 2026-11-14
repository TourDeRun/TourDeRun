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
import com.tourderun.core.data.SystemClock
import com.tourderun.core.data.stats.FakeStatsRepository
import com.tourderun.core.data.team.FixedMyTeamSource
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.feature.stat.ui.WeeklyStatsScreen
import com.tourderun.feature.stat.ui.WeeklyStatsViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * S16 주간 통계 확인용(STAT-01·02). 디버그 빌드에만 있고, 내비게이션(AppNavHost)이 생기기 전까지 adb로 연다.
 *
 * 아래 칩으로 가짜 통계를 고른다(기본 = 42.5km·상위 11%, 이번 주 0km, 오프라인 = 기본 + 캐시 출처).
 * Hilt 바인딩은 바꾸지 않고, 고른 상황마다 [FakeStatsRepository]로 ViewModel을 따로 만든다. Firebase를 쓰지 않는다.
 */
@AndroidEntryPoint
class DebugWeeklyStatsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TdrTheme {
                var mode by rememberSaveable { mutableStateOf(DebugWeeklyMode.DEFAULT) }
                Column(Modifier.fillMaxSize()) {
                    // 상황마다 key가 달라서 ViewModel도 따로 만들어진다.
                    val viewModel = viewModel(key = mode.name) {
                        WeeklyStatsViewModel(mode.repository(), FixedMyTeamSource(), SystemClock())
                    }
                    WeeklyStatsScreen(onBack = ::finish, modifier = Modifier.weight(1f), viewModel = viewModel)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = TdrSpacing.s5, vertical = TdrSpacing.s2),
                        horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s2),
                    ) {
                        DebugWeeklyMode.entries.forEach { m ->
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

private enum class DebugWeeklyMode(@StringRes val labelRes: Int) {
    DEFAULT(R.string.debug_mode_default),
    ZERO(R.string.debug_weekly_zero),
    OFFLINE(R.string.debug_mode_offline),
    ;

    fun repository(): FakeStatsRepository = when (this) {
        DEFAULT -> FakeStatsRepository(FakeStatsRepository.DEFAULT_WEEKLY)
        // 지난주 기록은 있고 이번 주만 0km
        ZERO -> FakeStatsRepository(
            FakeStatsRepository.DEFAULT_WEEKLY.copy(thisWeekM = 0, thisWeekRuns = 0, memberM = emptyMap<String, Long>()),
        )
        OFFLINE -> FakeStatsRepository(FakeStatsRepository.DEFAULT_WEEKLY, topPercent = null, fromCache = true)
    }
}
