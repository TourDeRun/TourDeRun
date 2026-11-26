package com.tourderun.feature.team.ui

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tourderun.R
import com.tourderun.core.designsystem.component.InviteCodeBox
import com.tourderun.core.designsystem.component.MascotFace
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrTopBar
import com.tourderun.core.designsystem.component.TeamAppearance
import com.tourderun.core.designsystem.component.TeamImage
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

/** S06 초대 코드 공유 화면 상태 (팀을 막 만든 팀장). */
data class InviteCodeUiState(
    val teamName: String,
    val appearance: TeamAppearance,
    val code: String,
    val memberCount: Int,
    val maxMembers: Int = 4,
    val season: Int,
    val routeFrom: String,
    val routeTo: String,
    val routeKm: Int,
)

/**
 * S06 초대 코드 공유 (TEAM-02). 내 팀과 초대 코드를 [InviteCodeViewModel]에서 받는다.
 * 복사는 클립보드, 공유는 Android 공유 시트. 불러오는 중이면 스피너.
 */
@Composable
fun InviteCodeScreen(
    onBack: () -> Unit,
    onDepart: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InviteCodeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState
    if (state == null) {
        Box(
            modifier
                .fillMaxSize()
                .background(TdrTheme.colors.bgSurface),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = TdrTheme.colors.brand)
        }
        return
    }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val shareText = stringResource(R.string.invite_share_text, state.teamName, state.code)
    InviteCodeContent(
        uiState = state,
        onBack = onBack,
        onCopyCode = {
            clipboard.setText(AnnotatedString(state.code))
            // Android 13부터는 시스템이 복사 알림을 띄우므로 그 전 버전에서만 알린다
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, R.string.invite_copied, Toast.LENGTH_SHORT).show()
            }
        },
        onShareCode = {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
            context.startActivity(Intent.createChooser(send, null))
        },
        onDepart = onDepart,
        modifier = modifier,
    )
}

/** S06 초대 코드 공유 화면 그리기만 한다 (상태 없음, Preview 대상). 복사·공유 후 [onDepart]로 홈에 간다. */
@Composable
fun InviteCodeContent(
    uiState: InviteCodeUiState,
    onBack: () -> Unit,
    onCopyCode: () -> Unit,
    onShareCode: () -> Unit,
    onDepart: () -> Unit,
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
            .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3, bottom = 40.dp),
    ) {
        TdrTopBar(onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TdrSpacing.s5, Alignment.CenterVertically),
        ) {
            TeamImage(
                uiState.appearance,
                size = 80.dp,
                face = MascotFace.Open,
                contentDescription = stringResource(R.string.team_image_desc, uiState.teamName),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TdrSpacing.s1)) {
                Text(stringResource(R.string.invite_created), style = type.label.copy(fontWeight = type.bodySm.fontWeight), color = colors.inkMuted)
                Text(uiState.teamName, style = type.titleLg.copy(fontSize = 26.sp, lineHeight = 34.sp))
                Text(
                    stringResource(R.string.invite_season, uiState.season, uiState.routeFrom, uiState.routeTo, uiState.routeKm),
                    style = type.label.copy(fontWeight = type.bodySm.fontWeight),
                    color = colors.inkMuted,
                )
            }
            InviteCodeBox(code = uiState.code, onCopy = onCopyCode, onShare = onShareCode)
            MemberSlots(uiState.memberCount, uiState.maxMembers)
        }
        Spacer(Modifier.height(TdrSpacing.s4))
        TdrButton(stringResource(R.string.invite_depart), onClick = onDepart, modifier = Modifier.fillMaxWidth())
    }
}

/** "팀원 1 / 4" + 자리 점 4개. */
@Composable
private fun MemberSlots(count: Int, max: Int) {
    val colors = TdrTheme.colors
    val desc = stringResource(R.string.invite_members_desc, count, max)
    Row(Modifier.fillMaxWidth().padding(horizontal = TdrSpacing.s1), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.invite_members, count, max),
            style = TdrTheme.typography.caption,
            color = colors.inkMuted,
            modifier = Modifier.weight(1f),
        )
        Row(
            Modifier.clearAndSetSemantics { contentDescription = desc },
            horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s1),
        ) {
            repeat(max) { i ->
                Box(
                    Modifier
                        .size(20.dp)
                        .background(if (i < count) colors.brand else colors.bgSubtle, CircleShape)
                        .then(if (i < count) Modifier else Modifier.border(1.dp, colors.border, CircleShape)),
                )
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun InviteCodeContentPreview() {
    TdrTheme {
        InviteCodeContent(
            InviteCodeUiState("뛰뛰크루", TeamAppearance(), "4K7Q2M", 1, 4, 1, "서울", "부산", 350),
            {}, {}, {}, {},
        )
    }
}
