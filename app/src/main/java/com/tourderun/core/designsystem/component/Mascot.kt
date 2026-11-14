package com.tourderun.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tourderun.core.designsystem.theme.TdrTheme

/** 마스코트 뒤 배경 모양. */
enum class MascotFrame { None, Circle, Rounded }

/** 마스코트 표정. */
enum class MascotFace {
    /** 기본 웃음 (볼 터치 포함). */
    Smile,
    /** 입 벌린 웃음 — 팀 생성 완료, 로드뷰의 우리 팀. */
    Open,
    /** 눈 감고 활짝 — 스테이지 클리어. */
    Joy,
    /** 졸린 눈 — 뒤처진 팀. */
    Sleepy,
    /** 볼 터치 없는 단순한 얼굴 — 작은 아바타. */
    Plain,
}

private val Ink = Color(0xFF171717)
private val Cheek = Color(0xFFFFB3C1)
private val Body = Color.White

/**
 * 마스코트 「뛰뛰」. 와이어프레임 SVG(100×100)를 그대로 옮겼다.
 *
 * @param background 배경색. [MascotFrame.None]이면 무시한다.
 * @param bandColor 머리띠 색. 배경과 구분되는 색을 준다 ([TeamColor.bandColor]).
 */
@Composable
fun Mascot(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    frame: MascotFrame = MascotFrame.Circle,
    background: Color = TdrTheme.colors.brandSoft,
    bandColor: Color = TdrTheme.colors.brandStrong,
    face: MascotFace = MascotFace.Smile,
    contentDescription: String? = null,
) {
    val a11y = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier
    }
    Canvas(modifier.size(size).then(a11y)) {
        val unit = this.size.minDimension / 100f
        when (frame) {
            MascotFrame.None -> Unit
            MascotFrame.Circle -> drawCircle(background)
            MascotFrame.Rounded -> drawRoundRect(background, cornerRadius = CornerRadius(24f * unit))
        }
        // SVG의 translate(13 12) scale(0.74) / translate(14 17) scale(0.72)
        val (ox, oy, k) = when (frame) {
            MascotFrame.None -> Triple(0f, 0f, 1f)
            MascotFrame.Circle -> Triple(13f, 12f, 0.74f)
            MascotFrame.Rounded -> Triple(14f, 17f, 0.72f)
        }
        MascotPainter(this, unit, ox, oy, k).draw(bandColor, face)
    }
}

private class MascotPainter(
    private val scope: DrawScope,
    private val unit: Float,
    private val ox: Float,
    private val oy: Float,
    private val k: Float,
) {
    private fun p(x: Float, y: Float) = Offset((ox + x * k) * unit, (oy + y * k) * unit)
    private fun len(v: Float) = v * k * unit

    private fun oval(color: Color, cx: Float, cy: Float, rx: Float, ry: Float) {
        val c = p(cx, cy)
        scope.drawOval(color, topLeft = Offset(c.x - len(rx), c.y - len(ry)), size = Size(len(rx) * 2, len(ry) * 2))
    }

    private fun path(block: Path.() -> Unit) = Path().apply(block)
    private fun Path.move(x: Float, y: Float) = p(x, y).let { moveTo(it.x, it.y) }
    private fun Path.line(x: Float, y: Float) = p(x, y).let { lineTo(it.x, it.y) }
    private fun Path.quad(cx: Float, cy: Float, x: Float, y: Float) {
        val c = p(cx, cy)
        val e = p(x, y)
        @Suppress("DEPRECATION")
        quadraticBezierTo(c.x, c.y, e.x, e.y)
    }

    private fun strokePath(color: Color, width: Float, block: Path.() -> Unit) =
        scope.drawPath(path(block), color, style = Stroke(width = len(width), cap = StrokeCap.Round))

    fun draw(band: Color, face: MascotFace) {
        // 머리띠 꼬리
        scope.drawPath(path { move(80f, 46f); line(97f, 35f); line(93f, 47f); close() }, band)
        scope.drawPath(path { move(80f, 48f); line(95f, 54f); line(88f, 58f); close() }, band)
        // 몸
        oval(Body, 50f, 56f, 34f, 31f)
        // 머리띠
        strokePath(band, 8f) { move(19f, 47f); quad(50f, 29f, 81f, 47f) }

        when (face) {
            MascotFace.Joy -> strokePath(Ink, 3f) {
                move(35f, 62f); quad(40f, 55f, 45f, 62f)
                move(55f, 62f); quad(60f, 55f, 65f, 62f)
            }
            MascotFace.Sleepy -> strokePath(Ink, 3.2f) {
                move(36f, 63f); line(44f, 63f)
                move(56f, 63f); line(64f, 63f)
            }
            else -> {
                oval(Ink, 40f, 61f, 4f, 5.6f)
                oval(Ink, 60f, 61f, 4f, 5.6f)
                if (face != MascotFace.Plain) {
                    scope.drawCircle(Body, radius = len(1.3f), center = p(41.3f, 59.2f))
                    scope.drawCircle(Body, radius = len(1.3f), center = p(61.3f, 59.2f))
                }
            }
        }
        if (face != MascotFace.Plain && face != MascotFace.Sleepy) {
            oval(Cheek, 31f, 70f, 5f, 3f)
            oval(Cheek, 69f, 70f, 5f, 3f)
        }
        when (face) {
            MascotFace.Open -> scope.drawPath(path { move(44f, 67f); quad(50f, 77f, 56f, 67f); close() }, Ink)
            MascotFace.Joy -> scope.drawPath(path { move(43f, 67f); quad(50f, 79f, 57f, 67f); close() }, Ink)
            MascotFace.Sleepy -> strokePath(Ink, 3f) { move(45f, 71f); quad(50f, 68f, 55f, 71f) }
            else -> strokePath(Ink, 2.8f) { move(45f, 69f); quad(50f, 75f, 55f, 69f) }
        }
    }
}

@Preview
@Composable
private fun MascotPreview() {
    TdrTheme {
        androidx.compose.foundation.layout.Row {
            Mascot(size = 64.dp)
            Mascot(size = 64.dp, frame = MascotFrame.Rounded, background = TeamColor.BLUE.color, face = MascotFace.Open)
            Mascot(size = 64.dp, background = TeamColor.ORANGE.color, bandColor = TeamColor.ORANGE.bandColor, face = MascotFace.Plain)
            Mascot(size = 64.dp, background = TeamColor.PURPLE.color, face = MascotFace.Sleepy)
            Mascot(size = 64.dp, frame = MascotFrame.None, face = MascotFace.Joy)
        }
    }
}
