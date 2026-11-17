package com.tourderun.feature.journey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.tourderun.R
import com.tourderun.core.designsystem.component.MascotFace
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.component.TdrProgressBar
import com.tourderun.core.designsystem.component.TdrTab
import com.tourderun.core.designsystem.component.TeamImage
import com.tourderun.core.designsystem.component.latLngBoundsOf
import com.tourderun.core.designsystem.component.toLatLng
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSize
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format
import com.tourderun.core.ui.model.StageProgressUi
import com.tourderun.core.ui.model.TeamUi
import com.tourderun.core.ui.model.stageProgressUi
import com.tourderun.core.ui.model.toNextCityText
import com.tourderun.core.ui.preview.PreviewSamples
import com.tourderun.core.ui.preview.TabScreenPreview
import com.tourderun.domain.route.routeFor

/** S08 로드뷰 화면 상태. */
data class RoadViewUiState(
    val team: TeamUi,
    val stage: StageProgressUi,
    val rank: Int,
    val totalTeams: Int,
    /** 지도에 그릴 경로·도시·팀 위치. `roadMapUi()`로 만든다. */
    val map: RoadMapUi,
)

/**
 * S08 로드뷰 (JRN-04): Google 지도 위 전체 경로와 도시, 우리 팀·다른 팀 위치.
 * 처음에는 지금 스테이지가 화면에 맞게 보이고, 위 카드의 버튼으로 전체 경로와 오간다.
 * 팀 이름과 거리는 표시 옆 라벨로 보인다(우리 팀은 오른쪽, 다른 팀은 왼쪽). 도시 라벨은 지나온 도시는 위, 나머지는 아래.
 */
@Composable
fun RoadViewScreen(
    modifier: Modifier = Modifier,
    viewModel: RoadViewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState
    if (state == null) {
        // 받기 전이거나 팀이 없을 때: 지도 바탕색만
        Box(modifier.fillMaxSize().background(TdrTheme.colors.bgMap))
    } else {
        RoadViewContent(state, modifier)
    }
}

/** S08 화면 내용. 상태 없음(Preview 대상). */
@Composable
fun RoadViewContent(
    uiState: RoadViewUiState,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val stage = uiState.stage
    var showAll by rememberSaveable { mutableStateOf(false) }
    // 카드에 가려지는 만큼 지도 안쪽 여백으로 줘서, 경로가 카드 사이 보이는 칸에 맞춰진다.
    var topCardPx by remember { mutableIntStateOf(0) }
    var bottomCardPx by remember { mutableIntStateOf(0) }

    Box(modifier.fillMaxSize().background(colors.bgMap)) {
        RoadMap(uiState, showAll, topCardPx, bottomCardPx, Modifier.fillMaxSize())

        // 위: 스테이지 카드
        TdrCard(
            modifier = Modifier
                .onSizeChanged { topCardPx = it.height }
                .statusBarsPadding()
                .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3)
                .shadow(12.dp, TdrShapes.lg),
            contentPadding = PaddingValues(horizontal = TdrSpacing.s4, vertical = TdrSpacing.s35),
            verticalArrangement = Arrangement.spacedBy(TdrSpacing.s2),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.road_header, stage.stageNumber, stage.stageCount, uiState.totalTeams, uiState.rank),
                    style = type.caption,
                    color = colors.inkMuted,
                    modifier = Modifier.weight(1f),
                )
                // 보이는 칩은 작게 두고 터치 영역만 최소 크기로 넓힌다.
                Box(
                    Modifier
                        .defaultMinSize(minWidth = TdrSize.touch, minHeight = TdrSize.touch)
                        .clip(TdrShapes.full)
                        .clickable(role = Role.Button) { showAll = !showAll },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(if (showAll) R.string.road_view_stage else R.string.road_view_all),
                        style = type.captionBold,
                        color = colors.brand,
                        modifier = Modifier
                            .clip(TdrShapes.full)
                            .background(colors.brandSoft)
                            .padding(horizontal = TdrSpacing.s25, vertical = TdrSpacing.s15),
                    )
                }
            }
            Text(stringResource(R.string.route_format, stage.fromCity, stage.toCity), style = type.statSm)
            TdrProgressBar(stage.progress, height = 6.dp, contentDescription = stringResource(R.string.stage_progress_desc))
        }

        // 아래: 다음 도시 + 범례
        TdrCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { bottomCardPx = it.height }
                .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, bottom = TdrSpacing.s4)
                .shadow(12.dp, TdrShapes.lg),
            contentPadding = PaddingValues(horizontal = TdrSpacing.s4, vertical = TdrSpacing.s3),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TdrSpacing.s05)) {
                    Text(stringResource(R.string.road_next_city), style = type.caption, color = colors.inkMuted)
                    Text(
                        stage.toNextCityText(),
                        style = type.subtitle.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s25)) {
                    LegendItem(colors.brand, stringResource(R.string.road_legend_done))
                    LegendItem(colors.routeRemaining, stringResource(R.string.road_legend_left))
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s1)) {
        Box(Modifier.width(16.dp).height(4.dp).clip(TdrShapes.full).background(color))
        Text(label, style = TdrTheme.typography.micro, color = TdrTheme.colors.inkMuted)
    }
}

/**
 * 경로(지나온 길 강조색 · 남은 길 회색 점선), 도시, 팀 표시를 올린 지도.
 * Preview에서는 `GoogleMap`이 그려지지 않아 바탕색만 보인다.
 */
@Composable
private fun RoadMap(uiState: RoadViewUiState, showAll: Boolean, topCardPx: Int, bottomCardPx: Int, modifier: Modifier) {
    val colors = TdrTheme.colors
    val density = LocalDensity.current
    val map = uiState.map
    val camera = rememberCameraPositionState {
        // 지도가 준비되기 전(또는 타일을 못 받을 때)의 임시 위치
        position = CameraPosition.fromLatLngZoom(map.ourPosition.toLatLng(), DEFAULT_ZOOM)
    }
    var loaded by remember { mutableStateOf(false) }
    val focus = if (showAll) map.cities.map { it.position } else map.stageFocus + map.ourPosition
    // newLatLngBounds는 지도 크기가 정해진 뒤에만 쓸 수 있어서 onMapLoaded를 기다린다.
    LaunchedEffect(loaded, focus, topCardPx, bottomCardPx) {
        if (loaded) {
            val edge = with(density) { FocusEdge.roundToPx() }
            camera.animate(CameraUpdateFactory.newLatLngBounds(latLngBoundsOf(focus), edge))
        }
    }
    val lineWidth = with(density) { 7.dp.toPx() }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        uiSettings = RoadMapUiSettings,
        contentPadding = with(density) { PaddingValues(top = topCardPx.toDp(), bottom = bottomCardPx.toDp()) },
        onMapLoaded = { loaded = true },
    ) {
        Polyline(
            points = map.remainingPath.map { it.toLatLng() },
            color = colors.routeRemaining,
            width = lineWidth,
            pattern = listOf(Dot(), Gap(lineWidth)),
        )
        Polyline(
            points = map.passedPath.map { it.toLatLng() },
            color = colors.brand,
            width = lineWidth,
            jointType = JointType.ROUND,
            startCap = RoundCap(),
            endCap = RoundCap(),
        )

        map.cities.forEach { city ->
            val arrived = city.kind == CityPinKind.ARRIVED
            val label = if (arrived) {
                stringResource(R.string.road_city_arrived, city.name)
            } else {
                stringResource(R.string.road_city_next, city.name, city.cumulativeKm)
            }
            // 전체 보기에서는 지나온 도시들이 서로 붙어 라벨이 겹치므로, 지나온 도시는 체크 표시만 남긴다.
            val showLabel = !showAll || city.kind != CityPinKind.ARRIVED
            MarkerComposable(
                city.kind,
                label,
                showLabel,
                state = remember(city.position) { MarkerState(city.position.toLatLng()) },
                anchor = PinCenter,
            ) {
                // 지나온 도시 라벨은 위에 둔다. 도시를 막 지난 팀 표시(경로상 아래쪽)에 가려지지 않게.
                CenteredPin(labelHeight = 28.dp, labelAbove = arrived, icon = { CityIcon(city.kind) }) {
                    when {
                        !showLabel -> Unit
                        city.kind == CityPinKind.NEXT ->
                            MarkerLabel(label, colors.brandStrong, colors.inkOnBrand, TdrShapes.sm)
                        else -> MarkerLabel(label, colors.bgSurface, colors.inkMuted, TdrShapes.sm)
                    }
                }
            }
        }

        map.rivals.forEach { rival ->
            val rivalLabel = stringResource(R.string.road_team_distance, rival.team.name, Format.teamKm(rival.team.distanceM))
            MarkerComposable(
                rival.team,
                showAll,
                state = remember(rival.position) { MarkerState(rival.position.toLatLng()) },
                anchor = PinCenter,
                zIndex = 1f,
                onClick = { true },
            ) {
                SidePin(
                    labelOnStart = true,
                    icon = {
                        TeamImage(
                            rival.team.appearance,
                            size = 38.dp,
                            circle = true,
                            modifier = Modifier.border(2.dp, colors.bgSurface, CircleShape),
                        )
                    },
                ) {
                    // 전체 경로에서는 팀들이 붙어 라벨이 겹치므로 표시만 남긴다.
                    if (!showAll) {
                        MarkerLabel(rivalLabel, colors.bgSurface, rival.team.appearance.color.textColor, TdrShapes.full)
                    }
                }
            }
        }

        val ourKm = stringResource(R.string.km_value, Format.teamKm(uiState.team.distanceM))
        MarkerComposable(
            uiState.team,
            state = remember(map.ourPosition) { MarkerState(map.ourPosition.toLatLng()) },
            anchor = PinCenter,
            zIndex = 2f,
            onClick = { true },
        ) {
            SidePin(
                labelOnStart = false,
                icon = {
                    TeamImage(
                        uiState.team.appearance,
                        size = OUR_ICON_SIZE.dp,
                        circle = true,
                        face = MascotFace.Open,
                        modifier = Modifier.border(4.dp, colors.bgSurface, CircleShape),
                    )
                },
            ) {
                Column(
                    Modifier.clip(TdrShapes.md).background(colors.brand).padding(horizontal = TdrSpacing.s25, vertical = TdrSpacing.s15),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.road_our_team, uiState.team.name),
                        style = TdrTheme.typography.captionBold,
                        color = colors.inkOnBrand,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        ourKm,
                        style = TdrTheme.typography.micro.copy(fontWeight = FontWeight.Medium),
                        color = colors.inkOnBrand,
                    )
                }
            }
        }
    }
}

/**
 * 아이콘 가운데가 지도 좌표에 오도록 하는 표시 틀. 지도 표시는 그림 전체의 한 점([PinCenter])만 좌표에 맞출 수 있어서,
 * 아이콘 아래 라벨 칸과 같은 높이의 빈 칸을 위에도 둬 아이콘이 그림 한가운데에 오게 한다.
 */
@Composable
private fun CenteredPin(
    labelHeight: Dp,
    icon: @Composable () -> Unit,
    labelAbove: Boolean = false,
    label: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(labelHeight).padding(bottom = TdrSpacing.s1), contentAlignment = Alignment.BottomCenter) {
            if (labelAbove) label()
        }
        icon()
        Box(Modifier.height(labelHeight).padding(top = TdrSpacing.s1), contentAlignment = Alignment.TopCenter) {
            if (!labelAbove) label()
        }
    }
}

/**
 * 아이콘 옆에 라벨을 두는 표시 틀(팀). [CenteredPin]과 같은 이유로 양옆에 같은 너비의 칸을 둬서
 * 아이콘이 그림 한가운데에 오게 한다. 라벨은 [SideLabelWidth]를 넘으면 말줄임.
 */
@Composable
private fun SidePin(labelOnStart: Boolean, icon: @Composable () -> Unit, label: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(SideLabelWidth).padding(end = TdrSpacing.s15), contentAlignment = Alignment.CenterEnd) {
            if (labelOnStart) label()
        }
        icon()
        Box(Modifier.width(SideLabelWidth).padding(start = TdrSpacing.s15), contentAlignment = Alignment.CenterStart) {
            if (!labelOnStart) label()
        }
    }
}

@Composable
private fun CityIcon(kind: CityPinKind) {
    val colors = TdrTheme.colors
    when (kind) {
        CityPinKind.ARRIVED ->
            Box(Modifier.size(28.dp).background(colors.brand, CircleShape), contentAlignment = Alignment.Center) {
                Icon(TdrIcons.CheckBold, contentDescription = null, tint = colors.inkOnBrand, modifier = Modifier.size(14.dp))
            }
        CityPinKind.NEXT ->
            Box(
                Modifier.size(36.dp).background(colors.bgSurface, CircleShape).border(2.dp, colors.brand, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(TdrIcons.Flag, contentDescription = null, tint = colors.brand, modifier = Modifier.size(18.dp))
            }
        CityPinKind.LATER ->
            Box(Modifier.size(14.dp).background(colors.bgSurface, CircleShape).border(3.dp, colors.routeRemaining, CircleShape))
    }
}

@Composable
private fun MarkerLabel(text: String, background: Color, color: Color, shape: Shape) {
    Text(
        text,
        style = TdrTheme.typography.microBold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(shape)
            .background(background)
            .padding(horizontal = TdrSpacing.s2, vertical = TdrSpacing.s1),
    )
}

/** 확대·이동은 되고, 화면을 가리는 버튼과 기울이기·돌리기는 끈다. */
private val RoadMapUiSettings = MapUiSettings(
    compassEnabled = false,
    indoorLevelPickerEnabled = false,
    mapToolbarEnabled = false,
    myLocationButtonEnabled = false,
    rotationGesturesEnabled = false,
    tiltGesturesEnabled = false,
    zoomControlsEnabled = false,
)

private val PinCenter = Offset(0.5f, 0.5f)

/** 범위를 맞출 때 가장자리에 두는 여백. 끝 도시의 표시와 라벨(좌표에서 아래·옆으로 뻗는다)이 잘리지 않을 만큼. */
private val FocusEdge = 72.dp
private const val DEFAULT_ZOOM = 10f
private const val OUR_ICON_SIZE = 58f

/** 팀 라벨 칸 너비. 팀 이름 12자 + 거리가 들어갈 만큼. */
private val SideLabelWidth = 152.dp

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RoadViewScreenPreview() {
    TabScreenPreview(TdrTab.Road) {
        RoadViewContent(
            RoadViewUiState(
                team = PreviewSamples.team,
                stage = PreviewSamples.stage,
                rank = 2,
                totalTeams = 5,
                map = roadMapUi(routeFor(1), PreviewSamples.team.distanceM, PreviewSamples.rivals.take(2)),
            ),
        )
    }
}

/** 빈 상태: 출발 전(0km), 지도에 다른 팀 없음. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RoadViewScreenEmptyPreview() {
    TabScreenPreview(TdrTab.Road) {
        RoadViewContent(
            RoadViewUiState(
                team = PreviewSamples.team.copy(memberCount = 1, distanceM = 0),
                stage = stageProgressUi(0, routeFor(1)),
                rank = 1,
                totalTeams = 1,
                map = roadMapUi(routeFor(1), 0, emptyList()),
            ),
        )
    }
}
