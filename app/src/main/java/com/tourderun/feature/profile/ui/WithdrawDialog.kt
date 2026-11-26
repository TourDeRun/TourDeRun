package com.tourderun.feature.profile.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tourderun.R
import com.tourderun.core.data.session.GoogleCredentialClient
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import kotlinx.coroutines.launch

/**
 * S13 회원 탈퇴 흐름 (AUTH-05). 확인 → 구글 재인증 창 → 탈퇴.
 * 재인증 창은 Activity가 필요해서 여기(화면)가 띄우고 결과만 [viewModel]에 넘긴다(S01 로그인과 같은 방식).
 */
@Composable
fun WithdrawFlow(viewModel: WithdrawViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // RUN-02: 러닝 중이라 막혔을 때 안내
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    if (!uiState.showConfirm) return
    val scope = rememberCoroutineScope()
    val client = remember { GoogleCredentialClient() }
    WithdrawDialog(
        uiState = uiState,
        onConfirm = {
            viewModel.onAction(WithdrawAction.ReauthStarted)
            scope.launch {
                viewModel.onAction(WithdrawAction.CredentialReceived(client.requestIdToken(context)))
            }
        },
        onDismiss = { viewModel.onAction(WithdrawAction.Dismiss) },
    )
}

/** 탈퇴 확인 창 그리기만 한다 (상태 없음). 무엇이 지워지고 무엇이 남는지 알린다. */
@Composable
fun WithdrawDialog(uiState: WithdrawUiState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.withdraw_title), style = type.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
                Text(stringResource(R.string.withdraw_body), style = type.body, color = colors.inkMuted)
                if (uiState.isWorking) LinearProgressIndicator(color = colors.danger, modifier = Modifier.fillMaxWidth())
                uiState.errorRes?.let { Text(stringResource(it), style = type.caption, color = colors.danger) }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !uiState.isWorking) {
                Text(
                    stringResource(R.string.withdraw_confirm),
                    color = if (uiState.isWorking) colors.inkMuted else colors.danger,
                    style = type.subtitle,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isWorking) {
                Text(stringResource(R.string.action_cancel), color = colors.ink, style = type.subtitle)
            }
        },
        containerColor = colors.bgSurface,
        shape = TdrShapes.lg,
    )
}

@Preview
@Composable
private fun WithdrawDialogPreview() {
    TdrTheme { WithdrawDialog(WithdrawUiState(showConfirm = true), {}, {}) }
}

@Preview(name = "진행 중·실패")
@Composable
private fun WithdrawDialogWorkingPreview() {
    TdrTheme {
        WithdrawDialog(WithdrawUiState(showConfirm = true, isWorking = true, errorRes = R.string.withdraw_error_network), {}, {})
    }
}
