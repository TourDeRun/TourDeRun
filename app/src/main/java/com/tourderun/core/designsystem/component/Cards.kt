package com.tourderun.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

/**
 * 카드 (radius-lg, 아주 옅은 그림자). [onClick]이 있으면 카드 전체가 눌린다.
 */
@Composable
fun TdrCard(
    modifier: Modifier = Modifier,
    color: Color = TdrTheme.colors.bgSurface,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(TdrSpacing.s4),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(TdrSpacing.s3),
    content: @Composable ColumnScope.() -> Unit,
) {
    val elevated = color == TdrTheme.colors.bgSurface && !TdrTheme.colors.isDark
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (elevated) Modifier.shadow(1.dp, TdrShapes.lg, clip = false) else Modifier)
            .clip(TdrShapes.lg)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * 진행 바. [secondary]가 있으면 그 위치까지 [secondaryColor]로 먼저 칠한다 (러닝 결과: 이번 러닝으로 늘어난 부분).
 */
@Composable
fun TdrProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = TdrTheme.colors.brandSoft,
    fillColor: Color = TdrTheme.colors.brand,
    secondary: Float? = null,
    secondaryColor: Color = TdrTheme.colors.brandLight,
    contentDescription: String? = null,
) {
    val value = progress.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(TdrShapes.full)
            .background(trackColor)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(maxOf(value, secondary ?: 0f), 0f..1f)
                if (contentDescription != null) this.contentDescription = contentDescription
            },
    ) {
        if (secondary != null) {
            Box(
                Modifier
                    .fillMaxWidth(secondary.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(TdrShapes.full)
                    .background(secondaryColor),
            )
        }
        Box(
            Modifier
                .fillMaxWidth(value)
                .fillMaxHeight()
                .clip(TdrShapes.full)
                .background(fillColor),
        )
    }
}

/** 숫자 + 작은 단위 ("3.24km"). */
fun valueWithUnit(value: String, unit: String?, unitSize: Int = 12, unitColor: Color = Color.Unspecified): AnnotatedString =
    buildAnnotatedString {
        append(value)
        if (unit != null) {
            withStyle(SpanStyle(fontSize = unitSize.sp, fontWeight = FontWeight.SemiBold, color = unitColor)) { append(unit) }
        }
    }

/** 가운데 정렬 숫자 + 설명 (거리 / 시간 / 평균 페이스). */
@Composable
fun StatColumn(
    value: AnnotatedString,
    label: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = TdrTheme.typography.statMd,
    valueColor: Color = Color.Unspecified,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = valueStyle, color = valueColor, textAlign = TextAlign.Center, maxLines = 1)
        Text(label, style = TdrTheme.typography.micro, color = TdrTheme.colors.inkMuted)
    }
}

/** 3칸 숫자 줄. */
@Composable
fun StatRow(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s2), content = content)
}

/** 디자인 시스템 StatCard: 라벨 · 큰 숫자 + 단위 · 설명. */
@Composable
fun TdrStatCard(
    label: String,
    value: String,
    unit: String?,
    caption: String?,
    modifier: Modifier = Modifier,
) {
    TdrCard(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = TdrTheme.typography.caption, color = TdrTheme.colors.inkMuted)
        Text(
            valueWithUnit(value, unit?.let { " $it" }, unitSize = 13, unitColor = TdrTheme.colors.inkMuted),
            style = TdrTheme.typography.statLg,
        )
        if (caption != null) {
            Text(caption, style = TdrTheme.typography.caption, color = TdrTheme.colors.inkMuted)
        }
    }
}

/** 카드 머리: 왼쪽 제목, 오른쪽 보조 글자. */
@Composable
fun CardHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = TdrTheme.typography.subtitle, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = TdrTheme.typography.caption, color = TdrTheme.colors.inkMuted)
        }
    }
}

/** 옅은 색 정사각 아이콘 칸 (모달 머리, 팀 시작 화면). */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    background: Color = TdrTheme.colors.brandSoft,
    tint: Color = TdrTheme.colors.brand,
) {
    Box(
        modifier
            .size(size)
            .clip(if (size > 56.dp) TdrShapes.lg else TdrShapes.md)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.45f))
    }
}

/** 날짜 칸 ("11월" / "6"). 내 기록 목록·프로필. */
@Composable
fun DateTile(month: String, day: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Column(
        modifier
            .size(size)
            .clip(TdrShapes.md)
            .background(TdrTheme.colors.brandSoft),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(month, style = TdrTheme.typography.micro.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = TdrTheme.colors.brand)
        Text(day, style = TdrTheme.typography.topBar.copy(fontWeight = FontWeight.ExtraBold), color = TdrTheme.colors.brand)
    }
}

/** 안내 문구 카드 (정보 아이콘 + 작은 글자). */
@Composable
fun NoticeCard(text: String, modifier: Modifier = Modifier, icon: ImageVector = TdrIcons.Info, iconTint: Color = TdrTheme.colors.brand) {
    TdrCard(modifier, contentPadding = PaddingValues(horizontal = 14.dp, vertical = TdrSpacing.s3)) {
        Row(horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.padding(top = 1.dp).size(16.dp))
            Text(text, style = TdrTheme.typography.caption.copy(lineHeight = 18.sp), color = TdrTheme.colors.inkMuted)
        }
    }
}

/** 둥근 알약 라벨. */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = TdrTheme.colors.brandSoft,
    contentColor: Color = TdrTheme.colors.brand,
    style: TextStyle = TdrTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
) {
    Text(
        text,
        style = style,
        color = contentColor,
        modifier = modifier
            .clip(TdrShapes.full)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
