package com.tourderun.feature.team.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tourderun.R
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.component.TdrTextField
import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.designsystem.component.TeamColorPicker
import com.tourderun.core.designsystem.component.TeamIcon
import com.tourderun.core.designsystem.component.TeamIconPicker
import com.tourderun.core.designsystem.theme.TdrSize
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

/** S04 팀 만들기 모달 상태. */
data class CreateTeamUiState(
    val name: String = "",
    val color: TeamColor = TeamColor.BLUE,
    val icon: TeamIcon = TeamIcon.RUNNER,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
) {
    /** 앞뒤 공백을 뺀 2~12자. */
    val isValid: Boolean get() = name.trim().length in 2..TEAM_NAME_MAX

    companion object {
        const val TEAM_NAME_MAX = 12
    }
}

/** S04 팀 만들기 (팀 시작 화면 위 모달). 만들면 초대 코드 화면(S06)으로 간다. */
@Composable
fun CreateTeamDialog(
    uiState: CreateTeamUiState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onColorSelect: (TeamColor) -> Unit,
    onIconSelect: (TeamIcon) -> Unit,
    onSubmit: () -> Unit,
) {
    TeamDialog(onDismissRequest = onDismiss) {
        CreateTeamContent(uiState, onDismiss, onNameChange, onColorSelect, onIconSelect, onSubmit)
    }
}

@Composable
internal fun CreateTeamContent(
    uiState: CreateTeamUiState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onColorSelect: (TeamColor) -> Unit,
    onIconSelect: (TeamIcon) -> Unit,
    onSubmit: () -> Unit,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    TeamModalCard(
        icon = TdrIcons.Plus,
        iconBackground = colors.brandSoft,
        iconTint = colors.brand,
        title = stringResource(R.string.create_team_title),
        subtitle = stringResource(R.string.create_team_subtitle),
        onClose = onDismiss,
    ) {
        TdrTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.team_name_label),
            placeholder = stringResource(R.string.create_team_name_placeholder),
            error = uiState.errorMessage,
            maxLength = CreateTeamUiState.TEAM_NAME_MAX,
        )
        Column(verticalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
            Text(stringResource(R.string.team_icon_label), style = type.label)
            TeamIconPicker(selected = uiState.icon, color = uiState.color, onSelect = onIconSelect)
        }
        Column(verticalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
            Text(stringResource(R.string.team_color_label), style = type.label)
            TeamColorPicker(selected = uiState.color, onSelect = onColorSelect)
            Text(stringResource(R.string.create_team_helper), style = type.caption, color = colors.inkMuted)
        }
        TdrButton(
            text = stringResource(R.string.create_team_submit),
            onClick = onSubmit,
            enabled = uiState.isValid && !uiState.isSubmitting,
            height = TdrSize.buttonMd,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CreateTeamDialogPreview() {
    ModalOverTeamStartPreview {
        CreateTeamContent(CreateTeamUiState(name = "뛰뛰크루"), {}, {}, {}, {}, {})
    }
}
