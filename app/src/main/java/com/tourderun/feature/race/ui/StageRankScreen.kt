package com.tourderun.feature.race.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tourderun.R
import com.tourderun.core.designsystem.component.Pill
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.component.TdrTopBar
import com.tourderun.core.designsystem.component.TeamAppearance
import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.designsystem.component.TeamIcon
import com.tourderun.core.designsystem.component.TeamImage
import com.tourderun.core.designsystem.component.valueWithUnit
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format

/** S15 스테이지 순위. 홈의 스테이지 경쟁 카드에서 들어온다. */
@Composable
fun StageRankScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StageRankViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StageRankContent(uiState, onBack, modifier)
}

/** S15 화면 내용. 상태 없음(Preview 대상). */
@Composable
fun StageRankContent(
    uiState: StageRankUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
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
        TdrTopBar(title = stringResource(R.string.rank_title), onBack = onBack) {
            if (uiState is StageRankUiState.Content && uiState.isOffline) {
                Pill(
                    stringResource(R.string.rank_offline),
                    background = colors.inkMuted,
                    contentColor = colors.inkOnBrand,
                )
            }
        }
        when (uiState) {
            StageRankUiState.Loading -> Box(
                Modifier.fillMaxWidth().padding(vertical = TdrSpacing.s8),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = colors.brand)
            }

            StageRankUiState.NoTeam -> Text(
                stringResource(R.string.rank_no_team),
                style = type.body,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = TdrSpacing.s8),
            )

            is StageRankUiState.Content -> RankBody(uiState)
        }
    }
}

@Composable
private fun RankBody(uiState: StageRankUiState.Content) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    TdrCard(color = colors.brandStrong, verticalArrangement = Arrangement.spacedBy(TdrSpacing.s1)) {
        Text(
            stringResource(R.string.rank_header_range, uiState.stageNumber, uiState.startKm, uiState.endKm),
            style = type.caption,
            color = colors.inkOnBrandMuted,
        )
        Text(
            stringResource(R.string.rank_header_title, uiState.fromCity, uiState.toCity, uiState.totalTeams, uiState.ourRank),
            style = type.titleLg.copy(lineHeight = 28.sp),
            color = colors.inkOnBrand,
        )
    }

    TdrCard(contentPadding = PaddingValues(0.dp), verticalArrangement = Arrangement.Top) {
        uiState.rows.forEachIndexed { i, row ->
            val gap = row.gapToAheadM?.let { stringResource(R.string.rank_gap, Format.km(it)) }
                ?: stringResource(R.string.rank_leader)
            RankRow(row, gap)
            if (i < uiState.rows.lastIndex) HorizontalDivider(color = colors.border)
        }
    }
    if (uiState.isAlone) {
        Text(
            stringResource(R.string.rank_alone),
            style = type.bodySm,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = TdrSpacing.s2),
        )
    }
    Text(
        stringResource(R.string.rank_notice),
        style = type.micro.copy(lineHeight = 16.sp),
        color = colors.inkMuted,
        modifier = Modifier.padding(horizontal = TdrSpacing.s1),
    )
}

@Composable
private fun RankRow(row: RankRowUi, gap: String) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (row.isOurTeam) colors.brandSoft else colors.bgSurface)
            .padding(horizontal = TdrSpacing.s4, vertical = TdrSpacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            row.rank.toString(),
            style = type.subtitle.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold),
            color = if (row.isOurTeam) colors.brand else colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
        TeamImage(teamAppearance(row.colorIndex, row.iconIndex), size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                if (row.isOurTeam) stringResource(R.string.rank_our_team, row.name) else row.name,
                style = type.subtitle.copy(lineHeight = 20.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(stringResource(R.string.rank_members, row.memberCount, gap), style = type.caption, color = colors.inkMuted)
        }
        Text(
            valueWithUnit(Format.km(row.distanceM), stringResource(R.string.unit_km), unitSize = 11, unitColor = colors.inkMuted),
            style = type.subtitle.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
        )
    }
}

/** 팀 색·아이콘 번호(M-06) → 대표 이미지. 범위 밖 번호는 기본값(파랑·러너). */
private fun teamAppearance(colorIndex: Int, iconIndex: Int) = TeamAppearance(
    color = TeamColor.entries.getOrElse(colorIndex) { TeamColor.BLUE },
    icon = TeamIcon.entries.getOrElse(iconIndex) { TeamIcon.RUNNER },
)

private fun previewRow(rank: Int, name: String, color: Int, members: Int, distanceM: Long, gapM: Long?, ours: Boolean = false) =
    RankRowUi(rank, name, iconIndex = 0, colorIndex = color, memberCount = members, distanceM = distanceM, gapToAheadM = gapM, isOurTeam = ours)

private fun previewContent(rows: List<RankRowUi>, isOffline: Boolean = false) = StageRankUiState.Content(
    stageNumber = 3,
    fromCity = "오산",
    toCity = "천안",
    startKm = 48,
    endKm = 85,
    ourRank = rows.indexOfFirst { it.isOurTeam } + 1,
    rows = rows,
    isOffline = isOffline,
)

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun StageRankContentPreview() {
    TdrTheme {
        StageRankContent(
            previewContent(
                listOf(
                    previewRow(1, "주황이들", 5, 3, 69_200, null),
                    previewRow(2, "뛰뛰크루", 0, 4, 66_800, 2_400, ours = true),
                    previewRow(3, "보라돌이", 4, 2, 65_700, 1_100),
                    previewRow(4, "새벽러너스", 3, 4, 58_300, 7_400),
                    previewRow(5, "한솔이네", 2, 1, 50_900, 7_400),
                ),
            ),
            onBack = {},
        )
    }
}

@Preview(name = "혼자 · 오프라인", widthDp = 390, heightDp = 844)
@Composable
private fun StageRankContentAlonePreview() {
    TdrTheme {
        StageRankContent(
            previewContent(listOf(previewRow(1, "뛰뛰크루", 0, 4, 66_800, null, ours = true)), isOffline = true),
            onBack = {},
        )
    }
}

@Preview(name = "불러오는 중", widthDp = 390, heightDp = 844)
@Composable
private fun StageRankContentLoadingPreview() {
    TdrTheme {
        StageRankContent(StageRankUiState.Loading, onBack = {})
    }
}
