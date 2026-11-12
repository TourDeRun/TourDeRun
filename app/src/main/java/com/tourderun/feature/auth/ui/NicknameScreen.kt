package com.tourderun.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tourderun.R
import com.tourderun.core.designsystem.component.Mascot
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrTextField
import com.tourderun.core.designsystem.component.TdrTopBar
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

/**
 * S02 닉네임 설정 화면 상태.
 * @property errorMessage 서버 검증 실패 등 ViewModel이 정한 문구. null이면 도움말을 보인다.
 */
data class NicknameUiState(
    val nickname: String = "",
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
) {
    /** 2~10자, 한글·영문·숫자만 (공백 불가). */
    val isValid: Boolean get() = NICKNAME_REGEX.matches(nickname)

    companion object {
        const val MAX_LENGTH = 10
        private val NICKNAME_REGEX = Regex("^[가-힣a-zA-Z0-9]{2,10}$")
    }
}

/** S02 닉네임 설정. */
@Composable
fun NicknameScreen(
    uiState: NicknameUiState,
    onBack: () -> Unit,
    onNicknameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bgSurface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3, bottom = TdrSpacing.s8),
    ) {
        TdrTopBar(onBack = onBack) {
            Text(stringResource(R.string.nickname_step), style = type.caption, color = colors.inkMuted)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 36.dp),
            verticalArrangement = Arrangement.spacedBy(TdrSpacing.s6),
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TdrSpacing.s4),
            ) {
                Mascot(size = 96.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.nickname_title), style = type.titleLg)
                    Text(stringResource(R.string.nickname_subtitle), style = type.bodySm, color = colors.inkMuted)
                }
            }
            TdrTextField(
                value = uiState.nickname,
                onValueChange = onNicknameChange,
                label = stringResource(R.string.nickname_label),
                placeholder = stringResource(R.string.nickname_placeholder),
                helper = stringResource(R.string.nickname_helper),
                error = uiState.errorMessage,
                maxLength = NicknameUiState.MAX_LENGTH,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        }
        Spacer(Modifier.height(TdrSpacing.s4))
        TdrButton(
            text = stringResource(R.string.nickname_start),
            onClick = onSubmit,
            enabled = uiState.isValid && !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun NicknameScreenPreview() {
    TdrTheme { NicknameScreen(NicknameUiState(nickname = "민지"), {}, {}, {}) }
}
