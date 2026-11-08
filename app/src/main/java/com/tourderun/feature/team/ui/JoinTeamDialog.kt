package com.tourderun.feature.team.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tourderun.R
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrButtonStyle
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.component.TdrTextField
import com.tourderun.core.designsystem.component.TeamAppearance
import com.tourderun.core.designsystem.component.TeamImage
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSize
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

/** 올바른 코드를 넣었을 때 보이는 팀 미리보기. */
data class JoinTeamPreviewUi(
    val name: String,
    val appearance: TeamAppearance,
    val memberCount: Int,
    val maxMembers: Int = 4,
    val stageNumber: Int,
    val fromCity: String,
    val toCity: String,
)

/** S05 초대 코드 입력 모달 상태. */
data class JoinTeamUiState(
    val code: String = "",
    val preview: JoinTeamPreviewUi? = null,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
) {
    val canJoin: Boolean get() = code.length == CODE_LENGTH && preview != null && !isSubmitting

    companion object {
        const val CODE_LENGTH = 6
    }
}

/** S05 초대 코드로 참가 (팀 시작 화면 위 모달). 참가하면 홈으로 간다. */
@Composable
fun JoinTeamDialog(
    uiState: JoinTeamUiState,
    onDismiss: () -> Unit,
    onCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
) {
    TeamDialog(onDismissRequest = onDismiss) {
        JoinTeamContent(uiState, onDismiss, onCodeChange, onJoin)
    }
}

@Composable
internal fun JoinTeamContent(
    uiState: JoinTeamUiState,
    onDismiss: () -> Unit,
    onCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    TeamModalCard(
        icon = TdrIcons.Enter,
        iconBackground = colors.actionJoinSoft,
        iconTint = colors.actionJoin,
        title = stringResource(R.string.join_title),
        subtitle = stringResource(R.string.join_subtitle),
        onClose = onDismiss,
    ) {
        TdrTextField(
            value = uiState.code,
            onValueChange = { onCodeChange(it.uppercase()) },
            label = stringResource(R.string.join_code_label),
            error = uiState.errorMessage,
            maxLength = JoinTeamUiState.CODE_LENGTH,
            minHeight = 56.dp,
            textStyle = type.titleLg.copy(fontSize = 24.sp, letterSpacing = 0.3.em),
            textAlign = TextAlign.Center,
            focusColor = colors.actionJoin,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        )
        uiState.preview?.let { team ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(TdrShapes.md)
                    .background(colors.actionJoinSoft)
                    .padding(TdrSpacing.s3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3),
            ) {
                TeamImage(team.appearance, size = 40.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(team.name, style = type.bodySm.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    Text(
                        stringResource(
                            R.string.join_preview_info,
                            team.memberCount,
                            team.maxMembers,
                            team.stageNumber,
                            team.fromCity,
                            team.toCity,
                        ),
                        style = type.caption,
                        color = colors.inkMuted,
                    )
                }
            }
        }
        Text(stringResource(R.string.join_notice), style = type.caption, color = colors.inkMuted)
        TdrButton(
            text = stringResource(R.string.join_submit),
            onClick = onJoin,
            style = TdrButtonStyle.Join,
            enabled = uiState.canJoin,
            height = TdrSize.buttonMd,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun JoinTeamDialogPreview() {
    ModalOverTeamStartPreview {
        JoinTeamContent(
            JoinTeamUiState(
                code = "4K7Q2M",
                preview = JoinTeamPreviewUi("뛰뛰크루", TeamAppearance(), 3, 4, 3, "오산", "천안"),
            ),
            {}, {}, {},
        )
    }
}
