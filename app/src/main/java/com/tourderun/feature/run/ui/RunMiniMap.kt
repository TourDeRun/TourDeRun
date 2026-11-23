package com.tourderun.feature.run.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.tourderun.core.designsystem.component.RouteSketch
import com.tourderun.core.designsystem.component.StaticMapUiSettings
import com.tourderun.core.designsystem.component.toLatLng
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.domain.model.LatLngValue

/**
 * EXT-02 러닝 중 경로 미니 지도. 지금까지 채택한 점을 선으로, 현재 위치를 점으로 그리고 현재 위치를 따라간다.
 * 확대·이동은 막는다([StaticMapUiSettings]). 선 갱신 주기(5초)와 점 솎아내기는 ViewModel이 한다.
 * Preview에서는 지도 없이 [RouteSketch]로 그린다.
 */
@Composable
fun RunMiniMap(
    path: List<LatLngValue>,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    if (LocalInspectionMode.current || path.isEmpty()) {
        RouteSketch(
            points = path,
            modifier = modifier,
            background = colors.bgSurface,
            roadColor = colors.border,
            lineColor = colors.brandBright,
            pointColor = colors.ink,
        )
        return
    }
    val density = LocalDensity.current
    val latLngs = remember(path) { path.map { it.toLatLng() } }
    val current = latLngs.last()
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(current, FOLLOW_ZOOM) }
    LaunchedEffect(current) {
        camera.animate(CameraUpdateFactory.newLatLng(current), durationMs = FOLLOW_ANIMATION_MS)
    }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        uiSettings = StaticMapUiSettings,
    ) {
        if (latLngs.size >= 2) {
            Polyline(
                points = latLngs,
                color = colors.brandBright,
                width = with(density) { 5.dp.toPx() },
                jointType = JointType.ROUND,
                startCap = RoundCap(),
                endCap = RoundCap(),
            )
        }
        Circle(
            center = current,
            radius = CURRENT_DOT_RADIUS_M,
            fillColor = colors.brand,
            strokeColor = colors.bgSurface,
            strokeWidth = with(density) { 2.dp.toPx() },
        )
    }
}

private const val FOLLOW_ZOOM = 16f
private const val FOLLOW_ANIMATION_MS = 600
private const val CURRENT_DOT_RADIUS_M = 6.0
