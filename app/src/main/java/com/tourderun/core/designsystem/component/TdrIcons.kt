package com.tourderun.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 와이어프레임의 선 아이콘 (24×24 viewBox, 둥근 끝).
 * `Icon(TdrIcons.Back, ...)`처럼 쓰고 색은 tint(기본 LocalContentColor)로 정한다.
 */
object TdrIcons {
    val Back: ImageVector by lazy { stroke("Back", "M15 5l-7 7 7 7") }
    val Close: ImageVector by lazy { stroke("Close", "M6 6l12 12M18 6L6 18") }
    val ChevronRight: ImageVector by lazy { stroke("ChevronRight", "M9 6l6 6-6 6") }
    val Plus: ImageVector by lazy { stroke("Plus", "M12 5v14M5 12h14", width = 2.2f) }
    val Enter: ImageVector by lazy { stroke("Enter", "M14 4h5v16h-5M4 12h11M11 8l4 4-4 4") }
    val People: ImageVector by lazy {
        stroke("People", "M16 11a4 4 0 1 0-8 0 4 4 0 0 0 8 0zM4 20c1.3-3 4.3-4.5 8-4.5s6.7 1.5 8 4.5")
    }
    val Check: ImageVector by lazy { stroke("Check", "M5 12l5 5 9-10", width = 2.6f) }
    val CheckBold: ImageVector by lazy { stroke("CheckBold", "M5 12l5 5 9-10", width = 3f) }
    val Edit: ImageVector by lazy { stroke("Edit", "M4 20h4L19 9l-4-4L4 16zM13 7l4 4") }
    val Lock: ImageVector by lazy {
        stroke(
            "Lock",
            "M7 11h10a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2zM8 11V8a4 4 0 0 1 8 0v3",
        )
    }
    val Flag: ImageVector by lazy { stroke("Flag", "M6 21V4h11l-2 4 2 4H6") }
    val Info: ImageVector by lazy { stroke("Info", "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM12 11v5M12 8h.01") }
    val BarChart: ImageVector by lazy { stroke("BarChart", "M4 20V10M10 20V4M16 20v-7M22 20H2") }
    val TrendUp: ImageVector by lazy { stroke("TrendUp", "M4 17l6-6 4 4 6-8M15 7h5v5") }
    val ArrowRight: ImageVector by lazy { stroke("ArrowRight", "M4 12h16M14 6l6 6-6 6") }
    val Crown: ImageVector by lazy { fill("Crown", "M3 8l5 4 4-7 4 7 5-4-2 11H5z") }
    val Stop: ImageVector by lazy {
        fill("Stop", "M8 6h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z")
    }
    val Pause: ImageVector by lazy {
        fill(
            "Pause",
            "M7.5 5h1A1.5 1.5 0 0 1 10 6.5v11A1.5 1.5 0 0 1 8.5 19h-1A1.5 1.5 0 0 1 6 17.5v-11A1.5 1.5 0 0 1 7.5 5z" +
                "M15.5 5h1A1.5 1.5 0 0 1 18 6.5v11a1.5 1.5 0 0 1-1.5 1.5h-1a1.5 1.5 0 0 1-1.5-1.5v-11A1.5 1.5 0 0 1 15.5 5z",
        )
    }
    val Play: ImageVector by lazy { fill("Play", "M8 5.5v13a1 1 0 0 0 1.5.9l10-6.5a1 1 0 0 0 0-1.8l-10-6.5A1 1 0 0 0 8 5.5z") }

    // 하단 탭
    val TabHome: ImageVector by lazy {
        stroke("TabHome", "M4 11l8-7 8 7v8a1 1 0 0 1-1 1h-4v-6H9v6H5a1 1 0 0 1-1-1z", width = 1.8f)
    }
    val TabRoad: ImageVector by lazy {
        stroke("TabRoad", "M4 6l5-2 6 2 5-2v14l-5 2-6-2-5 2zM9 4v14M15 6v14", width = 1.8f)
    }
    val TabProfile: ImageVector by lazy {
        stroke(
            "TabProfile",
            "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 20c1.5-3.5 4.5-5 8-5s6.5 1.5 8 5",
            width = 1.8f,
        )
    }
    val Runner: ImageVector by lazy {
        ImageVector.Builder("Runner", 24.dp, 24.dp, 24f, 24f)
            .addPath(
                pathData = addPathNodes("M14 2.5a2 2 0 1 0 0 4 2 2 0 0 0 0-4z"),
                fill = SolidColor(Color.Black),
            )
            .addPath(
                pathData = addPathNodes("M8 21l3-6 3 2v4M7 12l3-4h4l2 4h3M11 8l-1 5"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .build()
    }

    /** 구글 로고 (색이 고정이라 Icon이 아니라 Image로 그린다). */
    val GoogleLogo: ImageVector by lazy {
        ImageVector.Builder("GoogleLogo", 24.dp, 24.dp, 24f, 24f)
            .addPath(
                addPathNodes("M21 12.2c0-.7-.1-1.3-.2-1.9H12v3.6h5.1a4.4 4.4 0 0 1-1.9 2.9v2.4h3.1c1.8-1.7 2.7-4.1 2.7-7z"),
                fill = SolidColor(Color(0xFF1D5FE0)),
            )
            .addPath(
                addPathNodes("M12 21.5c2.6 0 4.7-.9 6.3-2.3l-3.1-2.4c-.9.6-2 .9-3.2.9-2.5 0-4.6-1.7-5.3-3.9H3.5v2.5A9.5 9.5 0 0 0 12 21.5z"),
                fill = SolidColor(Color(0xFF13855A)),
            )
            .addPath(
                addPathNodes("M6.7 13.8a5.7 5.7 0 0 1 0-3.6V7.7H3.5a9.5 9.5 0 0 0 0 8.6z"),
                fill = SolidColor(Color(0xFFE8B10C)),
            )
            .addPath(
                addPathNodes("M12 6.3c1.4 0 2.6.5 3.6 1.4l2.7-2.7A9.5 9.5 0 0 0 3.5 7.7l3.2 2.5C7.4 8 9.5 6.3 12 6.3z"),
                fill = SolidColor(Color(0xFFD6342C)),
            )
            .build()
    }
}

internal fun stroke(name: String, pathData: String, width: Float = 2f): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(
            pathData = addPathNodes(pathData),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()

internal fun fill(name: String, pathData: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
        .build()
