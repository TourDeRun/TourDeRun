package com.tourderun.feature.journey.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
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
    /** 이번에 함께 얻은 카드 수(이 카드 포함). 한 번에 두 도시 이상 지나면 2 이상이고, 연출은 마지막 카드만 보인다. */
    val count: Int = 1,
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
 * JRN-05 스테이지 클리어. 홈·러닝 결과(S10) 위에 전체 화면으로 뜬다.
 * 배경은 brand 고정이고 글자는 흰색이다.
 *
 * 뜰 때 짧은 등장 연출이 있다: 배지 → 캐릭터(튀어 오름) → 제목·다음 스테이지 → 카드(기울며 올라옴) → 버튼 순으로
 * 아래에서 떠오르며 나타난다(약 1초). 기기 설정에서 애니메이션을 끄면 Compose가 바로 끝낸다.
 *
 * @param animate false면 처음부터 다 보인다(Preview·테스트용)
 */
@Composable
fun StageClearScreen(
    uiState: StageClearUiState,
    onViewCard: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val onBrand = colors.inkOnBrand
    var entered by remember { mutableStateOf(!animate) }
    LaunchedEffect(Unit) { entered = true }
    Box(modifier.fillMaxSize().background(colors.brand)) {
        Confetti(Modifier.fillMaxWidth().height(300.dp).enter(entered, delayMs = 0))
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
                    modifier = Modifier.enter(entered, delayMs = 0),
                )
                Mascot(
                    size = 150.dp,
                    frame = MascotFrame.None,
                    bandColor = colors.brandStrong,
                    face = MascotFace.Joy,
                    contentDescription = stringResource(R.string.clear_mascot_desc),
                    modifier = Modifier.popIn(entered),
                )
                Column(
                    Modifier.enter(entered, delayMs = STEP_MS * 2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
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
                    RegionCardMini(card, Modifier.cardIn(entered))
                    Text(
                        if (card.count > 1) stringResource(R.string.clear_cards_got, card.count) else stringResource(R.string.clear_card_got),
                        style = type.caption,
                        color = colors.brandSoft,
                    )
                }
            }
            Spacer(Modifier.height(TdrSpacing.s4))
            Row(
                Modifier.fillMaxWidth().enter(entered, delayMs = STEP_MS * 4),
                horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3),
            ) {
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

/** 등장 연출 한 단계 간격(ms). */
private const val STEP_MS = 150

/** 한 요소가 나타나는 시간(ms). */
private const val ENTER_MS = 400

/** 아래에서 떠오르며 나타난다. [delayMs] 뒤에 시작. */
@Composable
private fun Modifier.enter(entered: Boolean, delayMs: Int): Modifier {
    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = ENTER_MS, delayMillis = delayMs),
        label = "enter",
    )
    val riseOffsetPx = with(LocalDensity.current) { TdrSpacing.s6.toPx() }
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * riseOffsetPx
    }
}

/** 캐릭터: 작게 시작해 튀어 오르며 커진다. */
@Composable
private fun Modifier.popIn(entered: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "popInScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = ENTER_MS, delayMillis = STEP_MS),
        label = "popInAlpha",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}

/** 카드: 더 기울어진 채 아래에서 올라와 제자리(-3도)에 놓인다. */
@Composable
private fun Modifier.cardIn(entered: Boolean): Modifier {
    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "cardIn",
    )
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = ENTER_MS, delayMillis = STEP_MS * 3),
        label = "cardInAlpha",
    )
    val riseOffsetPx = with(LocalDensity.current) { TdrSpacing.s8.toPx() * 2 }
    return graphicsLayer {
        this.alpha = alpha
        translationY = (1f - progress) * riseOffsetPx
        rotationZ = CARD_START_TILT * (1f - progress)
    }
}

/** 카드가 올라오기 시작할 때 더 기울어진 각도(도). 제자리 -3도에 더해진다. */
private const val CARD_START_TILT = -12f

/** 기울어진 작은 지역 카드. 사진은 아직 자리표시. */
@Composable
private fun RegionCardMini(card: EarnedCardUi, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    val colors = TdrTheme.colors
    Column(
        modifier
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
            animate = false,
        )
    }
}

@Preview(name = "카드 2장 · 완주", widthDp = 390, heightDp = 844)
@Composable
private fun StageClearScreenFinishPreview() {
    TdrTheme {
        StageClearScreen(
            StageClearUiState(
                clearedStage = 7,
                arrivedCity = "부산",
                nextStage = null,
                nextCity = null,
                nextStageKm = null,
                card = EarnedCardUi("부산", "Busan", 8, 8, count = 2),
            ),
            {}, {},
            animate = false,
        )
    }
}

@Preview(name = "카드 없음", widthDp = 390, heightDp = 844)
@Composable
private fun StageClearScreenNoCardPreview() {
    TdrTheme {
        StageClearScreen(
            StageClearUiState(
                clearedStage = 1,
                arrivedCity = "안양",
                nextStage = 2,
                nextCity = "오산",
                nextStageKm = 29,
                card = null,
            ),
            {}, {},
            animate = false,
        )
    }
}
