package com.tourderun.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.domain.model.LatLngValue

fun LatLngValue.toLatLng(): LatLng = LatLng(lat, lng)

/** [points]를 모두 담는 범위. 비어 있으면 안 된다. */
fun latLngBoundsOf(points: List<LatLngValue>): LatLngBounds =
    LatLngBounds.builder().apply { points.forEach { include(it.toLatLng()) } }.build()

/** 확대·이동·버튼을 모두 끈 지도 설정 (기록 상세, 러닝 미니 지도). */
val StaticMapUiSettings = MapUiSettings(
    compassEnabled = false,
    indoorLevelPickerEnabled = false,
    mapToolbarEnabled = false,
    myLocationButtonEnabled = false,
    rotationGesturesEnabled = false,
    scrollGesturesEnabled = false,
    scrollGesturesEnabledDuringRotateOrZoom = false,
    tiltGesturesEnabled = false,
    zoomControlsEnabled = false,
    zoomGesturesEnabled = false,
)

/**
 * 달린 경로를 실제 지도 위에 그린다 (REC-03). 선 + 시작(흰 원)·끝(채운 원) 표시, 경로가 칸에 맞게 확대된다.
 * 지도는 움직이지 않는다([StaticMapUiSettings]).
 *
 * Preview와 점이 2개 미만일 때는 지도 없이 [RouteSketch]로 그린다.
 * API 키가 없거나(DUMMY) 키 인증에 실패하면 지도 칸이 빈 바탕으로만 보인다(선과 표시도 안 나온다). 앱은 꺼지지 않는다.
 */
@Composable
fun RouteMap(
    points: List<LatLngValue>,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current || points.size < 2) {
        RouteSketch(points, modifier)
        return
    }
    val colors = TdrTheme.colors
    val density = LocalDensity.current
    val latLngs = remember(points) { points.map { it.toLatLng() } }
    val bounds = remember(points) { latLngBoundsOf(points) }
    val camera = rememberCameraPositionState {
        // 지도가 준비되기 전(또는 타일을 못 받을 때)의 임시 위치
        position = CameraPosition.fromLatLngZoom(bounds.center, DEFAULT_ZOOM)
    }
    var loaded by remember { mutableStateOf(false) }
    // newLatLngBounds는 지도 크기가 정해진 뒤에만 쓸 수 있어서 onMapLoaded를 기다린다.
    LaunchedEffect(loaded, bounds) {
        if (loaded) {
            camera.move(CameraUpdateFactory.newLatLngBounds(bounds, with(density) { TdrSpacing.s8.roundToPx() }))
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        uiSettings = StaticMapUiSettings,
        onMapLoaded = { loaded = true },
    ) {
        Polyline(
            points = latLngs,
            color = colors.brand,
            width = with(density) { 5.dp.toPx() },
            jointType = JointType.ROUND,
            startCap = RoundCap(),
            endCap = RoundCap(),
        )
        MarkerComposable(
            state = remember(latLngs) { MarkerState(latLngs.first()) },
            anchor = Offset(0.5f, 0.5f),
        ) { RouteEndDot(filled = false) }
        MarkerComposable(
            state = remember(latLngs) { MarkerState(latLngs.last()) },
            anchor = Offset(0.5f, 0.5f),
        ) { RouteEndDot(filled = true) }
    }
}

/** 경로 시작(흰 원)·끝(채운 원) 표시. */
@Composable
private fun RouteEndDot(filled: Boolean) {
    val colors = TdrTheme.colors
    Box(
        Modifier
            .size(16.dp)
            .background(if (filled) colors.brand else colors.bgSurface, CircleShape)
            .border(3.dp, if (filled) colors.bgSurface else colors.brand, CircleShape),
    )
}

private const val DEFAULT_ZOOM = 14f
