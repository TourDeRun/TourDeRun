package com.tourderun.feature.team.ui

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
import kotlin.math.roundToInt

/** S14 팀 화면 상태. */
data class TeamUiState(
    val team: TeamUi,
    /** 기여 거리 순으로 정렬해서 준다. */
    val members: List<MemberUi>,
    val inviteCode: String,
    /** 내가 팀장이면 수정·내보내기를 보인다. */
    val isLeader: Boolean,
    val myUid: String,
)

/** S14 팀. 홈의 팀 카드·팀원 기록에서 들어온다. */
@Composable
fun TeamScreen(
    uiState: TeamUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onKick: (MemberUi) -> Unit,
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
                        stringResource(R.string.team_distance, Format.km(team.distanceM)),
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
                )
            }
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
private fun MemberRow(member: MemberUi, teamDistanceM: Long, canKick: Boolean, onKick: () -> Unit) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val share = if (teamDistanceM > 0) member.contributionM.toFloat() / teamDistanceM else 0f
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
                    stringResource(R.string.team_member_sub, Format.km(member.contributionM), (share * 100).roundToInt()),
                    style = type.caption,
                    color = colors.inkMuted,
                )
            }
            if (canKick) {
                val kickDesc = stringResource(R.string.team_kick_desc, member.nickname)
                TextButton(
                    onClick = onKick,
                    shape = TdrShapes.sm,
                    modifier = Modifier.semantics { contentDescription = kickDesc },
                ) {
                    Text(
                        stringResource(R.string.team_kick),
                        style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.inkMuted,
                    )
                }
            }
        }
        TdrProgressBar(progress = share, trackColor = colors.bgSubtle, fillColor = member.color.color)
    }
}

@Preview(widthDp = 390, heightDp = 960)
@Composable
private fun TeamScreenPreview() {
    TdrTheme {
        TeamScreen(
            TeamUiState(PreviewSamples.team, PreviewSamples.members, "4K7Q2M", isLeader = true, myUid = "u1"),
            {}, {}, {}, {}, {},
        )
    }
}
