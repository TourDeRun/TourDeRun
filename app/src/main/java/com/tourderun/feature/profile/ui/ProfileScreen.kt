package com.tourderun.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tourderun.R
import com.tourderun.core.designsystem.component.CardHeader
import com.tourderun.core.designsystem.component.Mascot
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.component.TdrEditIconButton
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.component.TdrTab
import com.tourderun.core.designsystem.component.valueWithUnit
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format
import com.tourderun.core.ui.model.RunRowUi
import com.tourderun.core.ui.preview.PreviewSamples
import com.tourderun.core.ui.preview.TabScreenPreview
import com.tourderun.feature.record.ui.RunRow

/** S13 프로필 상태. */
data class ProfileUiState(
    val nickname: String,
    val teamName: String?,
    /** null이면 아직 모름 → 역할 표시를 숨긴다. */
    val isLeader: Boolean?,
    /** null이면 아직 모르거나 읽기 실패 → "–" (0km와 구분). */
    val totalDistanceM: Long?,
    val runCount: Int?,
    val latestRun: RunRowUi?,
    /** 지역 도감(P1). 기능이 꺼져 있으면 null. */
    val collection: CollectionSummaryUi?,
    /** AUTH-04 로그아웃 확인 창이 열려 있는지. */
    val showLogoutConfirm: Boolean = false,
)

data class CollectionSummaryUi(
    val collected: Int,
    val total: Int,
    /** 다음에 얻을 도시. 다 모았으면 null. */
    val nextCity: String?,
)

/** S13 프로필. 하단 탭(프로필)은 AppNavHost가 붙인다. */
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEditNickname: () -> Unit,
    onTeamClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onRunClick: (RunRowUi) -> Unit,
    onCollectionClick: () -> Unit,
    onLogout: () -> Unit,
    onLogoutConfirm: () -> Unit,
    onLogoutDismiss: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3, bottom = TdrSpacing.s4),
        verticalArrangement = Arrangement.spacedBy(TdrSpacing.s3),
    ) {
        // 내 정보
        TdrCard(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(TdrSpacing.s25),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TdrSpacing.s25)) {
                Mascot(size = 88.dp, contentDescription = stringResource(R.string.profile_character_desc))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s05)) {
                    Text(uiState.nickname, style = type.statSm)
                    TdrEditIconButton(onEditNickname, stringResource(R.string.profile_edit_nickname))
                }
                if (uiState.teamName != null) {
                    TextButton(onClick = onTeamClick, modifier = Modifier.offset(y = (-6).dp)) {
                        val isLeader = uiState.isLeader
                        Text(
                            if (isLeader == null) {
                                stringResource(R.string.profile_team_only, uiState.teamName)
                            } else {
                                stringResource(
                                    R.string.profile_team_role,
                                    uiState.teamName,
                                    stringResource(if (isLeader) R.string.role_leader else R.string.role_member),
                                )
                            },
                            style = type.caption,
                            color = colors.inkMuted,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
                    ProfileStat(
                        uiState.totalDistanceM?.let { Format.km(it) } ?: stringResource(R.string.value_unknown),
                        stringResource(R.string.unit_km_spaced),
                        stringResource(R.string.profile_total_distance),
                        Modifier.weight(1f),
                    )
                    ProfileStat(
                        uiState.runCount?.toString() ?: stringResource(R.string.value_unknown),
                        stringResource(R.string.unit_times_spaced),
                        stringResource(R.string.profile_run_count),
                        Modifier.weight(1f),
                    )
                }
            }
        }

        // 내 기록
        TdrCard(onClick = onRecordsClick, verticalArrangement = Arrangement.spacedBy(TdrSpacing.s25)) {
            CardHeader(stringResource(R.string.records_title), trailing = stringResource(R.string.profile_view_all))
            uiState.latestRun?.let { run ->
                RunRow(run, onClick = { onRunClick(run) }, inCard = false)
            }
        }

        // 지역 도감 (P1)
        uiState.collection?.let { c ->
            TdrCard(onClick = onCollectionClick) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s35)) {
                    CardStack()
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TdrSpacing.s05)) {
                        Text(stringResource(R.string.profile_collection, c.collected, c.total), style = type.subtitle.copy(lineHeight = 20.sp))
                        Text(
                            c.nextCity?.let { stringResource(R.string.profile_collection_next, it) }
                                ?: stringResource(R.string.profile_collection_done),
                            style = type.caption,
                            color = colors.inkMuted,
                        )
                    }
                    Icon(TdrIcons.ChevronRight, contentDescription = null, tint = colors.inkSubtle, modifier = Modifier.size(18.dp))
                }
            }
        }

        // 계정
        val accountLabel = stringResource(R.string.profile_account)
        TdrCard(
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.semantics { paneTitle = accountLabel },
        ) {
            AccountRow(stringResource(R.string.profile_logout), colors.ink, onLogout)
            HorizontalDivider(color = colors.border)
            AccountRow(stringResource(R.string.profile_withdraw), colors.danger, onWithdraw)
        }
        Text(
            stringResource(R.string.profile_account_notice),
            style = type.micro.copy(lineHeight = 16.sp),
            color = colors.inkMuted,
            modifier = Modifier.padding(horizontal = TdrSpacing.s1),
        )
    }

    if (uiState.showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = onLogoutDismiss,
            title = { Text(stringResource(R.string.profile_logout_title), style = type.title) },
            text = { Text(stringResource(R.string.profile_logout_body), style = type.body, color = colors.inkMuted) },
            confirmButton = {
                TextButton(onClick = onLogoutConfirm) {
                    Text(stringResource(R.string.profile_logout), color = colors.danger, style = type.subtitle)
                }
            },
            dismissButton = {
                TextButton(onClick = onLogoutDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.ink, style = type.subtitle)
                }
            },
            containerColor = colors.bgSurface,
            shape = TdrShapes.lg,
        )
    }
}

@Composable
private fun ProfileStat(value: String, unit: String, label: String, modifier: Modifier) {
    val colors = TdrTheme.colors
    Column(
        modifier.clip(TdrShapes.md).background(colors.bgSubtle).padding(TdrSpacing.s3),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TdrSpacing.s05),
    ) {
        Text(valueWithUnit(value, unit, unitColor = colors.inkMuted), style = TdrTheme.typography.statMd)
        Text(label, style = TdrTheme.typography.micro, color = colors.inkMuted)
    }
}

@Composable
private fun AccountRow(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text,
        style = TdrTheme.typography.body,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = TdrSpacing.s4, vertical = 15.dp),
    )
}

/** 겹친 카드 3장 장식. */
@Composable
private fun CardStack() {
    val colors = TdrTheme.colors
    val shape = RoundedCornerShape(6.dp)
    Row {
        listOf(colors.brandLight, colors.brand, colors.brandStrong).forEachIndexed { i, c ->
            Box(
                Modifier
                    .offset(x = (-10 * i).dp)
                    .size(width = 28.dp, height = 38.dp)
                    .border(2.dp, colors.bgSurface, shape)
                    .padding(TdrSpacing.s05)
                    .clip(shape)
                    .background(c),
            )
        }
    }
}

private val previewState = ProfileUiState(
    nickname = "민지",
    teamName = "뛰뛰크루",
    isLeader = true,
    totalDistanceM = 86_400,
    runCount = 21,
    latestRun = PreviewSamples.runs.first(),
    collection = CollectionSummaryUi(3, 8, "천안"),
)

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ProfileScreenPreview() {
    TabScreenPreview(TdrTab.Profile) {
        ProfileScreen(previewState, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(widthDp = 390, heightDp = 844, name = "로그아웃 확인")
@Composable
private fun ProfileLogoutConfirmPreview() {
    TabScreenPreview(TdrTab.Profile) {
        ProfileScreen(previewState.copy(showLogoutConfirm = true), {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(widthDp = 390, heightDp = 844, name = "숫자·역할을 아직 모름(읽기 실패)")
@Composable
private fun ProfileUnknownPreview() {
    TabScreenPreview(TdrTab.Profile) {
        ProfileScreen(previewState.copy(isLeader = null, totalDistanceM = null, runCount = null), {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
