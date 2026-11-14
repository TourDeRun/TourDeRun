package com.tourderun.feature.journey.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tourderun.R
import com.tourderun.core.designsystem.component.Mascot
import com.tourderun.core.designsystem.component.MascotFace
import com.tourderun.core.designsystem.component.MascotFrame
import com.tourderun.core.designsystem.component.Pill
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSize
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

/** 이번 도착으로 얻은 지역 카드 (EXT-01, P1). */
data class EarnedCardUi(
    val cityName: String,
    val cityNameEn: String,
    /** 1부터. 출발 도시 포함 순번. */
    val index: Int,
    val total: Int,
)

/** JRN-05 스테이지 클리어 연출 상태. */
data class StageClearUiState(
    val clearedStage: Int,
    val arrivedCity: String,
    /** 다음 스테이지. 완주면 null. */
    val nextStage: Int?,
    val nextCity: String?,
    val nextStageKm: Int?,
    /** 지역 카드 기능(P1)이 꺼져 있으면 null. */
    val card: EarnedCardUi?,
)

/**
 * JRN-05 스테이지 클리어. 러닝 결과(S10) 위에 전체 화면으로 뜬다.
 * 배경은 brand 고정이고 글자는 흰색이다.
 */
@Composable
fun StageClearScreen(
    uiState: StageClearUiState,
    onViewCard: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val onBrand = colors.inkOnBrand
    Box(modifier.fillMaxSize().background(colors.brand)) {
        Confetti(Modifier.fillMaxWidth().height(300.dp))
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = TdrSpacing.s6, end = TdrSpacing.s6, top = 48.dp, bottom = TdrSpacing.s8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TdrSpacing.s5),
            ) {
                Pill(
                    stringResource(R.string.clear_badge, uiState.clearedStage),
                    background = Color.White.copy(alpha = 0.16f),
                    contentColor = onBrand,
                    style = type.label.copy(fontWeight = FontWeight.Bold),
                )
                Mascot(
                    size = 150.dp,
                    frame = MascotFrame.None,
                    bandColor = colors.brandStrong,
                    face = MascotFace.Joy,
                    contentDescription = stringResource(R.string.clear_mascot_desc),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.clear_title, uiState.arrivedCity),
                        style = type.display.copy(fontSize = 32.sp, letterSpacing = 0.sp),
                        color = onBrand,
                        modifier = Modifier.semantics { heading() },
                    )
                    val next = if (uiState.nextStage != null && uiState.nextCity != null && uiState.nextStageKm != null) {
                        stringResource(R.string.clear_next, uiState.nextStage, uiState.nextCity, uiState.nextStageKm)
                    } else {
                        stringResource(R.string.clear_finished)
                    }
                    Text(next, style = type.body.copy(fontSize = 16.sp, lineHeight = 24.sp), color = onBrand, textAlign = TextAlign.Center)
                }
                uiState.card?.let { card ->
                    RegionCardMini(card)
                    Text(stringResource(R.string.clear_card_got), style = type.caption, color = colors.brandSoft)
                }
            }
            Spacer(Modifier.height(TdrSpacing.s4))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3)) {
                if (uiState.card != null) {
                    ClearButton(stringResource(R.string.clear_view_card), onViewCard, Color.White.copy(alpha = 0.16f), onBrand, Modifier.weight(1f))
                }
                ClearButton(stringResource(R.string.action_confirm), onConfirm, Color.White, colors.brand, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ClearButton(text: String, onClick: () -> Unit, container: Color, content: Color, modifier: Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(TdrSize.buttonLg),
        shape = TdrShapes.md,
        color = container,
        contentColor = content,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = TdrTheme.typography.subtitle)
        }
    }
}

/** 기울어진 작은 지역 카드. 사진은 아직 자리표시. */
@Composable
private fun RegionCardMini(card: EarnedCardUi) {
    val shape = RoundedCornerShape(20.dp)
    val colors = TdrTheme.colors
    Column(
        Modifier
            .rotate(-3f)
            .width(168.dp)
            .shadow(16.dp, shape)
            .clip(shape)
            .background(colors.bgSurface),
    ) {
        Box(
            Modifier.fillMaxWidth().height(120.dp).background(colors.bgMap),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.card_photo_placeholder), style = TdrTheme.typography.caption, color = colors.inkMuted)
        }
        Column(Modifier.padding(TdrSpacing.s3), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(card.cityName, style = TdrTheme.typography.title.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp), color = colors.ink)
            Text(
                stringResource(R.string.clear_card_sub, card.cityNameEn, card.index, card.total),
                style = TdrTheme.typography.micro,
                color = colors.inkMuted,
            )
        }
    }
}

/** 배경 색종이 (장식). */
@Composable
private fun Confetti(modifier: Modifier) {
    val cream = TdrTheme.colors.highlightSoft
    val light = TdrTheme.colors.brandLight
    Canvas(modifier) {
        val sx = size.width / 390f
        val sy = size.height / 300f
        drawCircle(cream, 6 * sx, Offset(40 * sx, 60 * sy))
        drawCircle(light, 5 * sx, Offset(340 * sx, 90 * sy))
        drawCircle(cream, 4 * sx, Offset(300 * sx, 40 * sy))
        drawCircle(light, 4 * sx, Offset(80 * sx, 150 * sy))
        drawCircle(cream, 5 * sx, Offset(318 * sx, 184 * sy))
        drawCircle(cream, 7 * sx, Offset(64 * sx, 234 * sy))
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun StageClearScreenPreview() {
    TdrTheme {
        StageClearScreen(
            StageClearUiState(
                clearedStage = 3,
                arrivedCity = "천안",
                nextStage = 4,
                nextCity = "세종",
                nextStageKm = 40,
                card = EarnedCardUi("천안", "Cheonan", 4, 8),
            ),
            {}, {},
        )
    }
}
