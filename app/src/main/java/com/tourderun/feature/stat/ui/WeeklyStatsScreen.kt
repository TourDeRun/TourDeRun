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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.tourderun.R
import com.tourderun.core.designsystem.component.CardHeader
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.component.TdrStatCard
import com.tourderun.core.designsystem.component.TdrTopBar
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format

/** S16 주간 통계 상태 (STAT). */
data class WeeklyStatsUiState(
    /** "2026-W45 · 11월 2일(월) ~ 8일(일)" — ViewModel이 만든다. */
    val periodText: String,
    val totalTeams: Int,
    val topPercent: Int,
    val teamsAhead: Int,
    val distanceM: Long,
    val lastWeekDistanceM: Long,
    val runCount: Int,
    val lastWeekRunCount: Int,
)

/** S16 주간 통계. 홈의 이번 주 카드에서 들어온다. */
@Composable
fun WeeklyStatsScreen(
    uiState: WeeklyStatsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
        TdrTopBar(title = stringResource(R.string.weekly_title), onBack = onBack)
        Text(
            uiState.periodText,
            style = type.caption,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        TdrCard(
            color = colors.highlightSoft,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.weekly_of_teams, uiState.totalTeams), style = type.label.copy(fontWeight = FontWeight.Normal), color = colors.highlightInk)
                Text(
                    stringResource(R.string.weekly_top, uiState.topPercent),
                    style = type.display.copy(fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = 0.sp),
                    color = colors.highlightInk,
                )
                Text(stringResource(R.string.weekly_teams_ahead, uiState.teamsAhead), style = type.caption, color = colors.highlightInk)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3)) {
            TdrStatCard(
                label = stringResource(R.string.weekly_team_distance),
                value = Format.km(uiState.distanceM),
                unit = stringResource(R.string.unit_km),
                caption = stringResource(R.string.weekly_vs_last_km, Format.signedKm(uiState.distanceM - uiState.lastWeekDistanceM)),
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
                value = Format.km(uiState.lastWeekDistanceM),
                fraction = uiState.lastWeekDistanceM / max,
                color = colors.brandLight,
                emphasize = false,
            )
            CompareBar(
                label = stringResource(R.string.weekly_this_week),
                value = Format.km(uiState.distanceM),
                fraction = uiState.distanceM / max,
                color = colors.brand,
                emphasize = true,
            )
        }
        Text(
            stringResource(R.string.weekly_notice),
            style = type.micro.copy(lineHeight = 16.sp),
            color = colors.inkMuted,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
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

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun WeeklyStatsScreenPreview() {
    TdrTheme {
        WeeklyStatsScreen(
            WeeklyStatsUiState("2026-W45 · 11월 2일(월) ~ 8일(일)", 38, 11, 3, 42_500, 36_400, 9, 7),
            onBack = {},
        )
    }
}
