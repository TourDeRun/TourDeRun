package com.tourderun.feature.collection.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tourderun.R
import com.tourderun.core.designsystem.component.Pill
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

data class LandmarkUi(val name: String, val description: String)

/** S18 지역 카드 상세 상태 (EXT-01, P1). 소개·랜드마크 문구는 공식 자료로 확인한 값만 넣는다. */
data class CardDetailUiState(
    val name: String,
    val nameEn: String,
    /** 출발 도시면 null. */
    val stageNumber: Int?,
    val startCity: String,
    val distanceFromStartKm: Int,
    val description: String,
    val landmarks: List<LandmarkUi>,
    val photoCredit: String,
    /** "11월 4일" */
    val acquiredDateText: String,
    val teamName: String,
)

/** S18 지역 카드 상세. 사진은 아직 자리표시다 (가로 1080px WebP 예정). */
@Composable
fun CardDetailScreen(
    uiState: CardDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bgSurface)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth().height(300.dp).background(colors.bgMap)) {
            Text(
                stringResource(R.string.card_photo_placeholder),
                style = type.label.copy(fontWeight = FontWeight.Normal),
                color = colors.inkMuted,
                modifier = Modifier.align(Alignment.Center),
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = TdrSpacing.s3, top = TdrSpacing.s3)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
            ) {
                Icon(TdrIcons.Back, contentDescription = stringResource(R.string.action_back), tint = colors.ink, modifier = Modifier.size(22.dp))
            }
            Text(
                stringResource(R.string.card_photo_credit, uiState.photoCredit),
                style = type.micro.copy(fontSize = 10.sp, lineHeight = 12.sp),
                color = colors.inkMuted,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = TdrSpacing.s3, bottom = 10.dp),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(TdrSpacing.s5),
            verticalArrangement = Arrangement.spacedBy(TdrSpacing.s4),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Pill(
                    if (uiState.stageNumber != null) {
                        stringResource(R.string.card_stage_badge, uiState.stageNumber, uiState.startCity, uiState.distanceFromStartKm)
                    } else {
                        stringResource(R.string.card_start_badge)
                    },
                    style = type.micro.copy(lineHeight = 16.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    buildAnnotatedString {
                        append(uiState.name)
                        append(" ")
                        withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.inkMuted)) {
                            append(uiState.nameEn)
                        }
                    },
                    style = type.statLg.copy(letterSpacing = 0.sp),
                    modifier = Modifier.semantics { heading() },
                )
            }
            Text(uiState.description, style = type.body.copy(fontSize = 14.sp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.card_landmarks), style = type.subtitle)
                uiState.landmarks.forEachIndexed { i, landmark ->
                    Row(horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3)) {
                        Box(
                            Modifier.size(24.dp).background(colors.brand, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text((i + 1).toString(), style = type.caption.copy(fontWeight = FontWeight.ExtraBold), color = colors.inkOnBrand)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(landmark.name, style = type.bodySm.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                            Text(landmark.description, style = type.label.copy(fontWeight = FontWeight.Normal), color = colors.inkMuted)
                        }
                    }
                }
            }
            Spacer(Modifier.height(TdrSpacing.s2))
            Text(
                stringResource(R.string.card_acquired, uiState.acquiredDateText, uiState.teamName),
                style = type.caption.copy(lineHeight = 18.sp),
                color = colors.inkMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TdrShapes.md)
                    .background(colors.bgSubtle)
                    .padding(horizontal = 14.dp, vertical = TdrSpacing.s3),
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CardDetailScreenPreview() {
    TdrTheme {
        CardDetailScreen(
            CardDetailUiState(
                name = "오산",
                nameEn = "Osan",
                stageNumber = 2,
                startCity = "서울",
                distanceFromStartKm = 48,
                description = "[지역 소개 2~3문장 · 지자체·관광 공식 자료로 사실 확인 후 작성]",
                landmarks = listOf(
                    LandmarkUi("[랜드마크 이름]", "[한두 문장 설명]"),
                    LandmarkUi("[랜드마크 이름]", "[한두 문장 설명]"),
                ),
                photoCredit = "[출처 표기]",
                acquiredDateText = "11월 4일",
                teamName = "뛰뛰크루",
            ),
            onBack = {},
        )
    }
}
