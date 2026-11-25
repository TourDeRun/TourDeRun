package com.tourderun.feature.collection.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tourderun.R
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.component.TdrTopBar
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.data.card.OwnedCard
import com.tourderun.core.ui.Format
import com.tourderun.core.ui.LoadFailed
import com.tourderun.core.ui.loadFailedMessageRes
import com.tourderun.domain.route.routeFor

/** 지역 카드 상태. */
sealed interface RegionCardStatus {
    /** 얻은 카드. [dateText] = "10월 28일". 받은 시각을 모르면 null(날짜 없이 "획득"). */
    data class Collected(val dateText: String?) : RegionCardStatus

    /** 다음에 얻을 카드. */
    data class Next(val remainingM: Long) : RegionCardStatus

    /** 아직 없는 카드. [stageNumber]가 0이면 출발 도시. */
    data class Locked(val stageNumber: Int, val cumulativeKm: Int) : RegionCardStatus

    /** 팀에 들어오기 전에 팀이 이미 지나간 도시. 카드 대상이 아니라 받을 수 없다(EXT-01). */
    data object Passed : RegionCardStatus
}

data class RegionCardUi(
    val cityId: String,
    val name: String,
    val status: RegionCardStatus,
)

/** S17 지역 도감 상태 (EXT-01, P1). */
data class CollectionUiState(
    val routeFrom: String,
    val routeTo: String,
    /** 경로 순서대로 (출발 도시 포함 8장). */
    val cards: List<RegionCardUi>,
) {
    val collected: Int get() = cards.count { it.status is RegionCardStatus.Collected }
}

/**
 * S17 지역 도감 (EXT-01). 상태는 [CollectionViewModel]. 얻은 카드만 눌러서 상세(S18)를 연다.
 * 불러오는 중이면 상단 바만, 실패면 다시 시도를 보인다.
 */
@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    onCardClick: (RegionCardUi) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val load by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = load) {
        CollectionLoad.Loading -> CollectionFrame(onBack, modifier) {}
        is CollectionLoad.Failed -> CollectionFrame(onBack, modifier) {
            LoadFailed(state.error.loadFailedMessageRes(), viewModel::retry)
        }
        is CollectionLoad.Content -> CollectionContent(state.ui, onBack, onCardClick, modifier)
    }
}

/** 상단 바 + 내용. 불러오는 중·실패에도 뒤로 갈 수 있게 상단 바를 먼저 그린다. */
@Composable
private fun CollectionFrame(onBack: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .background(TdrTheme.colors.bgCanvas)
            .statusBarsPadding(),
    ) {
        TdrTopBar(
            title = stringResource(R.string.collection_title),
            onBack = onBack,
            modifier = Modifier.padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3),
        )
        content()
    }
}

/** S17 화면 내용. 상태 없음(Preview 대상). */
@Composable
fun CollectionContent(
    uiState: CollectionUiState,
    onBack: () -> Unit,
    onCardClick: (RegionCardUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    CollectionFrame(onBack, modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).navigationBarsPadding(),
            contentPadding = PaddingValues(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3, bottom = TdrSpacing.s8),
            horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3),
            verticalArrangement = Arrangement.spacedBy(TdrSpacing.s3),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.padding(horizontal = 2.dp), verticalAlignment = Alignment.Bottom) {
                    Text(
                        stringResource(R.string.collection_count, uiState.collected, uiState.cards.size),
                        style = type.statSm,
                        modifier = Modifier.weight(1f),
                    )
                    Text(stringResource(R.string.route_format, uiState.routeFrom, uiState.routeTo), style = type.caption, color = colors.inkMuted)
                }
            }
            items(uiState.cards, key = { it.cityId }) { card ->
                RegionCardTile(card, onClick = { onCardClick(card) })
            }
        }
    }
}

@Composable
private fun RegionCardTile(card: RegionCardUi, onClick: () -> Unit) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val status = card.status
    val got = status is RegionCardStatus.Collected
    val next = status is RegionCardStatus.Next
    Column(
        Modifier
            .then(if (got) Modifier.shadow(1.dp, TdrShapes.lg) else Modifier)
            .clip(TdrShapes.lg)
            .background(colors.bgSurface)
            .then(if (next) Modifier.border(2.dp, colors.brand, TdrShapes.lg) else Modifier)
            .then(if (got) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(92.dp)
                .background(
                    when {
                        got -> colors.bgMap
                        next -> colors.brandSoft
                        else -> colors.locked
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (got) {
                Text(stringResource(R.string.card_photo_placeholder), style = type.micro, color = colors.inkMuted)
            } else {
                Icon(
                    TdrIcons.Lock,
                    contentDescription = stringResource(R.string.collection_locked_desc),
                    tint = if (next) colors.brand else colors.inkSubtle,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Column(Modifier.padding(horizontal = TdrSpacing.s3, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                card.name,
                style = type.subtitle.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 20.sp),
                color = if (got || next) colors.ink else colors.inkMuted,
            )
            Text(
                when (status) {
                    is RegionCardStatus.Collected -> status.dateText?.let { stringResource(R.string.collection_got, it) }
                        ?: stringResource(R.string.collection_got_no_date)
                    is RegionCardStatus.Next -> stringResource(R.string.collection_next, Format.kmUp(status.remainingM))
                    is RegionCardStatus.Locked -> if (status.stageNumber == 0) {
                        stringResource(R.string.card_start_badge)
                    } else {
                        stringResource(R.string.collection_locked, status.stageNumber, status.cumulativeKm)
                    }
                    RegionCardStatus.Passed -> stringResource(R.string.collection_passed)
                },
                style = type.micro,
                color = if (next) colors.brand else colors.inkMuted,
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun CollectionContentPreview() {
    TdrTheme {
        CollectionContent(
            CollectionUiState(
                "서울",
                "부산",
                listOf(
                    RegionCardUi("seoul", "서울", RegionCardStatus.Collected("10월 28일")),
                    RegionCardUi("anyang", "안양", RegionCardStatus.Collected("10월 31일")),
                    RegionCardUi("osan", "오산", RegionCardStatus.Collected("11월 4일")),
                    RegionCardUi("cheonan", "천안", RegionCardStatus.Next(18_200)),
                    RegionCardUi("sejong", "세종", RegionCardStatus.Locked(4, 125)),
                    RegionCardUi("yeongdong", "영동", RegionCardStatus.Locked(5, 181)),
                    RegionCardUi("daegu", "대구", RegionCardStatus.Locked(6, 262)),
                    RegionCardUi("busan", "부산", RegionCardStatus.Locked(7, 350)),
                ),
            ),
            {}, {},
        )
    }
}

@Preview(name = "카드 없음", widthDp = 390, heightDp = 900)
@Composable
private fun CollectionContentEmptyPreview() {
    TdrTheme {
        CollectionContent(collectionUiState(routeFor(1), owned = emptyList(), teamDistanceM = 0), {}, {})
    }
}

@Preview(name = "중간 참여(지나간 도시)", widthDp = 390, heightDp = 900)
@Composable
private fun CollectionContentJoinedLatePreview() {
    TdrTheme {
        CollectionContent(
            collectionUiState(routeFor(1), owned = listOf(OwnedCard("seoul", 0, "뛰뛰크루")), teamDistanceM = 66_800),
            {}, {},
        )
    }
}
