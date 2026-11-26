package com.tourderun.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tourderun.R
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrTheme

/**
 * 되돌리기 어려운 동작 앞의 확인 다이얼로그 (기록 삭제, 팀 나가기, 내보내기, 로그아웃, 회원 탈퇴).
 *
 * @param title 무슨 동작인지 묻는 문장 ("이 기록을 삭제할까요?")
 * @param confirmText 동작 이름 ("삭제"). "확인"을 쓰지 않는다
 * @param body 결과 설명. 없으면 생략
 * @param danger 되돌릴 수 없는 동작이면 확인 글자를 빨갛게
 * @param dismissText 왼쪽 버튼 글자. 기본 "취소"
 * @param onDismissClick 왼쪽 버튼 동작. 기본은 [onDismiss](바깥 누르기·뒤로 가기와 같다)
 */
@Composable
fun TdrConfirmDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    danger: Boolean = false,
    dismissText: String = stringResource(R.string.action_cancel),
    onDismissClick: () -> Unit = onDismiss,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title, style = type.title) },
        text = body?.let { { Text(it, style = type.body, color = colors.inkMuted) } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = if (danger) colors.danger else colors.brand, style = type.subtitle)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissClick) {
                Text(dismissText, color = colors.ink, style = type.subtitle)
            }
        },
        containerColor = colors.bgSurface,
        shape = TdrShapes.lg,
    )
}

@Preview
@Composable
private fun TdrConfirmDialogPreview() {
    TdrTheme {
        TdrConfirmDialog(
            title = "이 기록을 삭제할까요?",
            confirmText = "삭제",
            onConfirm = {},
            onDismiss = {},
            body = "삭제하면 되돌릴 수 없어요. 팀 거리는 줄지 않아요.",
            danger = true,
        )
    }
}
