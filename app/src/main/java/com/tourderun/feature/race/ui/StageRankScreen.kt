package com.tourderun.feature.race.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tourderun.R
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.component.TdrTopBar
import com.tourderun.core.designsystem.component.TeamAppearance
import com.tourderun.core.designsystem.component.TeamImage
import com.tourderun.core.designsystem.component.valueWithUnit
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format
import com.tourderun.core.ui.model.StageProgressUi
import com.tourderun.core.ui.preview.PreviewSamples

/** 순위 한 줄. 다른 팀은 이름·이미지·인원·거리만 보인다. */
data class RankRowUi(
    val rank: Int,
    val name: String,
    val appearance: TeamAppearance,
    val memberCount: Int,
    val distanceM: Long,
    val isOurTeam: Boolean,
)

/** S15 스테이지 순위 상태 (RACE-01·02). */
data class StageRankUiState(
    val stage: StageProgressUi,
    val ourRank: Int,
    /** 같은 스테이지 팀, 순위순 (같은 거리면 먼저 도달한 팀이 앞). */
    val rows: List<RankRowUi>,
)

/** S15 스테이지 순위. 홈의 스테이지 경쟁 카드에서 들어온다. */
@Composable
fun StageRankScreen(
    uiState: StageRankUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val stage = uiState.stage
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
        TdrTopBar(title = stringResource(R.string.rank_title), onBack = onBack)

        TdrCard(color = colors.brandStrong, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.rank_header_range, stage.stageNumber, stage.startKm, stage.endKm),
                style = type.caption,
                color = colors.inkOnBrandMuted,
            )
            Text(
                stringResource(R.string.rank_header_title, stage.fromCity, stage.toCity, uiState.rows.size, uiState.ourRank),
                style = type.titleLg.copy(lineHeight = 28.sp),
                color = colors.inkOnBrand,
            )
        }

        TdrCard(contentPadding = PaddingValues(0.dp), verticalArrangement = Arrangement.Top) {
            uiState.rows.forEachIndexed { i, row ->
                val gap = if (i == 0) {
                    stringResource(R.string.rank_leader)
                } else {
                    stringResource(R.string.rank_gap, Format.km(uiState.rows[i - 1].distanceM - row.distanceM))
                }
                RankRow(row, gap)
                if (i < uiState.rows.lastIndex) HorizontalDivider(color = colors.border)
            }
        }
        Text(
            stringResource(R.string.rank_notice),
            style = type.micro.copy(lineHeight = 16.sp),
            color = colors.inkMuted,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
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
        TeamImage(row.appearance, size = 40.dp)
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

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun StageRankScreenPreview() {
    val r = PreviewSamples.rivals
    val t = PreviewSamples.team
    TdrTheme {
        StageRankScreen(
            StageRankUiState(
                stage = PreviewSamples.stage,
                ourRank = 2,
                rows = listOf(
                    RankRowUi(1, r[0].name, r[0].appearance, r[0].memberCount, r[0].distanceM, false),
                    RankRowUi(2, t.name, t.appearance, t.memberCount, t.distanceM, true),
                    RankRowUi(3, r[1].name, r[1].appearance, r[1].memberCount, r[1].distanceM, false),
                    RankRowUi(4, r[2].name, r[2].appearance, r[2].memberCount, r[2].distanceM, false),
                    RankRowUi(5, r[3].name, r[3].appearance, r[3].memberCount, r[3].distanceM, false),
                ),
            ),
            onBack = {},
        )
    }
}
