package com.tourderun.feature.team.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.annotation.StringRes
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
    @StringRes val errorRes: Int? = null,
    val isSaving: Boolean = false,
) {
    val canSave: Boolean get() = name.trim().length in 2..CreateTeamUiState.TEAM_NAME_MAX && !isSaving
}

/**
 * S14 팀 정보 수정 (TEAM-05, 팀장만). 상태는 [TeamEditViewModel]. 저장하면 [onSaved]로 S14에 돌아간다.
 * 지금 팀 값을 읽는 동안은 스피너.
 */
@Composable
fun TeamEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeamEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                TeamEditEvent.Saved -> onSaved()
            }
        }
    }
    val state = uiState
    if (state == null) {
        // 읽는 중에도 상단 바는 먼저 그려 뒤로 가기 버튼으로 나갈 수 있게 한다 (#120 리뷰)
        Column(
            modifier
                .fillMaxSize()
                .background(TdrTheme.colors.bgCanvas)
                .statusBarsPadding()
                .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3),
        ) {
            TdrTopBar(title = stringResource(R.string.team_edit_title), onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TdrTheme.colors.brand)
            }
        }
        return
    }
    TeamEditContent(
        uiState = state,
        onBack = onBack,
        onSave = { viewModel.onAction(TeamEditAction.Save) },
        onNameChange = { viewModel.onAction(TeamEditAction.NameChanged(it)) },
        onColorSelect = { viewModel.onAction(TeamEditAction.ColorSelected(it)) },
        onIconSelect = { viewModel.onAction(TeamEditAction.IconSelected(it)) },
        modifier = modifier,
    )
}

/** S14 팀 정보 수정 그리기만 한다 (상태 없음, Preview 대상). */
@Composable
fun TeamEditContent(
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
            error = uiState.errorRes?.let { stringResource(it) },
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
private fun TeamEditContentPreview() {
    TdrTheme {
        TeamEditContent(TeamEditUiState("뛰뛰크루", TeamColor.BLUE, TeamIcon.RUNNER), {}, {}, {}, {}, {})
    }
}

@Preview(widthDp = 390, heightDp = 844, name = "이름이 짧아 저장 불가")
@Composable
private fun TeamEditContentInvalidPreview() {
    TdrTheme {
        TeamEditContent(TeamEditUiState("뛰", TeamColor.ORANGE, TeamIcon.FLAME), {}, {}, {}, {}, {})
    }
}
