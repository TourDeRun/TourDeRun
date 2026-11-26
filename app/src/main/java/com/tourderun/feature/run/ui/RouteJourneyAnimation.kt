package com.tourderun.feature.run.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.domain.model.Route
import kotlin.math.cos
import kotlin.math.max

/**
 * S09 시작 화면 그림: 서울 → 부산 경로선 위에 우리 팀 위치까지 선이 그려지고, 팀 위치가 맥박처럼 뛴다.
 * 좌표는 그림 배치에만 쓰고, 팀 위치는 고정 구간 거리(누적 거리) 비율로 정한다(좌표로 거리를 다시 계산하지 않는다).
 */
@Composable
fun RouteJourneyAnimation(
    route: Route,
    teamDistanceM: Long,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val labelStyle = TdrTheme.typography.micro
    val measurer = rememberTextMeasurer()
    val draw = remember(route, teamDistanceM) { Animatable(0f) }
    LaunchedEffect(draw) { draw.animateTo(1f, tween(DRAW_MS, easing = FastOutSlowInEasing)) }
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PULSE_MS), RepeatMode.Restart),
        label = "pulse",
    )

    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        val labelSpace = (TdrSpacing.s8 + TdrSpacing.s6).toPx()
        val pad = TdrSpacing.s3.toPx()
        // 위도 1도와 경도 1도의 실제 길이 비율을 맞춰 한반도 모양이 찌그러지지 않게 한다.
        val lngScale = cos(Math.toRadians(route.cities.map { it.lat }.average()))
        val minLat = route.cities.minOf { it.lat }
        val maxLat = route.cities.maxOf { it.lat }
        val minLng = route.cities.minOf { it.lng }
        val maxLng = route.cities.maxOf { it.lng }
        val spanX = (maxLng - minLng) * lngScale
        val spanY = maxLat - minLat
        val scale = minOf((size.width - labelSpace - pad * 2) / spanX, (size.height - pad * 2) / spanY).toFloat()
        // 도시 이름 칸까지 합친 그림이 가운데 오게 한다.
        val offsetX = (size.width - labelSpace - spanX.toFloat() * scale) / 2 + labelSpace / 2
        val offsetY = (size.height - spanY.toFloat() * scale) / 2
        val points = route.cities.map {
            Offset(
                offsetX + ((it.lng - minLng) * lngScale).toFloat() * scale,
                offsetY + (maxLat - it.lat).toFloat() * scale,
            )
        }

        val stroke = TdrSpacing.s15.toPx()
        val base = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(base, colors.bgSubtle, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // 지나온 길: 시작점에서 팀 위치까지, 처음 보일 때 서서히 그려진다.
        val shownM = (teamDistanceM.coerceIn(0, route.totalM) * draw.value).toLong()
        val done = Path().apply { moveTo(points.first().x, points.first().y) }
        var tip = points.first()
        for (i in route.stages.indices) {
            val startM = route.stageStartM(i)
            val endM = route.stageEndM(i)
            if (shownM <= startM) break
            val t = ((shownM - startM).toFloat() / (endM - startM)).coerceAtMost(1f)
            tip = points[i] + (points[i + 1] - points[i]) * t
            done.lineTo(tip.x, tip.y)
        }
        drawPath(done, colors.brandBright, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        points.forEachIndexed { i, p ->
            val reached = i == 0 || route.stageEndM(i - 1) <= teamDistanceM
            drawCircle(colors.bgCanvas, radius = TdrSpacing.s15.toPx(), center = p)
            drawCircle(if (reached) colors.brandBright else colors.inkSubtle, radius = TdrSpacing.s1.toPx(), center = p)
            val label = measurer.measure(route.cities[i].nameKo, labelStyle)
            drawText(
                label,
                color = if (reached) colors.ink else colors.inkMuted,
                topLeft = Offset(p.x + TdrSpacing.s25.toPx(), p.y - label.size.height / 2f),
            )
        }

        // 우리 팀 위치: 퍼지며 사라지는 원 + 가운데 점
        val ring = TdrSpacing.s2.toPx() + TdrSpacing.s4.toPx() * pulse
        drawCircle(colors.brandBright.copy(alpha = max(0f, 0.5f * (1f - pulse))), radius = ring, center = tip)
        drawCircle(colors.ink, radius = TdrSpacing.s25.toPx(), center = tip)
        drawCircle(colors.brandBright, radius = TdrSpacing.s15.toPx(), center = tip)
    }
}

private const val DRAW_MS = 1_200
private const val PULSE_MS = 1_600
