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
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tourderun.R
import com.tourderun.core.data.DataError
import com.tourderun.core.designsystem.component.CardHeader
import com.tourderun.core.designsystem.component.MascotFace
import com.tourderun.core.designsystem.component.MemberAvatar
import com.tourderun.core.designsystem.component.Pill
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.component.TdrProgressBar
import com.tourderun.core.designsystem.component.TdrTab
import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.designsystem.component.TeamImage
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format
import com.tourderun.core.ui.LoadFailed
import com.tourderun.core.ui.loadFailedMessageRes
import com.tourderun.core.ui.model.MemberUi
import com.tourderun.core.ui.model.RivalTeamUi
import com.tourderun.core.ui.model.StageProgressUi
import com.tourderun.core.ui.model.TeamUi
import com.tourderun.core.ui.model.stageProgressUi
import com.tourderun.core.ui.model.toNextCityText
import com.tourderun.core.ui.preview.PreviewSamples
import com.tourderun.core.ui.preview.TabScreenPreview
import com.tourderun.domain.route.routeFor
import kotlin.math.abs

/** 이번 주 요약 (S16 주간 통계로 들어가는 카드). */
data class WeeklySummaryUi(
    val distanceM: Long,
    val runCount: Int,
    /** 지난주 대비 (음수 가능). 지난주 기록이 없으면 null. */
    val diffFromLastWeekM: Long?,
    /** 상위 %를 보일 때만 값이 있다(오프라인·이번 주 0km면 null). [topPercent]와 함께 채운다. */
    val totalTeams: Int?,
    val topPercent: Int?,
)

/** S07 홈 화면 상태. */
data class HomeUiState(
    val team: TeamUi,
    val stage: StageProgressUi,
    val rank: Int,
    val totalTeams: Int,
    /** 바로 앞 팀. 스테이지 안에 없으면 다음 스테이지에서 가장 가까운 팀. 전체 1위면 null. */
    val ahead: RivalTeamUi?,
    /** 바로 뒤 팀. 스테이지 안에 없으면 이전 스테이지에서 가장 가까운 팀. 전체 꼴찌면 null. */
    val behind: RivalTeamUi?,
    /** 아직 통계를 받지 못했으면 null(카드를 숨긴다). */
    val weekly: WeeklySummaryUi?,
    /** 기여 거리 순. 비어 있으면 팀원 카드를 숨긴다. */
    val members: List<MemberUi>,
    /** [ahead]가 다음 스테이지 팀 (RACE-02). */
    val aheadIsOtherStage: Boolean = false,
    /** [behind]가 이전 스테이지 팀 (RACE-02). */
    val behindIsOtherStage: Boolean = false,
    /** 기기 캐시 값을 보여 주는 중. */
    val isOffline: Boolean = false,
    /** 이번 주 통계를 불러오지 못했다. 이번 주 카드 자리에 실패 + 다시 시도를 보인다. */
    val weeklyError: DataError? = null,
)

/**
 * S07 홈. 하단 탭(홈)은 AppNavHost가 붙인다. 상태를 받기 전과 팀이 없을 때는 바탕만 보인다.
 * 경쟁 상황을 불러오지 못하면 실패 + 다시 시도를 보인다(JRN-03).
 */
@Composable
fun HomeScreen(
    onTeamClick: () -> Unit,
    onStageRankClick: () -> Unit,
    onWeeklyStatsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val load by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = load) {
        HomeLoad.Loading -> Box(modifier.fillMaxSize().background(TdrTheme.colors.bgCanvas))
        is HomeLoad.Failed -> HomeFailed(state.error, viewModel::retry, modifier)
        is HomeLoad.Content -> HomeContent(
            uiState = state.ui,
            onTeamClick = onTeamClick,
            onStageRankClick = onStageRankClick,
            onWeeklyStatsClick = onWeeklyStatsClick,
            onRetryWeekly = viewModel::retryWeekly,
            modifier = modifier,
        )
    }
}

/** S07 불러오기 실패. 상태 없음(Preview 대상). */
@Composable
fun HomeFailed(error: DataError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(TdrTheme.colors.bgCanvas)
            .statusBarsPadding()
            .padding(horizontal = TdrSpacing.s5),
        contentAlignment = Alignment.Center,
    ) {
        LoadFailed(error.loadFailedMessageRes(), onRetry)
    }
}

/** S07 화면 내용. 상태 없음(Preview 대상). */
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onTeamClick: () -> Unit,
    onStageRankClick: () -> Unit,
    onWeeklyStatsClick: () -> Unit,
    onRetryWeekly: () -> Unit,
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
        if (uiState.isOffline) {
            // 팀 카드가 맨 아래로 내려가 오프라인 표시는 맨 위에 따로 둔다.
            Pill(
                stringResource(R.string.home_offline),
                background = colors.inkMuted,
                contentColor = colors.inkOnBrand,
                modifier = Modifier.align(Alignment.End),
            )
        }
        StageCard(uiState.stage, uiState.rank, uiState.totalTeams)
        RaceCard(uiState, onStageRankClick)
        val weekly = uiState.weekly
        val weeklyError = uiState.weeklyError
        if (weekly != null) {
            WeeklyCard(weekly, onWeeklyStatsClick)
        } else if (weeklyError != null) {
            TdrCard { LoadFailed(weeklyError.loadFailedMessageRes(), onRetryWeekly) }
        }
        TeamCard(uiState.team, uiState.members, onTeamClick)
    }
}

/** 남색 스테이지 카드: 스테이지 n/7 · 순위 · 구간 · 진행 바 · 다음 도시까지. */
@Composable
private fun StageCard(stage: StageProgressUi, rank: Int, totalTeams: Int) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    TdrCard(
        color = colors.brandStrong,
        contentPadding = PaddingValues(horizontal = TdrSpacing.s4, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(TdrSpacing.s25),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(
                stringResource(R.string.stage_of, stage.stageNumber, stage.stageCount),
                background = colors.onBrandChip,
                contentColor = colors.inkOnBrand,
            )
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.home_teams_prefix, totalTeams), style = type.caption, color = colors.inkOnBrandMuted)
            Text(
                stringResource(R.string.rank_value, rank),
                style = type.captionBold,
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
            trackColor = colors.onBrandTrack,
            fillColor = colors.inkOnBrand,
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
        if (uiState.ahead == null && uiState.behind == null) {
            // 앱 전체에 다른 팀이 없다 (RACE-02)
            Text(stringResource(R.string.home_race_empty), style = TdrTheme.typography.body, color = colors.inkMuted)
            return@TdrCard
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
            RaceCell(
                label = stringResource(
                    if (uiState.aheadIsOtherStage) R.string.home_race_ahead_other else R.string.home_race_ahead,
                    uiState.ahead?.name ?: stringResource(R.string.home_race_none),
                ),
                value = uiState.ahead?.let { stringResource(R.string.home_race_gap_ahead, Format.km(abs(it.distanceM - ourM))) } ?: "-",
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
                label = stringResource(
                    if (uiState.behindIsOtherStage) R.string.home_race_behind_other else R.string.home_race_behind,
                    uiState.behind?.name ?: stringResource(R.string.home_race_none),
                ),
                value = uiState.behind?.let { stringResource(R.string.home_race_gap_behind, Format.km(abs(it.distanceM - ourM))) } ?: "-",
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
            .padding(horizontal = TdrSpacing.s1, vertical = TdrSpacing.s25),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TdrSpacing.s05),
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s35)) {
            Icon(TdrIcons.BarChart, contentDescription = null, tint = colors.highlightInk, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TdrSpacing.s05)) {
                Text(
                    stringResource(R.string.home_weekly_title, Format.km(weekly.distanceM), weekly.runCount),
                    style = type.subtitle.copy(lineHeight = 20.sp),
                    color = colors.highlightInk,
                )
                // listOfNotNull은 inline이라 안에서 stringResource를 부를 수 있다
                val sub = if (weekly.distanceM <= 0L) {
                    stringResource(R.string.home_weekly_first)
                } else {
                    listOfNotNull(
                        weekly.diffFromLastWeekM?.let { stringResource(R.string.home_weekly_diff, Format.signedKm(it)) },
                        if (weekly.totalTeams != null && weekly.topPercent != null) {
                            stringResource(R.string.home_weekly_top, weekly.totalTeams, weekly.topPercent)
                        } else {
                            null
                        },
                    ).joinToString(" · ")
                }
                if (sub.isNotEmpty()) {
                    Text(sub, style = type.caption, color = colors.highlightInk)
                }
            }
            Icon(TdrIcons.ChevronRight, contentDescription = null, tint = colors.highlightInk, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * 팀원 기여 막대 하나. [member]가 null이면 "이전 팀원"(나간·내보내진 팀원이 남긴 거리, M-02) 막대다.
 *
 * @property km 기여 거리(km, 단위 없음). 합계와 같은 `Format.teamKm`(소수 한 자리 내림)이라 팀원이 팀보다 많이 달린 것으로 보이지 않는다
 * @property percent 팀 거리 중 몫. 팀원은 [MemberUi.percent](S14와 같은 값), 이전 팀원은 100에서 나머지
 */
internal data class MembersChartBar(
    val member: MemberUi?,
    val distanceM: Long,
    val km: String,
    val percent: Int,
)

/** 팀원 기여 카드 값. [bars]는 팀원(기여 순) 다음에 이전 팀원(있으면) 순서. */
internal data class MembersChart(val totalKm: String, val bars: List<MembersChartBar>)

/**
 * 팀원 목록 → 기여 카드 값 (TEAM-04·EXT-03).
 *
 * - 퍼센트는 다시 계산하지 않고 [MemberUi.percent](`contributionShares`, 최대 나머지 방식)를 쓴다.
 * - 이전 팀원 몫 = 팀 거리 − 지금 팀원 기여 합. 0보다 크면 막대를 하나 더하고, 퍼센트는 100 − 팀원 퍼센트 합이라
 *   보이는 퍼센트의 합이 100이 된다(S14와 같다).
 */
internal fun membersChart(members: List<MemberUi>, teamDistanceM: Long): MembersChart {
    val memberBars = members.map { MembersChartBar(it, it.contributionM, Format.teamKm(it.contributionM), it.percent) }
    val formerM = teamDistanceM - members.sumOf { it.contributionM }
    val formerBar = if (members.isNotEmpty() && formerM > 0) {
        MembersChartBar(
            member = null,
            distanceM = formerM,
            km = Format.teamKm(formerM),
            percent = (100 - members.sumOf { it.percent }).coerceAtLeast(0),
        )
    } else {
        null
    }
    return MembersChart(totalKm = Format.teamKm(teamDistanceM), bars = memberBars + listOfNotNull(formerBar))
}

/**
 * 팀원 누적 기록 세로 막대 + 아바타. 가장 긴 막대가 140dp.
 * 나간 팀원이 남긴 거리가 있으면 회색 "이전 팀원" 막대를 끝에 더한다(S14와 같은 표시).
 */
@Composable
private fun TeamCard(team: TeamUi, members: List<MemberUi>, onClick: () -> Unit) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    TdrCard(onClick = onClick, verticalArrangement = Arrangement.spacedBy(TdrSpacing.s35)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3)) {
            TeamImage(team.appearance, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TdrSpacing.s05)) {
                Text(team.name, style = type.title.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.home_team_summary, team.season, team.memberCount, Format.teamKm(team.distanceM)),
                    style = type.caption,
                    color = colors.inkMuted,
                )
            }
        }
        // 팀원 목록을 받기 전에는 막대만 숨긴다.
        if (members.isNotEmpty()) MembersChart(members, team.distanceM)
        Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_team_detail), style = type.captionBold, color = colors.inkMuted)
            Icon(TdrIcons.ChevronRight, contentDescription = null, tint = colors.inkSubtle, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MembersChart(members: List<MemberUi>, teamDistanceM: Long) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val chart = membersChart(members, teamDistanceM)
    val maxM = chart.bars.maxOfOrNull { it.distanceM }?.coerceAtLeast(1L) ?: 1L
    val formerName = stringResource(R.string.home_members_former)
    val chartDesc = stringResource(
        R.string.home_members_chart_desc,
        // map은 inline이라 안에서 stringResource를 부를 수 있다 (joinToString 람다는 안 된다)
        chart.bars.map { bar ->
            stringResource(R.string.home_members_chart_item, bar.member?.nickname ?: formerName, bar.km, bar.percent)
        }.joinToString(", "),
    )
    Column(verticalArrangement = Arrangement.spacedBy(TdrSpacing.s35)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = TdrSpacing.s2)
                .clearAndSetSemantics { contentDescription = chartDesc },
            horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s2),
            verticalAlignment = Alignment.Bottom,
        ) {
            chart.bars.forEach { bar ->
                val m = bar.member
                // 이전 팀원은 S14처럼 회색, 얼굴은 무표정, 이름은 흐리게
                val barColor = m?.color ?: TeamColor.GRAY
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (m?.isMvp == true) {
                        Icon(TdrIcons.Crown, contentDescription = null, tint = colors.mvp, modifier = Modifier.padding(bottom = TdrSpacing.s05).size(18.dp))
                    }
                    Text(
                        bar.km + stringResource(R.string.unit_km),
                        style = type.label.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (m == null) colors.inkMuted else Color.Unspecified,
                        modifier = Modifier.padding(bottom = TdrSpacing.s1),
                    )
                    Box(
                        Modifier
                            .width(40.dp)
                            .height((140f * bar.distanceM / maxM).dp)
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                            .background(barColor.color),
                    )
                    MemberAvatar(
                        barColor,
                        size = 52.dp,
                        face = if (m == null) MascotFace.Plain else MascotFace.Smile,
                        modifier = Modifier.padding(top = TdrSpacing.s15),
                    )
                    Text(
                        m?.nickname ?: formerName,
                        style = type.label.copy(fontWeight = FontWeight.Bold),
                        color = if (m == null) colors.inkMuted else Color.Unspecified,
                        maxLines = 1,
                        modifier = Modifier.padding(top = TdrSpacing.s1),
                    )
                    Text(stringResource(R.string.percent_value, bar.percent), style = type.micro, color = colors.inkMuted)
                }
            }
        }
        if (members.any { it.isMvp }) {
            Text(stringResource(R.string.home_mvp_hint), style = type.micro, color = colors.inkMuted)
        }
    }
}

@Preview(name = "팀 카드 · 이전 팀원 있음", widthDp = 390)
@Composable
private fun TeamCardFormerPreview() {
    TdrTheme {
        // 팀 거리 66.8km 중 두 명이 50.8km, 나간 팀원이 16.0km
        TeamCard(
            team = PreviewSamples.team.copy(memberCount = 2, distanceM = 66_800),
            members = listOf(
                MemberUi("u1", "민지", TeamColor.BLUE, 30_800, isLeader = true, isMvp = true, percent = 46),
                MemberUi("u2", "서준", TeamColor.NAVY, 20_000, percent = 30),
            ),
            onClick = {},
        )
    }
}

@Preview(widthDp = 390, heightDp = 1080)
@Composable
private fun HomeScreenPreview() {
    TabScreenPreview(TdrTab.Home) {
        HomeContent(
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
            {}, {}, {}, {},
        )
    }
}

/** 빈 상태: 막 만든 1인 팀. 경쟁 팀·팀원 카드 없음, 이번 주 첫 러닝 전. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun HomeScreenEmptyPreview() {
    TabScreenPreview(TdrTab.Home) {
        HomeContent(
            HomeUiState(
                team = PreviewSamples.team.copy(memberCount = 1, distanceM = 0),
                stage = stageProgressUi(0, routeFor(1)),
                rank = 1,
                totalTeams = 1,
                ahead = null,
                behind = null,
                weekly = WeeklySummaryUi(0, 0, null, null, null),
                members = emptyList(),
            ),
            {}, {}, {}, {},
        )
    }
}

@Preview(name = "이번 주 통계 실패", widthDp = 390, heightDp = 844)
@Composable
private fun HomeScreenWeeklyFailedPreview() {
    TabScreenPreview(TdrTab.Home) {
        HomeContent(
            HomeUiState(
                team = PreviewSamples.team,
                stage = PreviewSamples.stage,
                rank = 2,
                totalTeams = 5,
                ahead = PreviewSamples.rivals[0],
                behind = PreviewSamples.rivals[1],
                weekly = null,
                members = emptyList(),
                weeklyError = DataError.Unavailable,
            ),
            {}, {}, {}, {},
        )
    }
}

@Preview(name = "불러오기 실패", widthDp = 390, heightDp = 844)
@Composable
private fun HomeFailedPreview() {
    TabScreenPreview(TdrTab.Home) {
        HomeFailed(DataError.PermissionDenied, onRetry = {})
    }
}
