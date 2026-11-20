package com.tourderun.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tourderun.R
import com.tourderun.core.data.DataError
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrButtonStyle
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

/**
 * 실패 종류 → 불러오기 실패 문구. 서버에 닿지 못했으면 연결 확인, 그 밖(권한 거절·인덱스 누락 등)은 처리할 수 없음.
 * 화면은 이 값을 [LoadFailed]에 넘긴다.
 */
@StringRes
fun DataError.loadFailedMessageRes(): Int = when (this) {
    DataError.Unavailable -> R.string.load_failed_unavailable
    DataError.PermissionDenied, DataError.Other -> R.string.load_failed_other
}

/**
 * 불러오기 실패: 문구와 "다시 시도" 버튼 (S15·S16 공용, 홈·로드뷰·S11·S12도 이걸로).
 * 가로를 채우고 위아래 여백을 둔 채 가운데 정렬한다.
 *
 * @param messageRes 보통 [loadFailedMessageRes]의 값
 * @param onRetry 다시 시도. 보통 ViewModel이 리스너를 다시 구독한다
 */
@Composable
fun LoadFailed(
    @StringRes messageRes: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = TdrSpacing.s8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TdrSpacing.s4),
    ) {
        Text(
            stringResource(messageRes),
            style = TdrTheme.typography.body,
            color = TdrTheme.colors.inkMuted,
            textAlign = TextAlign.Center,
        )
        TdrButton(text = stringResource(R.string.action_retry), onClick = onRetry, style = TdrButtonStyle.Outline)
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadFailedUnavailablePreview() {
    TdrTheme { LoadFailed(DataError.Unavailable.loadFailedMessageRes(), onRetry = {}) }
}

@Preview(showBackground = true)
@Composable
private fun LoadFailedOtherPreview() {
    TdrTheme { LoadFailed(DataError.PermissionDenied.loadFailedMessageRes(), onRetry = {}) }
}
