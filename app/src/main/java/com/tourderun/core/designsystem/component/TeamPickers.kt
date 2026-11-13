package com.tourderun.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

private val SwatchShape = RoundedCornerShape(10.dp)

/** 선택 표시: 바탕색 2dp 틈 + 바깥 2dp 링. */
private fun Modifier.selectionRing(on: Boolean, ring: Color, gap: Color): Modifier =
    if (on) this.border(2.dp, ring, SwatchShape).padding(2.dp).border(2.dp, gap, SwatchShape).padding(2.dp) else this

/** 팀 색 8칸 한 줄. */
@Composable
fun TeamColorPicker(
    selected: TeamColor,
    onSelect: (TeamColor) -> Unit,
    modifier: Modifier = Modifier,
    gapColor: Color = TdrTheme.colors.bgSurface,
) {
    Row(modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TeamColor.entries.forEach { color ->
            val on = color == selected
            val label = stringResource(color.labelRes)
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .selectionRing(on, TdrTheme.colors.ink, gapColor)
                    .clip(SwatchShape)
                    .background(color.color)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(color) })
                    .semantics { contentDescription = label },
            )
        }
    }
}

/** 팀 아이콘 8칸 한 줄 (팀 만들기 모달). 선택된 칸은 팀 색으로 칠한다. */
@Composable
fun TeamIconPicker(
    selected: TeamIcon,
    color: TeamColor,
    onSelect: (TeamIcon) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TeamIcon.entries.forEach { icon ->
            val on = icon == selected
            val label = stringResource(icon.labelRes)
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .selectionRing(on, color.color, TdrTheme.colors.bgSurface)
                    .clip(SwatchShape)
                    .background(if (on) color.color else TdrTheme.colors.bgSubtle)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(icon) })
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon.vector,
                    contentDescription = null,
                    tint = if (on) Color.White else TdrTheme.colors.inkMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** 팀 아이콘 4×2 큰 칸 (팀 정보 수정). 칸마다 팀 색 미리보기 + 이름. */
@Composable
fun TeamIconGrid(
    selected: TeamIcon,
    color: TeamColor,
    onSelect: (TeamIcon) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
        TeamIcon.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
                row.forEach { icon ->
                    val on = icon == selected
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(TdrShapes.lg)
                            .background(if (on) TdrTheme.colors.brandSoft else TdrTheme.colors.bgSurface)
                            .then(if (on) Modifier.border(2.dp, TdrTheme.colors.brand, TdrShapes.lg) else Modifier)
                            .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(icon) })
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TeamImage(TeamAppearance(color, icon), size = 32.dp)
                        Text(stringResource(icon.labelRes), style = TdrTheme.typography.micro, color = TdrTheme.colors.ink)
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun TeamPickersPreview() {
    TdrTheme {
        Column(
            Modifier.background(TdrTheme.colors.bgSurface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TeamIconPicker(TeamIcon.RUNNER, TeamColor.BLUE, {})
            TeamColorPicker(TeamColor.BLUE, {})
            TeamIconGrid(TeamIcon.STAR, TeamColor.GREEN, {})
        }
    }
}
