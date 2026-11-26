package com.tourderun.feature.profile.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tourderun.R
import com.tourderun.core.designsystem.component.TdrTextField
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrTheme

/** AUTH-03 닉네임 수정 창 (S13 연필 버튼). 규칙에 맞을 때만 저장 버튼이 켜진다. */
@Composable
fun NicknameEditDialog(
    state: NicknameEditState,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_nickname), style = type.title) },
        text = {
            TdrTextField(
                value = state.value,
                onValueChange = onValueChange,
                placeholder = stringResource(R.string.nickname_placeholder),
                helper = stringResource(R.string.nickname_helper),
                error = state.errorRes?.let { stringResource(it) },
            )
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = state.isValid) {
                Text(
                    stringResource(R.string.action_save),
                    color = if (state.isValid) colors.brand else colors.inkMuted,
                    style = type.subtitle,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = colors.ink, style = type.subtitle)
            }
        },
        containerColor = colors.bgSurface,
        shape = TdrShapes.lg,
    )
}

@Preview
@Composable
private fun NicknameEditDialogPreview() {
    TdrTheme { NicknameEditDialog(NicknameEditState("민찬"), {}, {}, {}) }
}

@Preview(name = "규칙에 안 맞음")
@Composable
private fun NicknameEditDialogInvalidPreview() {
    TdrTheme { NicknameEditDialog(NicknameEditState("민"), {}, {}, {}) }
}
