package com.tourderun.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tourderun.R
import com.tourderun.core.data.card.FakeCardRepository
import com.tourderun.core.data.card.OwnedCard
import com.tourderun.core.data.journey.ArrivalScenario
import com.tourderun.core.data.journey.FakeArrivalRepository
import com.tourderun.core.data.session.FakeSessionRepository
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.feature.journey.ui.StageClearHost
import com.tourderun.feature.journey.ui.StageClearViewModel
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint

/**
 * JRN-05 스테이지 클리어 연출과 EXT-01 지역 카드 확인용. 디버그 빌드에만 있고 adb로 연다.
 *
 * 아래 칩으로 가짜 도착을 고른다(안양 도착 / 오산·천안 두 도시 / 부산 완주 / 안양 카드를 이미 가짐).
 * 앞의 셋은 가진 카드가 없어 연출에 카드가 보인다(두 도시는 마지막 도시 천안). "이미 가진 카드"는 카드 없이 뜬다.
 * "확인"(또는 뒤로 가기)·"카드 보기"를 누르면 연출이 닫힌다(카드 보기는 이 화면에서는 이동하지 않는다).
 * 칩을 다시 누르면 처음부터. 고를 때마다 [FakeArrivalRepository]·[FakeCardRepository]·[FakeSessionRepository]로
 * ViewModel을 따로 만든다.
 * Hilt 바인딩은 바꾸지 않고 Firebase를 쓰지 않는다.
 */
@AndroidEntryPoint
class DebugStageClearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TdrTheme {
                var mode by rememberSaveable { mutableStateOf(DebugClearMode.ONE_CITY) }
                var round by rememberSaveable { mutableIntStateOf(0) }
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.debug_clear_closed))
                        // 상황·횟수마다 key가 달라서 ViewModel도 새로 만들어진다.
                        val viewModel = viewModel(key = "${mode.name}-$round") {
                            val arrivals = FakeArrivalRepository(mode.scenario)
                            val cards = FakeCardRepository(mode.ownedCityIds.map { OwnedCard(it, 0L, "뛰뛰크루") })
                            StageClearViewModel(FakeSessionRepository(), Lazy { arrivals }, Lazy { cards })
                        }
                        StageClearHost(viewModel = viewModel)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = TdrSpacing.s5, vertical = TdrSpacing.s2),
                        horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s2),
                    ) {
                        DebugClearMode.entries.forEach { m ->
                            FilterChip(
                                selected = m == mode,
                                onClick = {
                                    mode = m
                                    round++
                                },
                                label = { Text(stringResource(m.labelRes)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class DebugClearMode(
    @StringRes val labelRes: Int,
    val scenario: ArrivalScenario,
    val ownedCityIds: List<String> = emptyList(),
) {
    ONE_CITY(R.string.debug_clear_one_city, ArrivalScenario.ONE_CITY),
    TWO_CITIES(R.string.debug_clear_two_cities, ArrivalScenario.TWO_CITIES),
    FINISH(R.string.debug_clear_finish, ArrivalScenario.FINISH),

    /** 안양 도착인데 안양 카드를 이미 가짐(팀을 옮겨 다시 도착) → 카드 없이 연출 */
    OWNED(R.string.debug_clear_owned, ArrivalScenario.ONE_CITY, ownedCityIds = listOf("seoul", "anyang")),
}
