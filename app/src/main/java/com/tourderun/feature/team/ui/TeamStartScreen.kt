package com.tourderun.feature.team.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tourderun.R
import com.tourderun.core.designsystem.component.IconTile
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrButtonStyle
import com.tourderun.core.designsystem.component.TdrIconButton
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

/** S03 팀 시작. 팀이 없는 사용자가 오는 화면. 만들기·참가는 모달([CreateTeamDialog], [JoinTeamDialog])로 띄운다. */
@Composable
fun TeamStartScreen(
    onCreateTeam: () -> Unit,
    onJoinTeam: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s8, bottom = 40.dp),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TdrSpacing.s3, Alignment.CenterVertically),
        ) {
            IconTile(TdrIcons.People, size = 72.dp)
            Spacer(Modifier.height(TdrSpacing.s2))
            Text(stringResource(R.string.team_start_title), style = TdrTheme.typography.titleLg, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.team_start_subtitle),
                style = TdrTheme.typography.body,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(TdrSpacing.s3)) {
            TdrButton(
                text = stringResource(R.string.team_start_create),
                onClick = onCreateTeam,
                icon = TdrIcons.Plus,
                modifier = Modifier.fillMaxWidth(),
            )
            TdrButton(
                text = stringResource(R.string.team_start_join),
                onClick = onJoinTeam,
                style = TdrButtonStyle.Outline,
                icon = TdrIcons.Enter,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 팀 만들기·참가 모달 틀: 아이콘 칸 · 제목 · 설명 · 닫기 + 내용.
 * [TeamDialog]로 감싸서 띄운다.
 */
@Composable
internal fun TeamModalCard(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = TdrTheme.colors
    Column(
        modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .shadow(16.dp, TdrShapes.lg)
            .clip(TdrShapes.lg)
            .background(colors.bgSurface)
            .padding(TdrSpacing.s5)
            .semantics { paneTitle = title },
        verticalArrangement = Arrangement.spacedBy(TdrSpacing.s4),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3)) {
            IconTile(icon, background = iconBackground, tint = iconTint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = TdrTheme.typography.title, modifier = Modifier.semantics { heading() })
                Text(subtitle, style = TdrTheme.typography.caption, color = colors.inkMuted)
            }
            TdrIconButton(
                icon = TdrIcons.Close,
                contentDescription = stringResource(R.string.action_close),
                onClick = onClose,
                tint = colors.inkSubtle,
                iconSize = 18.dp,
                modifier = Modifier.padding(0.dp),
            )
        }
        content()
    }
}

/** 화면 폭 - 좌우 20dp 모달. 배경 어둡게 하기(scrim)는 Dialog 창이 한다. */
@Composable
internal fun TeamDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(TdrSpacing.s5), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

/** 미리보기용: 팀 시작 화면 위에 모달을 겹쳐 그린다 (와이어프레임 S04·S05 모양). */
@Composable
internal fun ModalOverTeamStartPreview(content: @Composable () -> Unit) {
    TdrTheme {
        Box(Modifier.fillMaxSize()) {
            TeamStartScreen({}, {})
            Box(
                Modifier.fillMaxSize().background(TdrTheme.colors.scrim).padding(TdrSpacing.s5),
                contentAlignment = Alignment.Center,
            ) { content() }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun TeamStartScreenPreview() {
    TdrTheme { TeamStartScreen({}, {}) }
}
