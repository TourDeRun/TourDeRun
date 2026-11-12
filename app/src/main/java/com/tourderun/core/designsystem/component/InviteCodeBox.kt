package com.tourderun.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tourderun.R
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSize
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

/** 초대 코드 6칸 + 복사·공유 버튼 (디자인 시스템 InviteCode). */
@Composable
fun InviteCodeBox(
    code: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cellsDescription = stringResource(R.string.invite_code_desc, code.toCharArray().joinToString(" "))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TdrSpacing.s3)) {
        Text(stringResource(R.string.invite_code_label), style = TdrTheme.typography.label, color = TdrTheme.colors.inkMuted)
        Row(
            Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = cellsDescription },
            horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s2),
        ) {
            code.forEach { c ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(TdrShapes.sm)
                        .background(TdrTheme.colors.brandSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(c.toString(), style = TdrTheme.typography.titleLg, color = TdrTheme.colors.brandStrong)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3)) {
            TdrButton(
                text = stringResource(R.string.invite_code_copy),
                onClick = onCopy,
                style = TdrButtonStyle.Outline,
                height = TdrSize.buttonMd,
                modifier = Modifier.weight(1f),
            )
            TdrButton(
                text = stringResource(R.string.invite_code_share),
                onClick = onShare,
                height = TdrSize.buttonMd,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun InviteCodeBoxPreview() {
    TdrTheme {
        InviteCodeBox("4K7Q2M", {}, {}, Modifier.background(TdrTheme.colors.bgSurface).padding(16.dp))
    }
}
