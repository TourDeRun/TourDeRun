package com.tourderun.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tourderun.R

private val Navy = Color(0xFF0E2A5C)
private val Blue = Color(0xFF1D5FE0)

/**
 * 팀 색 8가지 (팀 문서 color 필드 값 = [id]).
 * @property bandColor 이 색 위에 그리는 마스코트 머리띠 색.
 */
enum class TeamColor(val id: String, @StringRes val labelRes: Int, val color: Color, val bandColor: Color) {
    BLUE("blue", R.string.team_color_blue, Blue, Navy),
    NAVY("navy", R.string.team_color_navy, Navy, Blue),
    TEAL("teal", R.string.team_color_teal, Color(0xFF0E8F9E), Navy),
    GREEN("green", R.string.team_color_green, Color(0xFF13855A), Navy),
    PURPLE("purple", R.string.team_color_purple, Color(0xFF7A4FD6), Navy),
    ORANGE("orange", R.string.team_color_orange, Color(0xFFE8590C), Blue),
    PINK("pink", R.string.team_color_pink, Color(0xFFC2255C), Navy),
    GRAY("gray", R.string.team_color_gray, Color(0xFF495057), Blue),
    ;

    /** 밝은 바탕 위 글자로 쓸 때 색. 주황은 대비(4.5:1)를 맞추려 진하게 쓴다. */
    val textColor: Color get() = if (this == ORANGE) Color(0xFFB5470B) else color

    companion object {
        fun fromId(id: String?): TeamColor = entries.firstOrNull { it.id == id } ?: BLUE
    }
}

/** 팀 대표 아이콘 8가지 (팀 문서 icon 필드 값 = [id]). [RUNNER]는 마스코트로 그린다. */
enum class TeamIcon(val id: String, @StringRes val labelRes: Int, pathData: String) {
    RUNNER("runner", R.string.team_icon_runner, "M12 5a8 7.5 0 1 0 0 15 8 7.5 0 0 0 0-15z"),
    BOLT("bolt", R.string.team_icon_bolt, "M13 2L5 14h6l-1 8 8-12h-6z"),
    MOUNTAIN("mountain", R.string.team_icon_mountain, "M2 20l7-12 4 6 3-4 6 10z"),
    STAR("star", R.string.team_icon_star, "M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z"),
    FLAME("flame", R.string.team_icon_flame, "M12 2c1 4 5 5.5 5 11a5 5 0 0 1-10 0c0-2.5 1.5-4 2.5-5 0 2 1 3 2 3 0-3-1-6 .5-9z"),
    WAVE("wave", R.string.team_icon_wave, "M2 15c2.5-3 5-3 7 0s4.5 3 7 0 4.5-3 6 0v5H2z"),
    HEART("heart", R.string.team_icon_heart, "M12 20s-8-5-8-11a4.5 4.5 0 0 1 8-2.5A4.5 4.5 0 0 1 20 9c0 6-8 11-8 11z"),
    FLAG("flag", R.string.team_icon_flag, "M6 21V3h11l-2 4.5 2 4.5H8v9z"),
    ;

    val vector: ImageVector by lazy { fill("TeamIcon.$id", pathData) }

    companion object {
        fun fromId(id: String?): TeamIcon = entries.firstOrNull { it.id == id } ?: RUNNER
    }
}

/** 팀의 대표 이미지 = 색 + 아이콘. 사진 업로드는 없다. */
@Immutable
data class TeamAppearance(
    val color: TeamColor = TeamColor.BLUE,
    val icon: TeamIcon = TeamIcon.RUNNER,
)

/**
 * 팀 대표 이미지. 둥근 사각형(기본) 또는 원.
 * 아이콘이 [TeamIcon.RUNNER]면 마스코트를, 아니면 흰 아이콘을 그린다.
 */
@Composable
fun TeamImage(
    appearance: TeamAppearance,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    circle: Boolean = false,
    face: MascotFace = MascotFace.Plain,
    contentDescription: String? = null,
) {
    if (appearance.icon == TeamIcon.RUNNER) {
        Mascot(
            modifier = modifier,
            size = size,
            frame = if (circle) MascotFrame.Circle else MascotFrame.Rounded,
            background = appearance.color.color,
            bandColor = appearance.color.bandColor,
            face = face,
            contentDescription = contentDescription,
        )
    } else {
        val shape = if (circle) CircleShape else RoundedCornerShape(size * 0.24f)
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(appearance.color.color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = appearance.icon.vector,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(size * 0.45f),
            )
        }
    }
}

/** 팀원 아바타 (팀 색 원 + 마스코트). */
@Composable
fun MemberAvatar(
    color: TeamColor,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    face: MascotFace = MascotFace.Plain,
) {
    Mascot(
        modifier = modifier,
        size = size,
        frame = MascotFrame.Circle,
        background = color.color,
        bandColor = color.bandColor,
        face = face,
    )
}
