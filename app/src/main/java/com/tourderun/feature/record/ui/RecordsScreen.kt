package com.tourderun.feature.record.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrIcons
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.tourderun.core.designsystem.component.DateTile
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.component.TdrTopBar
import com.tourderun.core.designsystem.component.valueWithUnit
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format
import com.tourderun.core.ui.model.RunRowUi
import com.tourderun.core.ui.preview.PreviewSamples

/** S11 내 기록 목록 상태. 20개씩 불러온다. */
data class RecordsUiState(
    val totalRuns: Int,
    val totalDistanceM: Long,
    /** 최신순. */
    val runs: List<RunRowUi>,
    val canLoadMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** 첫 목록을 받기 전. 빈 상태 문구를 잠깐 보였다 지우지 않으려고 둔다. */
    val isLoading: Boolean = false,
) {
    companion object {
        val Loading = RecordsUiState(totalRuns = 0, totalDistanceM = 0, runs = emptyList(), isLoading = true)
    }
}

/** S11 내 기록 (프로필에서 들어온다). */
@Composable
fun RecordsScreen(
    onBack: () -> Unit,
    onRunClick: (RunRowUi) -> Unit,
    onStartRun: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecordsContent(uiState, onBack, onRunClick, viewModel::loadMore, onStartRun, modifier)
}

/** S11 화면 내용. 상태 없음(Preview 대상). 끝까지 스크롤하면 [onLoadMore]를 부른다. */
@Composable
fun RecordsContent(
    uiState: RecordsUiState,
    onBack: () -> Unit,
    onRunClick: (RunRowUi) -> Unit,
    onLoadMore: () -> Unit,
    onStartRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val listState = rememberLazyListState()
    val atEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 2
        }
    }
    LaunchedEffect(atEnd, uiState.canLoadMore, uiState.isLoadingMore) {
        if (atEnd && uiState.canLoadMore && !uiState.isLoadingMore) onLoadMore()
    }

    Column(
        modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding(),
    ) {
        TdrTopBar(
            title = stringResource(R.string.records_title),
            onBack = onBack,
            modifier = Modifier.padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).navigationBarsPadding(),
            contentPadding = PaddingValues(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3, bottom = TdrSpacing.s8),
            verticalArrangement = Arrangement.spacedBy(TdrSpacing.s3),
        ) {
            item {
                Text(
                    stringResource(R.string.records_summary, uiState.totalRuns, Format.km(uiState.totalDistanceM)),
                    style = TdrTheme.typography.caption,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
            if (uiState.runs.isEmpty() && !uiState.isLoading) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = TdrSpacing.s8),
                        verticalArrangement = Arrangement.spacedBy(TdrSpacing.s5),
                    ) {
                        Text(
                            stringResource(R.string.records_empty),
                            style = TdrTheme.typography.body,
                            color = colors.inkMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TdrButton(
                            stringResource(R.string.records_empty_action),
                            onClick = onStartRun,
                            modifier = Modifier.fillMaxWidth(),
                            icon = TdrIcons.Runner,
                        )
                    }
                }
            }
            items(uiState.runs, key = { it.runId }) { run ->
                RunRow(run, onClick = { onRunClick(run) })
            }
            if (uiState.isLoadingMore) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(Modifier.padding(TdrSpacing.s2), color = colors.brand)
                    }
                }
            }
        }
    }
}

/** 기록 한 줄: 날짜 칸 · 제목 / 시간·페이스 · 거리. 프로필의 최근 기록에서도 쓴다. */
@Composable
fun RunRow(run: RunRowUi, onClick: () -> Unit, modifier: Modifier = Modifier, inCard: Boolean = true) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val content: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3)) {
            DateTile(stringResource(R.string.month_label, run.month), run.day.toString())
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(run.title, style = type.subtitle.copy(lineHeight = 20.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    Format.duration(run.movingSec) + " · " + stringResource(R.string.pace_per_km, Format.pace(run.paceSecPerKm)),
                    style = type.caption,
                    color = colors.inkMuted,
                )
            }
            Text(
                valueWithUnit(Format.km(run.distanceM, decimals = 2), stringResource(R.string.unit_km), unitSize = 11, unitColor = colors.inkMuted),
                style = type.subtitle.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold),
            )
        }
    }
    if (inCard) {
        TdrCard(modifier, onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) { content() }
    } else {
        content()
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RecordsScreenPreview() {
    TdrTheme {
        RecordsContent(RecordsUiState(21, 86_400, PreviewSamples.runs, canLoadMore = true), {}, {}, {}, {})
    }
}

@Preview(name = "기록 없음", widthDp = 390, heightDp = 844)
@Composable
private fun RecordsScreenEmptyPreview() {
    TdrTheme {
        RecordsContent(RecordsUiState(0, 0, emptyList()), {}, {}, {}, {})
    }
}
