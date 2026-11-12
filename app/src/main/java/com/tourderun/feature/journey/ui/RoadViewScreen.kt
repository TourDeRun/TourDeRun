package com.tourderun.feature.journey.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tourderun.R
import com.tourderun.core.designsystem.component.MascotFace
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.component.TdrProgressBar
import com.tourderun.core.designsystem.component.TdrTab
import com.tourderun.core.designsystem.component.TeamImage
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format
import com.tourderun.core.ui.model.RivalTeamUi
import com.tourderun.core.ui.model.StageProgressUi
import com.tourderun.core.ui.model.toNextCityText
import com.tourderun.core.ui.model.TeamUi
import com.tourderun.core.ui.preview.PreviewSamples
import com.tourderun.core.ui.preview.TabScreenPreview
import kotlin.math.roundToInt

/** S08 로드뷰 화면 상태. */
data class RoadViewUiState(
    val team: TeamUi,
    val stage: StageProgressUi,
    val rank: Int,
    val totalTeams: Int,
    /** 같은 스테이지에 있는 다른 팀. */
    val rivalsInStage: List<RivalTeamUi>,
)

/**
 * S08 로드뷰: 지금 스테이지 위의 우리 팀과 다른 팀.
 * 지도는 아직 자리표시([StageMapSketch])다.
 * TODO(JRN): maps-compose를 넣으면 [StageMapSketch] 자리에 GoogleMap + 마커를 그린다. 위·아래 카드는 그대로 둔다.
 */
@Composable
fun RoadViewScreen(
    uiState: RoadViewUiState,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val stage = uiState.stage
    Box(modifier.fillMaxSize().background(colors.bgMap)) {
        StageMapSketch(uiState, Modifier.fillMaxSize())

        // 위: 스테이지 카드
        TdrCard(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3)
                .shadow(12.dp, TdrShapes.lg),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(TdrSpacing.s2),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.road_header, stage.stageNumber, stage.stageCount, uiState.totalTeams, uiState.rank),
                    style = type.caption,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onViewAll,
                    shape = TdrShapes.full,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.textButtonColors(containerColor = colors.brandSoft),
                ) {
                    Text(stringResource(R.string.road_view_all), style = type.caption.copy(fontWeight = FontWeight.Bold), color = colors.brand)
                }
            }
            Text(stringResource(R.string.route_format, stage.fromCity, stage.toCity), style = type.statSm)
            TdrProgressBar(stage.progress, height = 6.dp, contentDescription = stringResource(R.string.stage_progress_desc))
        }

        // 아래: 다음 도시 + 범례
        TdrCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, bottom = TdrSpacing.s4)
                .shadow(12.dp, TdrShapes.lg),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.road_next_city), style = type.caption, color = colors.inkMuted)
                    Text(
                        stage.toNextCityText(),
                        style = type.subtitle.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendItem(colors.brand, stringResource(R.string.road_legend_done))
                    LegendItem(colors.routeRemaining, stringResource(R.string.road_legend_left))
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.width(16.dp).height(4.dp).clip(TdrShapes.full).background(color))
        Text(label, style = TdrTheme.typography.micro, color = TdrTheme.colors.inkMuted)
    }
}

/**
 * 지도 자리표시: 스테이지 구간을 곡선 하나로 그리고 팀 위치를 진행률대로 올린다.
 * 와이어프레임(390×760) 좌표를 화면 크기에 맞춰 늘린다.
 */
@Composable
private fun StageMapSketch(uiState: RoadViewUiState, modifier: Modifier) {
    val colors = TdrTheme.colors
    val stage = uiState.stage
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val sx = w / 390f
        val sy = h / 760f
        val path = remember(w, h) {
            Path().apply {
                moveTo(86 * sx, 616 * sy)
                cubicTo(120 * sx, 560 * sy, 100 * sx, 520 * sy, 150 * sx, 470 * sy)
                cubicTo(200 * sx, 420 * sy, 270 * sx, 380 * sy, 242 * sx, 225 * sy)
            }
        }
        val measure = remember(path) { PathMeasure().apply { setPath(path, false) } }
        fun at(t: Float): Offset = measure.getPosition(measure.length * t.coerceIn(0f, 1f))

        Canvas(Modifier.fillMaxSize()) {
            val road = Stroke(width = 10.dp.toPx())
            drawPath(
                Path().apply {
                    moveTo(40 * sx, 120 * sy); quadraticBezierTo(120 * sx, 90 * sy, 160 * sx, 160 * sy)
                    moveTo(300 * sx, 40 * sy); quadraticBezierTo(260 * sx, 200 * sy, 340 * sx, 320 * sy)
                    moveTo(0f, 520 * sy); quadraticBezierTo(160 * sx, 480 * sy, w, 560 * sy)
                },
                colors.mapRoad,
                style = road,
            )
            drawPath(
                path,
                colors.routeRemaining,
                style = Stroke(
                    width = 7.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 12.dp.toPx())),
                ),
            )
            val done = Path()
            measure.getSegment(0f, measure.length * stage.progress, done, true)
            drawPath(done, colors.brand, style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
        }

        // 지나온 도시 (스테이지 출발)
        MapMarker(at(0f), 32.dp) {
            Box(Modifier.size(32.dp).background(colors.brand, CircleShape), contentAlignment = Alignment.Center) {
                Icon(TdrIcons.CheckBold, contentDescription = null, tint = colors.inkOnBrand, modifier = Modifier.size(16.dp))
            }
            MarkerLabel(
                stringResource(R.string.road_city_arrived, stage.fromCity, stage.startKm),
                colors.bgSurface,
                colors.inkMuted,
                TdrShapes.sm,
            )
        }
        // 다음 도시
        MapMarker(at(1f), 40.dp) {
            Box(
                Modifier.size(40.dp).shadow(8.dp, CircleShape).background(colors.bgSurface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(TdrIcons.Flag, contentDescription = null, tint = colors.brand, modifier = Modifier.size(20.dp))
            }
            MarkerLabel(
                stringResource(R.string.road_city_next, stage.toCity, stage.endKm),
                colors.brandStrong,
                colors.inkOnBrand,
                TdrShapes.sm,
            )
        }
        // 다른 팀
        uiState.rivalsInStage.forEach { rival ->
            MapMarker(at(stage.progressOf(rival.distanceM)), 38.dp) {
                TeamImage(rival.appearance, size = 38.dp, circle = true)
                MarkerLabel(
                    stringResource(R.string.road_team_marker, rival.name, Format.km(rival.distanceM)),
                    colors.bgSurface,
                    rival.appearance.color.textColor,
                    TdrShapes.full,
                )
            }
        }
        // 우리 팀 (맨 위)
        MapMarker(at(stage.progress), 58.dp, Modifier.zIndex(1f)) {
            TeamImage(
                uiState.team.appearance,
                size = 58.dp,
                circle = true,
                face = MascotFace.Open,
                modifier = Modifier
                    .shadow(12.dp, CircleShape, ambientColor = colors.brand, spotColor = colors.brand)
                    .border(4.dp, Color.White, CircleShape),
            )
            Column(
                Modifier
                    .shadow(6.dp, TdrShapes.md)
                    .clip(TdrShapes.md)
                    .background(colors.brand)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    stringResource(R.string.road_our_team, uiState.team.name),
                    style = TdrTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                    color = colors.inkOnBrand,
                )
                Text(
                    stringResource(R.string.km_value, Format.km(uiState.team.distanceM)),
                    style = TdrTheme.typography.micro.copy(fontWeight = FontWeight.Medium),
                    color = colors.inkOnBrand,
                )
            }
        }
    }
}

/** 아이콘 가운데가 [position]에 오도록 놓고, 라벨은 오른쪽에 붙인다. */
@Composable
private fun MapMarker(position: Offset, iconSize: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val half = with(LocalDensity.current) { iconSize.toPx() / 2 }
    Row(
        modifier.offset { IntOffset((position.x - half).roundToInt(), (position.y - half).roundToInt()) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}

@Composable
private fun MarkerLabel(text: String, background: Color, color: Color, shape: Shape) {
    Text(
        text,
        style = TdrTheme.typography.micro.copy(fontWeight = FontWeight.Bold),
        color = color,
        modifier = Modifier
            .shadow(1.dp, shape)
            .clip(shape)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RoadViewScreenPreview() {
    TabScreenPreview(TdrTab.Road) {
        RoadViewScreen(
            RoadViewUiState(
                team = PreviewSamples.team,
                stage = PreviewSamples.stage,
                rank = 2,
                totalTeams = 5,
                rivalsInStage = PreviewSamples.rivals.take(2),
            ),
            onViewAll = {},
        )
    }
}
