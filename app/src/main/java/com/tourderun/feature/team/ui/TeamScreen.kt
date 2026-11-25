package com.tourderun.feature.team.ui

import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.tourderun.core.designsystem.component.TdrIcons
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.AlertDialog
import com.tourderun.core.designsystem.component.TeamColor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tourderun.R
import com.tourderun.core.designsystem.component.CardHeader
import com.tourderun.core.designsystem.component.MemberAvatar
import com.tourderun.core.designsystem.component.Pill
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrButtonStyle
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.component.TdrProgressBar
import com.tourderun.core.designsystem.component.TdrTopBar
import com.tourderun.core.data.DataError
import com.tourderun.core.ui.LoadFailed
import com.tourderun.core.ui.loadFailedMessageRes
import com.tourderun.core.designsystem.component.TdrTopBarTextAction
import com.tourderun.core.designsystem.component.TeamImage
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSize
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format
import com.tourderun.core.ui.model.MemberUi
import com.tourderun.core.ui.model.TeamUi
import com.tourderun.core.ui.preview.PreviewSamples

/** S14 팀 화면 상태. */
data class TeamUiState(
    val team: TeamUi,
    /** 기여 거리 순으로 정렬해서 준다. */
    val members: List<MemberUi>,
    val inviteCode: String,
    /** 내가 팀장이면 수정·내보내기를 보인다. */
    val isLeader: Boolean,
    val myUid: String,
    /** 나간 팀원들이 남긴 거리(M-02: 팀 거리는 줄지 않는다). 0이면 "이전 팀원" 줄을 숨긴다. */
    val formerM: Long = 0,
    val formerPercent: Int = 0,
)

/**
 * S14 팀 (TEAM-04). 내 팀·팀원·초대 코드를 [TeamViewModel]에서 받는다.
 * 불러오는 중이면 스피너, 실패면 다시 시도. 둘 다 상단 바를 먼저 그려 뒤로 갈 수 있게 한다.
 * 초대 코드 공유는 S06 화면을 다시 쓴다([onShowInvite]).
 */
@Composable
fun TeamScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShowInvite: () -> Unit,
    onLeft: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeamViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val leave by viewModel.leave.collectAsStateWithLifecycle()
    val kick by viewModel.kick.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val handOver by viewModel.handOver.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                TeamEvent.Left -> onLeft()
                is TeamEvent.ShowMessage -> Toast.makeText(context, event.messageRes, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val state = (uiState as? TeamLoadState.Ready)?.state
    if (state == null) {
        TeamLoadingContent(
            failed = uiState as? TeamLoadState.Failed,
            onBack = onBack,
            onRetry = viewModel::retry,
            modifier = modifier,
        )
        return
    }
    TeamContent(
        uiState = state,
        onBack = onBack,
        onEdit = onEdit,
        onKick = { viewModel.onAction(TeamAction.KickClick(it)) },
        onHandOver = { viewModel.onAction(TeamAction.HandOverClick(it)) },
        onShareInvite = onShowInvite,
        onLeave = { viewModel.onAction(TeamAction.LeaveClick) },
        modifier = modifier,
    )
    handOver.target?.let { target ->
        HandOverDialog(
            nickname = target.nickname,
            handOver = handOver,
            onConfirm = { viewModel.onAction(TeamAction.HandOverConfirm) },
            onDismiss = { viewModel.onAction(TeamAction.HandOverDismiss) },
        )
    }
    kick.target?.let { target ->
        KickMemberDialog(
            nickname = target.nickname,
            kick = kick,
            onConfirm = { viewModel.onAction(TeamAction.KickConfirm) },
            onDismiss = { viewModel.onAction(TeamAction.KickDismiss) },
        )
    }
    if (leave.showConfirm) {
        LeaveTeamDialog(
            isLast = state.team.memberCount <= 1,
            leave = leave,
            onConfirm = { viewModel.onAction(TeamAction.LeaveConfirm) },
            onDismiss = { viewModel.onAction(TeamAction.LeaveDismiss) },
        )
    }
}

/** TEAM-08 팀장 넘기기 확인 창. */
@Composable
private fun HandOverDialog(nickname: String, handOver: HandOverUiState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.team_hand_over_confirm_title, nickname), style = type.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
                Text(stringResource(R.string.team_hand_over_confirm_body), style = type.body, color = colors.inkMuted)
                handOver.errorRes?.let { Text(stringResource(it), style = type.caption, color = colors.danger) }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !handOver.isWorking) {
                Text(
                    stringResource(R.string.team_hand_over),
                    color = if (handOver.isWorking) colors.inkMuted else colors.brand,
                    style = type.subtitle,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !handOver.isWorking) {
                Text(stringResource(R.string.action_cancel), color = colors.ink, style = type.subtitle)
            }
        },
        containerColor = colors.bgSurface,
        shape = TdrShapes.lg,
    )
}

/** TEAM-06 내보내기 확인 창. */
@Composable
private fun KickMemberDialog(nickname: String, kick: KickUiState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.team_kick_confirm_title, nickname), style = type.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
                Text(stringResource(R.string.team_kick_confirm_body), style = type.body, color = colors.inkMuted)
                kick.errorRes?.let { Text(stringResource(it), style = type.caption, color = colors.danger) }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !kick.isKicking) {
                Text(
                    stringResource(R.string.team_kick),
                    color = if (kick.isKicking) colors.inkMuted else colors.danger,
                    style = type.subtitle,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !kick.isKicking) {
                Text(stringResource(R.string.action_cancel), color = colors.ink, style = type.subtitle)
            }
        },
        containerColor = colors.bgSurface,
        shape = TdrShapes.lg,
    )
}

/** TEAM-07 나가기 확인 창. 마지막 1명이면 팀이 없어진다고 알린다. */
@Composable
private fun LeaveTeamDialog(isLast: Boolean, leave: LeaveUiState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.team_leave_confirm_title), style = type.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
                Text(
                    stringResource(if (isLast) R.string.team_leave_confirm_last else R.string.team_leave_confirm_body),
                    style = type.body,
                    color = colors.inkMuted,
                )
                leave.errorRes?.let { Text(stringResource(it), style = type.caption, color = colors.danger) }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !leave.isLeaving) {
                Text(
                    stringResource(R.string.team_leave),
                    color = if (leave.isLeaving) colors.inkMuted else colors.danger,
                    style = type.subtitle,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !leave.isLeaving) {
                Text(stringResource(R.string.action_cancel), color = colors.ink, style = type.subtitle)
            }
        },
        containerColor = colors.bgSurface,
        shape = TdrShapes.lg,
    )
}

/** S14 팀 그리기만 한다 (상태 없음, Preview 대상). 홈의 팀 카드·팀원 기록에서 들어온다. */
@Composable
fun TeamContent(
    uiState: TeamUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onKick: (MemberUi) -> Unit,
    onHandOver: (MemberUi) -> Unit,
    onShareInvite: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val team = uiState.team
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3, bottom = TdrSpacing.s8),
        verticalArrangement = Arrangement.spacedBy(TdrSpacing.s3),
    ) {
        TdrTopBar(title = stringResource(R.string.team_title), onBack = onBack) {
            if (uiState.isLeader) TdrTopBarTextAction(stringResource(R.string.action_edit), onEdit)
        }

        TdrCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TeamImage(team.appearance, size = 64.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(team.name, style = type.statSm)
                    Text(
                        stringResource(R.string.team_members_count, team.memberCount, team.maxMembers, team.season),
                        style = type.caption,
                        color = colors.inkMuted,
                    )
                    Text(
                        stringResource(R.string.team_distance, Format.teamKm(team.distanceM)),
                        style = type.subtitle.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 20.sp),
                        color = colors.brand,
                    )
                }
            }
        }

        TdrCard(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CardHeader(stringResource(R.string.team_members_title))
            uiState.members.forEach { member ->
                MemberRow(
                    member = member,
                    teamDistanceM = team.distanceM,
                    canKick = uiState.isLeader && member.uid != uiState.myUid,
                    onKick = { onKick(member) },
                    onHandOver = { onHandOver(member) },
                )
            }
            if (uiState.formerM > 0) FormerMembersRow(uiState.formerM, uiState.formerPercent)
        }

        TdrCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.team_invite_label), style = type.caption, color = colors.inkMuted)
                    Text(
                        uiState.inviteCode,
                        style = type.statMd.copy(letterSpacing = 0.2.em),
                        modifier = Modifier.semantics {
                            contentDescription = uiState.inviteCode.toCharArray().joinToString(" ")
                        },
                    )
                }
                TdrButton(
                    text = stringResource(R.string.invite_code_share),
                    onClick = onShareInvite,
                    style = TdrButtonStyle.Soft,
                    height = TdrSize.touch,
                )
            }
        }

        Spacer(Modifier.height(TdrSpacing.s3))
        TdrButton(
            text = stringResource(R.string.team_leave),
            onClick = onLeave,
            style = TdrButtonStyle.OutlineDanger,
            height = TdrSize.buttonMd,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.team_leave_notice),
            style = type.micro.copy(lineHeight = 16.sp),
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MemberRow(
    member: MemberUi,
    teamDistanceM: Long,
    canKick: Boolean,
    onKick: () -> Unit,
    onHandOver: () -> Unit,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val share = member.percent / 100f
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MemberAvatar(member.color, size = 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(member.nickname, style = type.subtitle.copy(lineHeight = 20.sp))
                    val badge = type.micro.copy(lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
                    if (member.isLeader) Pill(stringResource(R.string.role_leader), style = badge)
                    if (member.isMvp) {
                        Pill(
                            stringResource(R.string.mvp_badge),
                            background = colors.highlightSoft,
                            contentColor = colors.highlightInk,
                            style = badge,
                        )
                    }
                }
                Text(
                    stringResource(R.string.team_member_sub, Format.teamKm(member.contributionM), member.percent),
                    style = type.caption,
                    color = colors.inkMuted,
                )
            }
            // 팀장만 다른 팀원에게 메뉴(팀장 넘기기 TEAM-08, 내보내기 TEAM-06)를 연다
            if (canKick) {
                var menuOpen by remember { mutableStateOf(false) }
                val menuDesc = stringResource(R.string.team_member_menu_desc, member.nickname)
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.semantics { contentDescription = menuDesc }) {
                        Icon(TdrIcons.More, contentDescription = null, tint = colors.inkMuted)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.team_hand_over), style = type.body) },
                            onClick = {
                                menuOpen = false
                                onHandOver()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.team_kick), style = type.body, color = colors.danger) },
                            onClick = {
                                menuOpen = false
                                onKick()
                            },
                        )
                    }
                }
            }
        }
        TdrProgressBar(progress = share, trackColor = colors.bgSubtle, fillColor = member.color.color)
    }
}

/** 나간 팀원들 몫을 묶은 한 줄 (EXT-03). 내보내기 버튼은 없다. */
@Composable
private fun FormerMembersRow(distanceM: Long, percent: Int) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MemberAvatar(TeamColor.GRAY, size = 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(stringResource(R.string.team_former_members), style = type.subtitle.copy(lineHeight = 20.sp), color = colors.inkMuted)
                Text(
                    stringResource(R.string.team_member_sub, Format.teamKm(distanceM), percent),
                    style = type.caption,
                    color = colors.inkMuted,
                )
            }
        }
        TdrProgressBar(progress = percent / 100f, trackColor = colors.bgSubtle, fillColor = TeamColor.GRAY.color)
    }
}

/** S14 불러오는 중(스피너)·실패(다시 시도). 상단 바는 먼저 그린다. */
@Composable
private fun TeamLoadingContent(
    failed: TeamLoadState.Failed?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(TdrTheme.colors.bgCanvas)
            .statusBarsPadding()
            .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3),
    ) {
        TdrTopBar(title = stringResource(R.string.team_title), onBack = onBack)
        if (failed != null) {
            LoadFailed(failed.error.loadFailedMessageRes(), onRetry)
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TdrTheme.colors.brand)
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844, name = "불러오기 실패")
@Composable
private fun TeamLoadFailedPreview() {
    TdrTheme {
        TeamLoadingContent(TeamLoadState.Failed(DataError.Unavailable), {}, {})
    }
}

@Preview(widthDp = 390, heightDp = 960)
@Composable
private fun TeamContentPreview() {
    TdrTheme {
        TeamContent(
            TeamUiState(PreviewSamples.team, PreviewSamples.members, "4K7Q2M", isLeader = true, myUid = "u1"),
            {}, {}, {}, {}, {}, {},
        )
    }
}

@Preview(widthDp = 390, heightDp = 960, name = "혼자 있는 팀 · 이전 팀원")
@Composable
private fun TeamContentAlonePreview() {
    TdrTheme {
        TeamContent(
            TeamUiState(
                team = PreviewSamples.team.copy(memberCount = 1, distanceM = 12_000),
                members = listOf(PreviewSamples.members.first().copy(contributionM = 9_000, isMvp = false, percent = 75)),
                inviteCode = "4K7Q2M",
                isLeader = true,
                myUid = "u1",
                formerM = 3_000,
                formerPercent = 25,
            ),
            {}, {}, {}, {}, {}, {},
        )
    }
}
