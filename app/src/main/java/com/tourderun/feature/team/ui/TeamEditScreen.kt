package com.tourderun.feature.team.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tourderun.R
import com.tourderun.core.designsystem.component.NoticeCard
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.component.TdrTextField
import com.tourderun.core.designsystem.component.TdrTopBar
import com.tourderun.core.designsystem.component.TdrTopBarTextAction
import com.tourderun.core.designsystem.component.TeamAppearance
import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.designsystem.component.TeamColorPicker
import com.tourderun.core.designsystem.component.TeamIcon
import com.tourderun.core.designsystem.component.TeamIconGrid
import com.tourderun.core.designsystem.component.TeamImage
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

/** S14 팀 이름·대표 이미지 수정 화면 상태 (팀장만). */
data class TeamEditUiState(
    val name: String,
    val color: TeamColor,
    val icon: TeamIcon,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
) {
    val canSave: Boolean get() = name.trim().length in 2..CreateTeamUiState.TEAM_NAME_MAX && !isSaving
}

/** S14 팀 정보 수정. */
@Composable
fun TeamEditScreen(
    uiState: TeamEditUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onColorSelect: (TeamColor) -> Unit,
    onIconSelect: (TeamIcon) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3, bottom = TdrSpacing.s8),
        verticalArrangement = Arrangement.spacedBy(TdrSpacing.s4),
    ) {
        TdrTopBar(title = stringResource(R.string.team_edit_title), onBack = onBack) {
            TdrTopBarTextAction(stringResource(R.string.action_save), onSave, enabled = uiState.canSave)
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            val shape = RoundedCornerShape(32.dp)
            TeamImage(
                TeamAppearance(uiState.color, uiState.icon),
                size = 128.dp,
                contentDescription = stringResource(R.string.team_edit_preview_desc),
                modifier = Modifier
                    .shadow(16.dp, shape)
                    .border(4.dp, colors.bgSurface, shape),
            )
        }
        TdrTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.team_name_label),
            error = uiState.errorMessage,
            maxLength = CreateTeamUiState.TEAM_NAME_MAX,
        )
        Column(verticalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
            Text(stringResource(R.string.team_color_label), style = type.label)
            TeamColorPicker(selected = uiState.color, onSelect = onColorSelect, gapColor = colors.bgCanvas)
        }
        Column(verticalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
            Text(stringResource(R.string.team_icon_label), style = type.label)
            TeamIconGrid(selected = uiState.icon, color = uiState.color, onSelect = onIconSelect)
        }
        NoticeCard(stringResource(R.string.team_edit_notice), icon = TdrIcons.Lock, iconTint = colors.inkMuted)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun TeamEditScreenPreview() {
    TdrTheme {
        TeamEditScreen(TeamEditUiState("뛰뛰크루", TeamColor.BLUE, TeamIcon.RUNNER), {}, {}, {}, {}, {})
    }
}
