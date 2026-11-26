package com.tourderun.feature.run.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tourderun.R
import com.tourderun.core.designsystem.component.Pill
import com.tourderun.core.designsystem.component.SampleRoutePoints
import com.tourderun.core.designsystem.component.StatColumn
import com.tourderun.core.designsystem.component.StatRow
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.component.TdrProgressBar
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSize
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format
import com.tourderun.core.ui.model.StageProgressUi
import com.tourderun.core.ui.model.stageProgressUi
import com.tourderun.domain.model.LatLngValue
import com.tourderun.domain.route.routeFor
import kotlin.math.roundToInt

/** S09 러닝 중 화면 상태. 거리는 측정 중이라 m 단위 Double. */
data class RunningUiState(
    val distanceM: Double,
    val movingSec: Long,
    val avgPaceSecPerKm: Long,
    val currentPaceSecPerKm: Long,
    val gpsGood: Boolean = true,
    val isPaused: Boolean = false,
    /** 이번 러닝 거리를 더한 팀 위치. */
    val stageWithRun: StageProgressUi,
    /** 미니 지도(P1). 비어 있으면 칸을 숨긴다. */
    val path: List<LatLngValue> = emptyList(),
)

/**
 * S09 러닝. 이 화면만 다크 테마다 (안에서 [TdrTheme] darkTheme = true를 건다).
 * 종료는 오조작을 막으려 일시정지한 뒤에만 누를 수 있다.
 */
@Composable
fun RunningScreen(
    uiState: RunningUiState,
    onPauseToggle: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TdrTheme(darkTheme = true) {
        RunningContent(uiState, onPauseToggle, onStop, modifier)
    }
}

@Composable
private fun RunningContent(
    uiState: RunningUiState,
    onPauseToggle: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val stage = uiState.stageWithRun
    val runM = uiState.distanceM.roundToInt().toLong()
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(TdrSpacing.s4),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (uiState.isPaused) colors.inkMuted else colors.live, CircleShape),
            )
            Text(
                stringResource(if (uiState.isPaused) R.string.run_status_paused else R.string.run_status),
                style = type.label.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = TdrSpacing.s15).weight(1f),
            )
            Pill(
                stringResource(if (uiState.gpsGood) R.string.run_gps_ok else R.string.run_gps_weak),
                background = colors.bgSurface,
                contentColor = if (uiState.gpsGood) colors.inkMuted else colors.highlightInk,
                style = type.micro.copy(lineHeight = TdrTheme.typography.caption.lineHeight),
            )
        }

        Column(
            Modifier.fillMaxWidth().padding(top = TdrSpacing.s3, bottom = TdrSpacing.s1),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(Format.km(runM, decimals = 2), style = type.runHero, color = colors.brandBright)
            Text(stringResource(R.string.run_km_label), style = type.label.copy(fontWeight = FontWeight.Normal), color = colors.inkMuted)
        }

        StatRow {
            val big = type.statMd
            StatColumn(AnnotatedString(Format.duration(uiState.movingSec)), stringResource(R.string.stat_time), Modifier.weight(1f), big)
            StatColumn(AnnotatedString(Format.pace(uiState.avgPaceSecPerKm)), stringResource(R.string.stat_avg_pace), Modifier.weight(1f), big)
            StatColumn(AnnotatedString(Format.pace(uiState.currentPaceSecPerKm)), stringResource(R.string.run_current_pace), Modifier.weight(1f), big)
        }

        TdrCard(verticalArrangement = Arrangement.spacedBy(TdrSpacing.s25)) {
            Row {
                Text(stringResource(R.string.run_team_adding), style = type.caption, color = colors.inkMuted, modifier = Modifier.weight(1f))
                Text(
                    Format.signedKm(runM, decimals = 2) + stringResource(R.string.unit_km),
                    style = type.captionBold,
                    color = colors.brandBright,
                )
            }
            TdrProgressBar(
                progress = stage.progress,
                trackColor = colors.border,
                fillColor = colors.brandBright,
                contentDescription = stringResource(R.string.stage_progress_desc),
            )
            Text(
                if (stage.finished) {
                    stringResource(R.string.stage_finished, stage.toCity)
                } else {
                    stringResource(R.string.run_to_next, stage.toCity, Format.kmUp(stage.toNextM, decimals = 2))
                },
                style = type.subtitle,
            )
            Text(
                stringResource(
                    R.string.run_team_info,
                    Format.teamKm(stage.teamDistanceM),
                    stage.stageNumber,
                    stage.fromCity,
                    stage.toCity,
                    (stage.progress * 100).roundToInt(),
                ),
                style = type.caption,
                color = colors.inkMuted,
            )
        }

        if (uiState.path.isNotEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().clip(TdrShapes.lg)) {
                RunMiniMap(path = uiState.path, modifier = Modifier.fillMaxSize())
                Pill(
                    stringResource(R.string.run_minimap),
                    background = colors.bgSubtle,
                    contentColor = colors.inkMuted,
                    style = type.micro,
                    modifier = Modifier.padding(start = TdrSpacing.s3, top = TdrSpacing.s25),
                )
            }
        } else {
            Box(Modifier.weight(1f))
        }

        RunControls(uiState.isPaused, onPauseToggle, onStop)
    }
}

@Composable
private fun RunControls(isPaused: Boolean, onPauseToggle: () -> Unit, onStop: () -> Unit) {
    val colors = TdrTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(top = TdrSpacing.s1),
        horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s4, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 종료는 일시정지한 뒤에만 누를 수 있다(일시정지가 오조작 방지 역할을 한다).
        Box(Modifier.width(StopSlotWidth), contentAlignment = Alignment.Center) {
            Surface(
                onClick = onStop,
                enabled = isPaused,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = if (isPaused) colors.danger else colors.bgSubtle,
                contentColor = if (isPaused) colors.inkOnBrand else colors.inkSubtle,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(TdrIcons.Stop, contentDescription = stringResource(R.string.run_stop), modifier = Modifier.size(18.dp))
                }
            }
        }
        Surface(
            onClick = onPauseToggle,
            modifier = Modifier
                .size(TdrSize.runButton)
                .shadow(TdrSpacing.s3, CircleShape, ambientColor = colors.brand, spotColor = colors.brand),
            shape = CircleShape,
            color = colors.brand,
            contentColor = colors.inkOnBrand,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isPaused) TdrIcons.Play else TdrIcons.Pause,
                    contentDescription = stringResource(if (isPaused) R.string.run_resume else R.string.run_pause),
                    modifier = Modifier.size(TdrSize.runButtonIcon),
                )
            }
        }
        // 일시정지 버튼이 화면 가운데에 오게 종료 묶음과 같은 너비를 비워 둔다.
        Spacer(Modifier.width(StopSlotWidth))
    }
}

/** 종료 버튼 칸 너비. 일시정지 버튼이 가운데에 오게 반대편도 같은 너비를 비운다. */
private val StopSlotWidth = 88.dp

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RunningScreenPreview() {
    RunningScreen(
        RunningUiState(
            distanceM = 3_240.0,
            movingSec = 1_122,
            avgPaceSecPerKm = 346,
            currentPaceSecPerKm = 331,
            stageWithRun = stageProgressUi(70_040, routeFor(1)),
            path = SampleRoutePoints,
        ),
        {}, {},
    )
}

/** 막 시작해 일시정지한 상태: GPS 약함, 경로 없음(미니 지도 숨김). */
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RunningScreenPausedPreview() {
    RunningScreen(
        RunningUiState(
            distanceM = 0.0,
            movingSec = 0,
            avgPaceSecPerKm = 0,
            currentPaceSecPerKm = 0,
            gpsGood = false,
            isPaused = true,
            stageWithRun = stageProgressUi(66_800, routeFor(1)),
        ),
        {}, {},
    )
}
