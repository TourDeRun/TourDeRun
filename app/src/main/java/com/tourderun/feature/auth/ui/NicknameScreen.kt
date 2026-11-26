package com.tourderun.feature.auth.ui

import androidx.annotation.StringRes
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tourderun.R
import com.tourderun.core.designsystem.component.Mascot
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrTextField
import com.tourderun.core.designsystem.component.TdrTopBar
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.domain.team.NICKNAME_MAX_LENGTH
import com.tourderun.domain.team.validateNickname

/**
 * S02 닉네임 설정 화면 상태.
 * @property errorRes 저장 실패 등 ViewModel이 정한 문구의 리소스 ID. null이면 도움말을 보인다.
 */
data class NicknameUiState(
    val nickname: String = "",
    @StringRes val errorRes: Int? = null,
    val isSubmitting: Boolean = false,
) {
    /** 2~10자, 한글·영문·숫자만. 앞뒤 공백은 저장할 때 지우므로 검사에서도 뺀다 (규칙은 :domain [validateNickname]). */
    val isValid: Boolean get() = validateNickname(nickname.trim()) == null

    companion object {
        const val MAX_LENGTH = NICKNAME_MAX_LENGTH
    }
}

/** S02 닉네임 설정. 저장 뒤 이동은 EntryGraph가 세션 상태를 보고 한다. */
@Composable
fun NicknameScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NicknameViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NicknameContent(
        uiState = uiState,
        onBack = onBack,
        onNicknameChange = { viewModel.onAction(NicknameAction.NicknameChanged(it)) },
        onSubmit = { viewModel.onAction(NicknameAction.Submit) },
        modifier = modifier,
    )
}

/** S02 닉네임 설정 화면 그리기만 한다 (상태 없음, Preview 대상). */
@Composable
fun NicknameContent(
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TdrSpacing.s15)) {
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
                error = uiState.errorRes?.let { stringResource(it) },
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
private fun NicknameContentPreview() {
    TdrTheme { NicknameContent(NicknameUiState(nickname = "민지"), {}, {}, {}) }
}

/** 빈 상태: 아직 입력 전. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun NicknameScreenEmptyPreview() {
    TdrTheme { NicknameContent(NicknameUiState(), {}, {}, {}) }
}
