package com.tourderun.feature.run.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tourderun.R
import com.tourderun.core.designsystem.component.StatColumn
import com.tourderun.core.designsystem.component.StatRow
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.component.TdrProgressBar
import com.tourderun.core.designsystem.component.valueWithUnit
import com.tourderun.core.designsystem.theme.DarkTdrColors
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format
import com.tourderun.core.ui.model.StageProgressUi

/** 이번 러닝으로 앞 팀을 추월했을 때. */
data class OvertakeUi(
    val teamName: String,
    val fromRank: Int,
    val toRank: Int,
    /** 러닝 전 앞 팀과의 차이(m, 양수). */
    val gapBeforeM: Long,
    /** 지금 앞선 거리(m, 양수). */
    val leadNowM: Long,
)

/** S10 러닝 결과 상태. */
data class RunResultUiState(
    /** "11월 6일 저녁 러닝" (REC-01이 정한 기본 제목). */
    val title: String,
    val distanceM: Long,
    val movingSec: Long,
    val paceSecPerKm: Long,
    val teamBeforeM: Long,
    /** 이번 러닝을 더한 뒤 스테이지 (teamDistanceM = 러닝 후 팀 거리). */
    val stageAfter: StageProgressUi,
    val overtake: OvertakeUi? = null,
)

/**
 * S10 러닝 결과. 도시에 도착했으면 이 화면 위에 스테이지 클리어(JRN-05)를 띄운다.
 */
@Composable
fun RunResultScreen(
    uiState: RunResultUiState,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val km = stringResource(R.string.unit_km)
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3, bottom = TdrSpacing.s8),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TdrSpacing.s3),
        ) {
            Column(Modifier.padding(top = TdrSpacing.s6, bottom = TdrSpacing.s2), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(uiState.title, style = type.label.copy(fontWeight = FontWeight.Normal), color = colors.inkMuted)
                Text(
                    stringResource(R.string.result_title, Format.km(uiState.distanceM, decimals = 2)),
                    style = type.titleXl,
                    modifier = Modifier.semantics { heading() },
                )
            }

            TdrCard {
                StatRow {
                    StatColumn(
                        valueWithUnit(Format.km(uiState.distanceM, decimals = 2), km),
                        stringResource(R.string.stat_distance),
                        Modifier.weight(1f),
                        valueColor = colors.brand,
                    )
                    StatColumn(AnnotatedString(Format.duration(uiState.movingSec)), stringResource(R.string.stat_time), Modifier.weight(1f))
                    StatColumn(AnnotatedString(Format.pace(uiState.paceSecPerKm)), stringResource(R.string.stat_avg_pace), Modifier.weight(1f))
                }
            }

            TeamDistanceCard(uiState)

            uiState.overtake?.let { o ->
                TdrCard(color = colors.highlightSoft) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3)) {
                        Icon(TdrIcons.TrendUp, contentDescription = null, tint = colors.highlightInk, modifier = Modifier.size(28.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                stringResource(R.string.result_overtake_title, o.teamName, o.fromRank, o.toRank),
                                style = type.subtitle.copy(lineHeight = 20.sp),
                                color = colors.highlightInk,
                            )
                            Text(
                                stringResource(R.string.result_overtake_sub, Format.km(o.gapBeforeM), Format.km(o.leadNowM)),
                                style = type.caption,
                                color = colors.highlightInk,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(TdrSpacing.s4))
        TdrButton(stringResource(R.string.action_confirm), onClick = onConfirm, modifier = Modifier.fillMaxWidth())
    }
}

/** 남색 카드: 팀 거리 전 → 후, 진행 바(흰색 = 러닝 전, 밝은 파랑 = 이번 러닝). */
@Composable
private fun TeamDistanceCard(uiState: RunResultUiState) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val stage = uiState.stageAfter
    val before = stage.progressOf(uiState.teamBeforeM)
    TdrCard(color = colors.brandStrong) {
        Text(stringResource(R.string.result_team_distance), style = type.caption, color = colors.inkOnBrandMuted)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.km_value, Format.km(uiState.teamBeforeM)),
                style = type.title.copy(lineHeight = 24.sp),
                color = colors.inkOnBrandMuted,
            )
            Icon(TdrIcons.ArrowRight, contentDescription = null, tint = colors.inkOnBrand, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.km_value, Format.km(stage.teamDistanceM)),
                style = type.statLg.copy(fontSize = 30.sp, lineHeight = 36.sp),
                color = colors.inkOnBrand,
            )
        }
        TdrProgressBar(
            progress = before,
            secondary = stage.progress,
            trackColor = Color.White.copy(alpha = 0.2f),
            fillColor = Color.White,
            secondaryColor = DarkTdrColors.brandBright,
            contentDescription = stringResource(R.string.result_stage_progress_desc, stage.stageNumber),
        )
        Text(
            buildAnnotatedString {
                append(stringResource(R.string.result_stage_line, stage.stageNumber, stage.fromCity, stage.toCity))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(stringResource(R.string.to_next_city, stage.toCity, Format.km(stage.toNextM)))
                }
            },
            style = type.label.copy(fontWeight = FontWeight.Normal),
            color = colors.inkOnBrand,
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RunResultScreenPreview() {
    TdrTheme {
        RunResultScreen(
            RunResultUiState(
                title = "11월 6일 저녁 러닝",
                distanceM = 3_240,
                movingSec = 1_122,
                paceSecPerKm = 346,
                teamBeforeM = 66_800,
                stageAfter = StageProgressUi(3, 7, "오산", "천안", 48, 85, 70_040),
                overtake = OvertakeUi("주황이들", 2, 1, 2_400, 840),
            ),
            onConfirm = {},
        )
    }
}
