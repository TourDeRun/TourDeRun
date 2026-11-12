package com.tourderun.feature.record.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tourderun.R
import com.tourderun.core.designsystem.component.RouteSketch
import com.tourderun.core.designsystem.component.SampleRoutePoints
import com.tourderun.core.designsystem.component.StatColumn
import com.tourderun.core.designsystem.component.StatRow
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrButtonStyle
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.component.TdrTextField
import com.tourderun.core.designsystem.component.TdrTopBar
import com.tourderun.core.designsystem.component.valueWithUnit
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format
import com.tourderun.domain.model.LatLngValue

/** S12 기록 상세 상태. 제목·메모만 고칠 수 있다. */
data class RecordDetailUiState(
    /** "2026. 11. 6 (금) 19:02" — 날짜 모양은 ViewModel이 만든다. */
    val startedAtText: String,
    val title: String,
    val memo: String,
    val distanceM: Long,
    val movingSec: Long,
    val paceSecPerKm: Long,
    val path: List<LatLngValue>,
    val hasChanges: Boolean = false,
    val isSaving: Boolean = false,
    val showDeleteConfirm: Boolean = false,
) {
    companion object {
        const val TITLE_MAX = 30
        const val MEMO_MAX = 200
    }
}

/** S12 기록 상세. 삭제는 확인 대화상자를 거친다 (팀 거리는 줄지 않는다). */
@Composable
fun RecordDetailScreen(
    uiState: RecordDetailUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    onSave: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val km = stringResource(R.string.unit_km)
    val mapDesc = stringResource(R.string.record_map_desc)
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3, bottom = TdrSpacing.s8),
    ) {
        TdrTopBar(
            title = uiState.startedAtText,
            onBack = onBack,
            titleStyle = type.label.copy(fontWeight = type.body.fontWeight),
            titleColor = colors.inkMuted,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = TdrSpacing.s3),
            verticalArrangement = Arrangement.spacedBy(TdrSpacing.s3),
        ) {
            RouteSketch(
                points = uiState.path,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(TdrShapes.lg)
                    .semantics { contentDescription = mapDesc },
            )
            TdrCard(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                TdrTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    label = stringResource(R.string.record_title_label),
                    maxLength = RecordDetailUiState.TITLE_MAX,
                    minHeight = 44.dp,
                    textStyle = type.subtitle.copy(fontSize = 16.sp),
                )
                StatRow(Modifier.padding(vertical = 4.dp)) {
                    StatColumn(valueWithUnit(Format.km(uiState.distanceM, decimals = 2), km, unitSize = 11), stringResource(R.string.stat_distance), Modifier.weight(1f), type.statSm)
                    StatColumn(AnnotatedString(Format.duration(uiState.movingSec)), stringResource(R.string.stat_time), Modifier.weight(1f), type.statSm)
                    StatColumn(AnnotatedString(Format.pace(uiState.paceSecPerKm)), stringResource(R.string.stat_avg_pace), Modifier.weight(1f), type.statSm)
                }
                TdrTextField(
                    value = uiState.memo,
                    onValueChange = onMemoChange,
                    label = stringResource(R.string.record_memo_label),
                    maxLength = RecordDetailUiState.MEMO_MAX,
                    singleLine = false,
                    minHeight = 72.dp,
                    textStyle = type.bodySm.copy(fontSize = 14.sp),
                )
                Text(stringResource(R.string.record_locked), style = type.micro, color = colors.inkMuted)
            }
        }
        Spacer(Modifier.height(TdrSpacing.s4))
        Row(horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3)) {
            TdrButton(
                stringResource(R.string.action_delete),
                onClick = onDeleteClick,
                style = TdrButtonStyle.OutlineDanger,
                modifier = Modifier.weight(1f),
            )
            TdrButton(
                stringResource(R.string.action_save),
                onClick = onSave,
                enabled = uiState.hasChanges && !uiState.isSaving,
                modifier = Modifier.weight(2f),
            )
        }
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text(stringResource(R.string.record_delete_title), style = type.title) },
            text = { Text(stringResource(R.string.record_delete_body), style = type.body, color = colors.inkMuted) },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm) {
                    Text(stringResource(R.string.action_delete), color = colors.danger, style = type.subtitle)
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.ink, style = type.subtitle, textAlign = TextAlign.Center)
                }
            },
            containerColor = colors.bgSurface,
            shape = TdrShapes.lg,
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RecordDetailScreenPreview() {
    TdrTheme {
        RecordDetailScreen(
            RecordDetailUiState(
                startedAtText = "2026. 11. 6 (금) 19:02",
                title = "11월 6일 저녁 러닝",
                memo = "비 그치고 시원했음. 마지막 1km 페이스 올림",
                distanceM = 3_240,
                movingSec = 1_122,
                paceSecPerKm = 346,
                path = SampleRoutePoints,
            ),
            {}, {}, {}, {}, {}, {}, {},
        )
    }
}
