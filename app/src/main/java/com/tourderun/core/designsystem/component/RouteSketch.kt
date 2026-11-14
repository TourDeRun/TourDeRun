package com.tourderun.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.domain.model.LatLngValue
import kotlin.math.cos
import kotlin.math.max

/**
 * 지도 SDK 없이 달린 경로 모양만 그린다 (기록 상세 지도, 러닝 미니 지도의 자리표시).
 * 위경도를 칸 크기에 맞춰 늘려 그리며 시작점은 흰 원, 끝점은 채운 원이다.
 * TODO(REC·RUN): 지도 SDK(maps-compose)를 넣으면 실제 지도로 바꾼다.
 */
@Composable
fun RouteSketch(
    points: List<LatLngValue>,
    modifier: Modifier = Modifier,
    background: Color = TdrTheme.colors.bgMap,
    roadColor: Color = TdrTheme.colors.mapRoad,
    lineColor: Color = TdrTheme.colors.brand,
    pointColor: Color = TdrTheme.colors.bgSurface,
) {
    Canvas(modifier) {
        drawRect(background)
        // 배경 도로 몇 줄 (장식)
        val road = Stroke(width = 10.dp.toPx())
        drawPath(
            Path().apply {
                moveTo(0f, size.height * 0.3f)
                quadraticBezierTo(size.width * 0.35f, size.height * 0.15f, size.width, size.height * 0.38f)
                moveTo(size.width * 0.26f, 0f)
                lineTo(size.width * 0.34f, size.height)
                moveTo(0f, size.height * 0.8f)
                quadraticBezierTo(size.width * 0.57f, size.height * 0.7f, size.width, size.height * 0.88f)
            },
            roadColor,
            style = road,
        )
        if (points.size < 2) return@Canvas

        val midLat = points.sumOf { it.lat } / points.size
        val lngScale = cos(Math.toRadians(midLat))
        val xs = points.map { it.lng * lngScale }
        val ys = points.map { -it.lat }
        val minX = xs.min()
        val minY = ys.min()
        val spanX = xs.max() - minX
        val spanY = ys.max() - minY
        val pad = 24.dp.toPx()
        val scale = minOf(
            (size.width - pad * 2) / max(spanX, 1e-9),
            (size.height - pad * 2) / max(spanY, 1e-9),
        )
        val offX = (size.width - spanX * scale) / 2
        val offY = (size.height - spanY * scale) / 2
        val mapped = xs.indices.map { i ->
            Offset((offX + (xs[i] - minX) * scale).toFloat(), (offY + (ys[i] - minY) * scale).toFloat())
        }
        val path = Path().apply {
            moveTo(mapped.first().x, mapped.first().y)
            mapped.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, lineColor, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val r = 8.dp.toPx()
        val ring = Stroke(width = 3.dp.toPx())
        drawCircle(pointColor, r, mapped.first())
        drawCircle(lineColor, r, mapped.first(), style = ring)
        drawCircle(lineColor, r, mapped.last())
        drawCircle(pointColor, r, mapped.last(), style = ring)
    }
}

/** 미리보기용 짧은 경로 (한강 근처). */
val SampleRoutePoints: List<LatLngValue> = listOf(
    LatLngValue(37.5170, 126.9550),
    LatLngValue(37.5195, 126.9585),
    LatLngValue(37.5200, 126.9630),
    LatLngValue(37.5230, 126.9670),
    LatLngValue(37.5255, 126.9720),
    LatLngValue(37.5248, 126.9775),
    LatLngValue(37.5222, 126.9800),
    LatLngValue(37.5190, 126.9790),
)
