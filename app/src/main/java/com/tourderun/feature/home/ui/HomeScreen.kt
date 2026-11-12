package com.tourderun.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tourderun.R
import com.tourderun.core.designsystem.component.CardHeader
import com.tourderun.core.designsystem.component.MascotFace
import com.tourderun.core.designsystem.component.MemberAvatar
import com.tourderun.core.designsystem.component.Pill
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.component.TdrProgressBar
import com.tourderun.core.designsystem.component.TdrTab
import com.tourderun.core.designsystem.component.TeamImage
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format
import com.tourderun.core.ui.model.MemberUi
import com.tourderun.core.ui.model.RivalTeamUi
import com.tourderun.core.ui.model.StageProgressUi
import com.tourderun.core.ui.model.toNextCityText
import com.tourderun.core.ui.model.TeamUi
import com.tourderun.core.ui.preview.PreviewSamples
import com.tourderun.core.ui.preview.TabScreenPreview
import kotlin.math.roundToInt

/** 이번 주 요약 (S16 주간 통계로 들어가는 카드). */
data class WeeklySummaryUi(
    val distanceM: Long,
    val runCount: Int,
    /** 지난주 대비 (음수 가능). */
    val diffFromLastWeekM: Long,
    val totalTeams: Int,
    val topPercent: Int,
)

/** S07 홈 화면 상태. */
data class HomeUiState(
    val team: TeamUi,
    val stage: StageProgressUi,
    val rank: Int,
    val totalTeams: Int,
    /** 바로 앞 팀. 선두면 null. */
    val ahead: RivalTeamUi?,
    /** 바로 뒤 팀. 꼴찌면 null. */
    val behind: RivalTeamUi?,
    val weekly: WeeklySummaryUi,
    /** 기여 거리 순. */
    val members: List<MemberUi>,
)

/** S07 홈. 하단 탭(홈)은 AppNavHost가 붙인다. */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onTeamClick: () -> Unit,
    onStageRankClick: () -> Unit,
    onWeeklyStatsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3, bottom = TdrSpacing.s5),
        verticalArrangement = Arrangement.spacedBy(TdrSpacing.s3),
    ) {
        TeamHeaderCard(uiState.team, onTeamClick)
        StageCard(uiState.stage, uiState.rank, uiState.totalTeams)
        RaceCard(uiState, onStageRankClick)
        WeeklyCard(uiState.weekly, onWeeklyStatsClick)
        MembersChartCard(uiState.members, uiState.team.distanceM, onTeamClick)
    }
}

@Composable
private fun TeamHeaderCard(team: TeamUi, onClick: () -> Unit) {
    val type = TdrTheme.typography
    TdrCard(onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3)) {
            TeamImage(team.appearance, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(team.name, style = type.title.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.home_team_summary, team.season, team.memberCount, Format.km(team.distanceM)),
                    style = type.caption,
                    color = TdrTheme.colors.inkMuted,
                )
            }
            Icon(TdrIcons.ChevronRight, contentDescription = null, tint = TdrTheme.colors.inkSubtle, modifier = Modifier.size(18.dp))
        }
    }
}

/** 남색 스테이지 카드: 스테이지 n/7 · 순위 · 구간 · 진행 바 · 다음 도시까지. */
@Composable
private fun StageCard(stage: StageProgressUi, rank: Int, totalTeams: Int) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    TdrCard(
        color = colors.brandStrong,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(
                stringResource(R.string.stage_of, stage.stageNumber, stage.stageCount),
                background = Color.White.copy(alpha = 0.16f),
                contentColor = colors.inkOnBrand,
            )
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.home_teams_prefix, totalTeams), style = type.caption, color = colors.inkOnBrandMuted)
            Text(
                stringResource(R.string.rank_value, rank),
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = colors.inkOnBrand,
            )
        }
        Text(
            stringResource(R.string.route_format, stage.fromCity, stage.toCity),
            style = type.titleXl.copy(lineHeight = 30.sp),
            color = colors.inkOnBrand,
        )
        TdrProgressBar(
            progress = stage.progress,
            trackColor = Color.White.copy(alpha = 0.2f),
            fillColor = Color.White,
            contentDescription = stringResource(R.string.stage_progress_desc),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_stage_done, Format.kmDown(stage.doneInStageM), stage.lengthKm),
                style = type.caption,
                color = colors.inkOnBrandMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                stage.toNextCityText(),
                style = type.bodySm.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = colors.inkOnBrand,
            )
        }
    }
}

@Composable
private fun RaceCard(uiState: HomeUiState, onClick: () -> Unit) {
    val colors = TdrTheme.colors
    val ourM = uiState.team.distanceM
    TdrCard(onClick = onClick) {
        CardHeader(stringResource(R.string.home_race_title), trailing = stringResource(R.string.home_race_all))
        Row(horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
            RaceCell(
                label = stringResource(R.string.home_race_ahead, uiState.ahead?.name ?: stringResource(R.string.home_race_none)),
                value = uiState.ahead?.let { Format.signedKm(it.distanceM - ourM) + stringResource(R.string.unit_km) } ?: "-",
                valueColor = uiState.ahead?.appearance?.color?.textColor ?: colors.inkMuted,
                background = colors.bgSubtle,
                labelColor = colors.inkMuted,
                modifier = Modifier.weight(1f),
            )
            RaceCell(
                label = stringResource(R.string.home_race_us),
                value = stringResource(R.string.rank_value, uiState.rank),
                valueColor = colors.brand,
                background = colors.brandSoft,
                labelColor = colors.brand,
                modifier = Modifier.weight(1f),
            )
            RaceCell(
                label = stringResource(R.string.home_race_behind, uiState.behind?.name ?: stringResource(R.string.home_race_none)),
                value = uiState.behind?.let { Format.signedKm(it.distanceM - ourM) + stringResource(R.string.unit_km) } ?: "-",
                valueColor = uiState.behind?.appearance?.color?.textColor ?: colors.inkMuted,
                background = colors.bgSubtle,
                labelColor = colors.inkMuted,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RaceCell(
    label: String,
    value: String,
    valueColor: Color,
    background: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(TdrShapes.md)
            .background(background)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = TdrTheme.typography.micro, color = labelColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = TdrTheme.typography.subtitle.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold), color = valueColor)
    }
}

@Composable
private fun WeeklyCard(weekly: WeeklySummaryUi, onClick: () -> Unit) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    TdrCard(color = colors.highlightSoft, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(TdrIcons.BarChart, contentDescription = null, tint = colors.highlightInk, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.home_weekly_title, Format.km(weekly.distanceM), weekly.runCount),
                    style = type.subtitle.copy(lineHeight = 20.sp),
                    color = colors.highlightInk,
                )
                Text(
                    stringResource(
                        R.string.home_weekly_sub,
                        Format.signedKm(weekly.diffFromLastWeekM),
                        weekly.totalTeams,
                        weekly.topPercent,
                    ),
                    style = type.caption,
                    color = colors.highlightInk,
                )
            }
            Icon(TdrIcons.ChevronRight, contentDescription = null, tint = colors.highlightInk, modifier = Modifier.size(18.dp))
        }
    }
}

/** 팀원 누적 기록 세로 막대 + 아바타. 가장 많이 달린 사람 막대가 140dp. */
@Composable
private fun MembersChartCard(members: List<MemberUi>, teamDistanceM: Long, onClick: () -> Unit) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val maxM = members.maxOfOrNull { it.contributionM }?.coerceAtLeast(1L) ?: 1L
    val chartDesc = stringResource(
        R.string.home_members_chart_desc,
        // map은 inline이라 안에서 stringResource를 부를 수 있다 (joinToString 람다는 안 된다)
        members.map { stringResource(R.string.home_members_chart_item, it.nickname, Format.km(it.contributionM)) }
            .joinToString(", "),
    )
    TdrCard(onClick = onClick, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CardHeader(stringResource(R.string.home_members_title), trailing = stringResource(R.string.home_members_total, Format.km(teamDistanceM)))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = TdrSpacing.s2)
                .clearAndSetSemantics { contentDescription = chartDesc },
            horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s2),
            verticalAlignment = Alignment.Bottom,
        ) {
            members.forEach { m ->
                val pct = if (teamDistanceM > 0) (m.contributionM * 100f / teamDistanceM).roundToInt() else 0
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (m.isMvp) {
                        Icon(TdrIcons.Crown, contentDescription = null, tint = colors.mvp, modifier = Modifier.padding(bottom = 2.dp).size(18.dp))
                    }
                    Text(
                        Format.km(m.contributionM) + stringResource(R.string.unit_km),
                        style = type.label.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Box(
                        Modifier
                            .width(40.dp)
                            .height((140f * m.contributionM / maxM).dp)
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                            .background(m.color.color),
                    )
                    MemberAvatar(m.color, size = 52.dp, face = MascotFace.Smile, modifier = Modifier.padding(top = 6.dp))
                    Text(m.nickname, style = type.label.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 4.dp))
                    Text("$pct%", style = type.micro, color = colors.inkMuted)
                }
            }
        }
        if (members.any { it.isMvp }) {
            Text(stringResource(R.string.home_mvp_hint), style = type.micro, color = colors.inkMuted)
        }
    }
}

@Preview(widthDp = 390, heightDp = 1080)
@Composable
private fun HomeScreenPreview() {
    TabScreenPreview(TdrTab.Home) {
        HomeScreen(
            HomeUiState(
                team = PreviewSamples.team,
                stage = PreviewSamples.stage,
                rank = 2,
                totalTeams = 5,
                ahead = PreviewSamples.rivals[0],
                behind = PreviewSamples.rivals[1],
                weekly = WeeklySummaryUi(42_500, 9, 6_100, 38, 11),
                members = PreviewSamples.members,
            ),
            {}, {}, {},
        )
    }
}
