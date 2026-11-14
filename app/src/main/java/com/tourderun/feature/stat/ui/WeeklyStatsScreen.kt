package com.tourderun.feature.stat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tourderun.R
import com.tourderun.core.designsystem.component.CardHeader
import com.tourderun.core.designsystem.component.Pill
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.component.TdrStatCard
import com.tourderun.core.designsystem.component.TdrTopBar
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format

/** S16 주간 통계. 홈의 이번 주 카드에서 들어온다. */
@Composable
fun WeeklyStatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeeklyStatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WeeklyStatsContent(uiState, modifier = modifier) { action ->
        when (action) {
            WeeklyStatsAction.Back -> onBack()
        }
    }
}

/** S16 화면 내용. 상태 없음(Preview 대상). */
@Composable
fun WeeklyStatsContent(
    uiState: WeeklyStatsUiState,
    modifier: Modifier = Modifier,
    onAction: (WeeklyStatsAction) -> Unit,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3, bottom = TdrSpacing.s8),
        verticalArrangement = Arrangement.spacedBy(TdrSpacing.s3),
    ) {
        TdrTopBar(title = stringResource(R.string.weekly_title), onBack = { onAction(WeeklyStatsAction.Back) }) {
            if (uiState is WeeklyStatsUiState.Content && uiState.isOffline) {
                Pill(
                    stringResource(R.string.weekly_offline),
                    background = colors.inkMuted,
                    contentColor = colors.inkOnBrand,
                )
            }
        }
        when (uiState) {
            WeeklyStatsUiState.Loading -> Box(
                Modifier.fillMaxWidth().padding(vertical = TdrSpacing.s8),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = colors.brand)
            }

            WeeklyStatsUiState.NoTeam -> Text(
                stringResource(R.string.weekly_no_team),
                style = type.body,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = TdrSpacing.s8),
            )

            is WeeklyStatsUiState.Content -> WeeklyBody(uiState)
        }
    }
}

@Composable
private fun WeeklyBody(uiState: WeeklyStatsUiState.Content) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val p = uiState.period
    Text(
        if (p.startMonth == p.endMonth) {
            stringResource(R.string.weekly_period_same_month, p.weekId, p.startMonth, p.startDay, p.endDay)
        } else {
            stringResource(R.string.weekly_period, p.weekId, p.startMonth, p.startDay, p.endMonth, p.endDay)
        },
        style = type.caption,
        color = colors.inkMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    RankCard(uiState.rank)

    Row(horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3)) {
        TdrStatCard(
            label = stringResource(R.string.weekly_team_distance),
            value = uiState.distanceKm,
            unit = stringResource(R.string.unit_km),
            caption = when (val change = uiState.change) {
                is WeeklyChangeUi.Diff -> stringResource(R.string.weekly_vs_last_km, Format.signedKm(change.diffM))
                WeeklyChangeUi.Same -> stringResource(R.string.weekly_vs_last_same)
                WeeklyChangeUi.NoLastWeek -> stringResource(R.string.weekly_no_last_week)
            },
            modifier = Modifier.weight(1f),
        )
        TdrStatCard(
            label = stringResource(R.string.weekly_run_count),
            value = uiState.runCount.toString(),
            unit = stringResource(R.string.unit_times_spaced).trim(),
            caption = stringResource(R.string.weekly_last_runs, uiState.lastWeekRunCount),
            modifier = Modifier.weight(1f),
        )
    }

    TdrCard {
        CardHeader(stringResource(R.string.weekly_compare), trailing = stringResource(R.string.unit_km))
        val max = maxOf(uiState.distanceM, uiState.lastWeekDistanceM, 1L).toFloat()
        CompareBar(
            label = stringResource(R.string.weekly_last_week),
            value = uiState.lastWeekDistanceKm,
            fraction = uiState.lastWeekDistanceM / max,
            color = colors.brandLight,
            emphasize = false,
        )
        CompareBar(
            label = stringResource(R.string.weekly_this_week),
            value = uiState.distanceKm,
            fraction = uiState.distanceM / max,
            color = colors.brand,
            emphasize = true,
        )
    }
    Text(
        stringResource(R.string.weekly_notice),
        style = type.micro.copy(lineHeight = 16.sp),
        color = colors.inkMuted,
        modifier = Modifier.padding(horizontal = TdrSpacing.s1),
    )
}

/** 상위 % 카드. 숨김이면 그리지 않는다. */
@Composable
private fun RankCard(rank: WeeklyRankUi) {
    if (rank == WeeklyRankUi.Hidden) return
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    TdrCard(
        color = colors.highlightSoft,
        contentPadding = PaddingValues(horizontal = TdrSpacing.s4, vertical = TdrSpacing.s5),
        verticalArrangement = Arrangement.spacedBy(TdrSpacing.s1),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TdrSpacing.s1),
        ) {
            when (rank) {
                is WeeklyRankUi.Shown -> {
                    Text(
                        stringResource(R.string.weekly_of_teams, rank.totalTeams),
                        style = type.label.copy(fontWeight = FontWeight.Normal),
                        color = colors.highlightInk,
                    )
                    Text(
                        stringResource(R.string.weekly_top, rank.percent),
                        style = type.display.copy(fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = 0.sp),
                        color = colors.highlightInk,
                    )
                    Text(stringResource(R.string.weekly_teams_ahead, rank.moreCount), style = type.caption, color = colors.highlightInk)
                }

                WeeklyRankUi.FirstRun -> Text(
                    stringResource(R.string.weekly_first_run),
                    style = type.title,
                    color = colors.highlightInk,
                    textAlign = TextAlign.Center,
                )

                WeeklyRankUi.Hidden -> Unit
            }
        }
    }
}

@Composable
private fun CompareBar(label: String, value: String, fraction: Float, color: Color, emphasize: Boolean) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val shape = RoundedCornerShape(6.dp)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            label,
            style = type.caption.copy(fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal),
            color = if (emphasize) colors.ink else colors.inkMuted,
            modifier = Modifier.width(56.dp),
        )
        Box(Modifier.weight(1f).height(18.dp).clip(shape).background(colors.bgSubtle)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(shape).background(color))
        }
        Text(
            value,
            style = type.caption.copy(fontWeight = if (emphasize) FontWeight.ExtraBold else FontWeight.Bold),
            color = if (emphasize) colors.brand else colors.ink,
            textAlign = TextAlign.End,
            modifier = Modifier.width(44.dp),
        )
    }
}

private val PreviewPeriod = WeekPeriodUi("2026-W46", 11, 9, 11, 15)

private fun previewContent(
    distanceM: Long = 42_500,
    runs: Long = 9,
    rank: WeeklyRankUi = WeeklyRankUi.Shown(percent = 11, totalTeams = 38, moreCount = 3),
    isOffline: Boolean = false,
) = WeeklyStatsUiState.Content(
    period = PreviewPeriod,
    distanceM = distanceM,
    runCount = runs,
    lastWeekDistanceM = 36_400,
    lastWeekRunCount = 7,
    change = WeeklyChangeUi.Diff(distanceM - 36_400),
    rank = rank,
    isOffline = isOffline,
)

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun WeeklyStatsContentPreview() {
    TdrTheme { WeeklyStatsContent(previewContent()) {} }
}

@Preview(name = "이번 주 0km", widthDp = 390, heightDp = 844)
@Composable
private fun WeeklyStatsContentZeroPreview() {
    TdrTheme { WeeklyStatsContent(previewContent(distanceM = 0, runs = 0, rank = WeeklyRankUi.FirstRun)) {} }
}

@Preview(name = "오프라인", widthDp = 390, heightDp = 844)
@Composable
private fun WeeklyStatsContentOfflinePreview() {
    TdrTheme { WeeklyStatsContent(previewContent(rank = WeeklyRankUi.Hidden, isOffline = true)) {} }
}
