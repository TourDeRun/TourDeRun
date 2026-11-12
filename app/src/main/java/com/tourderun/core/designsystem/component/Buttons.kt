package com.tourderun.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSize
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

enum class TdrButtonStyle {
    /** 파란 주 버튼. */
    Primary,
    /** 초대 코드로 참가 (청록). */
    Join,
    /** 흰 바탕 + 회색 테두리. */
    Outline,
    /** 흰 바탕 + 테두리 + 빨간 글자 (삭제·팀 나가기). */
    OutlineDanger,
    /** 옅은 파랑 바탕 + 파란 글자 (공유하기 등 작은 보조 버튼). */
    Soft,
}

/**
 * 와이어프레임의 버튼. 기본은 화면 폭 꽉 찬 52dp.
 * [enabled]가 false면 회색 바탕에 [text]를 보여준다 (예: "필수 항목에 모두 동의해 주세요").
 */
@Composable
fun TdrButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TdrButtonStyle = TdrButtonStyle.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = TdrSize.buttonLg,
) {
    val colors = TdrTheme.colors
    val (container, content) = when {
        !enabled -> colors.bgSubtle to colors.inkMuted
        style == TdrButtonStyle.Primary -> colors.brand to colors.inkOnBrand
        style == TdrButtonStyle.Join -> colors.actionJoin to colors.inkOnBrand
        style == TdrButtonStyle.Outline -> colors.bgSurface to colors.ink
        style == TdrButtonStyle.OutlineDanger -> colors.bgSurface to colors.danger
        else -> colors.brandSoft to colors.brand
    }
    val border = if (enabled && (style == TdrButtonStyle.Outline || style == TdrButtonStyle.OutlineDanger)) {
        BorderStroke(1.dp, colors.borderControl)
    } else {
        null
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = TdrShapes.md,
        color = container,
        contentColor = content,
        border = border,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TdrSpacing.s4),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(TdrSpacing.s2))
            }
            Text(text, style = TdrTheme.typography.subtitle)
        }
    }
}

/** 상단바·모달용 44dp 아이콘 버튼. */
@Composable
fun TdrIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TdrTheme.colors.ink,
    iconSize: Dp = 22.dp,
) {
    IconButton(onClick = onClick, modifier = modifier.size(TdrSize.touch)) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Preview(widthDp = 360)
@Composable
private fun TdrButtonPreview() {
    TdrTheme {
        androidx.compose.foundation.layout.Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TdrButton("새 팀 만들기", {}, Modifier.fillMaxWidth(), icon = TdrIcons.Plus)
            TdrButton("팀 참가하기", {}, Modifier.fillMaxWidth(), style = TdrButtonStyle.Outline, icon = TdrIcons.Enter)
            TdrButton("참가하기", {}, Modifier.fillMaxWidth(), style = TdrButtonStyle.Join)
            TdrButton("팀 나가기", {}, Modifier.fillMaxWidth(), style = TdrButtonStyle.OutlineDanger)
            TdrButton("필수 항목에 모두 동의해 주세요", {}, Modifier.fillMaxWidth(), enabled = false)
        }
    }
}
