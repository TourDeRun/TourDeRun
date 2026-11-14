package com.tourderun.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * 디자인 시스템 타이포 토큰 (tokens.json type.groups).
 * 서체는 Pretendard가 기준이지만 아직 폰트 파일을 넣지 않아 시스템 기본(한글은 Noto Sans KR 계열)을 쓴다.
 * TODO(UI-01): Pretendard 폰트 리소스를 넣으면 [TdrFontFamily]만 바꾼다.
 */
val TdrFontFamily: FontFamily = FontFamily.Default

private fun style(size: Int, line: Int, weight: FontWeight, letterSpacing: Double = 0.0) = TextStyle(
    fontFamily = TdrFontFamily,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = letterSpacing.em,
)

@Immutable
data class TdrTypography(
    // Numerals
    /** 러닝 화면 실시간 거리 (와이어프레임은 88sp까지 키움). */
    val runHero: TextStyle = style(88, 92, FontWeight.ExtraBold, -0.03),
    val statLg: TextStyle = style(28, 34, FontWeight.ExtraBold, -0.01),
    /** 시간·페이스·결과 숫자 (22sp). */
    val statMd: TextStyle = style(22, 28, FontWeight.ExtraBold),
    val statSm: TextStyle = style(20, 26, FontWeight.ExtraBold),
    // Text
    val display: TextStyle = style(34, 40, FontWeight.ExtraBold, -0.02),
    val titleXl: TextStyle = style(24, 32, FontWeight.ExtraBold),
    val titleLg: TextStyle = style(22, 30, FontWeight.ExtraBold),
    val title: TextStyle = style(18, 26, FontWeight.Bold),
    /** 상단바 가운데 제목. */
    val topBar: TextStyle = style(17, 24, FontWeight.Bold),
    val subtitle: TextStyle = style(15, 22, FontWeight.Bold),
    val body: TextStyle = style(15, 22, FontWeight.Normal),
    val bodySm: TextStyle = style(13, 20, FontWeight.Normal),
    val label: TextStyle = style(13, 18, FontWeight.SemiBold),
    val caption: TextStyle = style(12, 16, FontWeight.Normal),
    val micro: TextStyle = style(11, 14, FontWeight.Normal),
)
