package com.tourderun.feature.run.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tourderun.R
import com.tourderun.core.designsystem.component.Pill
import com.tourderun.core.designsystem.component.RouteSketch
import com.tourderun.core.designsystem.component.SampleRoutePoints
import com.tourderun.core.designsystem.component.StatColumn
import com.tourderun.core.designsystem.component.StatRow
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.component.TdrProgressBar
import com.tourderun.core.designsystem.theme.TdrShapes
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
 * 종료는 오조작을 막으려 길게 눌러야 한다.
 */
@Composable
fun RunningScreen(
    uiState: RunningUiState,
    onPauseToggle: () -> Unit,
    onStopLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TdrTheme(darkTheme = true) {
        RunningContent(uiState, onPauseToggle, onStopLongPress, modifier)
    }
}

@Composable
private fun RunningContent(
    uiState: RunningUiState,
    onPauseToggle: () -> Unit,
    onStopLongPress: () -> Unit,
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
                modifier = Modifier.padding(start = 6.dp).weight(1f),
            )
            Pill(
                stringResource(if (uiState.gpsGood) R.string.run_gps_ok else R.string.run_gps_weak),
                background = colors.bgSurface,
                contentColor = if (uiState.gpsGood) colors.inkMuted else colors.highlightInk,
                style = type.micro.copy(lineHeight = TdrTheme.typography.caption.lineHeight),
            )
        }

        Column(
            Modifier.fillMaxWidth().padding(top = TdrSpacing.s3, bottom = 4.dp),
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

        TdrCard(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row {
                Text(stringResource(R.string.run_team_adding), style = type.caption, color = colors.inkMuted, modifier = Modifier.weight(1f))
                Text(
                    Format.signedKm(runM, decimals = 2) + stringResource(R.string.unit_km),
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
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
                    Format.km(stage.teamDistanceM),
                    stage.stageNumber,
                    stage.fromCity,
                    stage.toCity,
                    (stage.progress * 100).roundToInt(),
                ),
                style = type.caption,
                color = colors.inkMuted,
            )
        }

        if (uiState.path.size >= 2) {
            Box(Modifier.weight(1f).fillMaxWidth().clip(TdrShapes.lg)) {
                RouteSketch(
                    points = uiState.path,
                    modifier = Modifier.fillMaxSize(),
                    background = colors.bgSurface,
                    roadColor = colors.border,
                    lineColor = colors.brandBright,
                    pointColor = colors.ink,
                )
                Pill(
                    stringResource(R.string.run_minimap),
                    background = colors.bgSubtle,
                    contentColor = colors.inkMuted,
                    style = type.micro,
                    modifier = Modifier.padding(start = TdrSpacing.s3, top = 10.dp),
                )
            }
        } else {
            Box(Modifier.weight(1f))
        }

        RunControls(uiState.isPaused, onPauseToggle, onStopLongPress)
    }
}

@Composable
private fun RunControls(isPaused: Boolean, onPauseToggle: () -> Unit, onStopLongPress: () -> Unit) {
    val colors = TdrTheme.colors
    val stopLabel = stringResource(R.string.run_stop)
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 종료: 길게 눌러야 동작. 접근성 서비스에는 길게 누르기 동작으로 알린다.
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.bgSubtle)
                .pointerInput(onStopLongPress) { detectTapGestures(onLongPress = { onStopLongPress() }) }
                .semantics {
                    contentDescription = stopLabel
                    role = Role.Button
                    onLongClick(label = stopLabel) {
                        onStopLongPress()
                        true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(TdrIcons.Stop, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
        }
        Surface(
            onClick = onPauseToggle,
            modifier = Modifier
                .size(80.dp)
                .shadow(12.dp, CircleShape, ambientColor = colors.brand, spotColor = colors.brand),
            shape = CircleShape,
            color = colors.brand,
            contentColor = colors.inkOnBrand,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isPaused) TdrIcons.Play else TdrIcons.Pause,
                    contentDescription = stringResource(if (isPaused) R.string.run_resume else R.string.run_pause),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Text(
            stringResource(R.string.run_stop_hint),
            style = TdrTheme.typography.micro,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp),
        )
    }
}

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
