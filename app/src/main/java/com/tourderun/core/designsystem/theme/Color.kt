package com.tourderun.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 디자인 시스템 「Tour De Run」(ttwi) 색 토큰.
 * 이름은 tokens.json과 같다 (bg-canvas → bgCanvas). 화면 코드는 hex 대신 이 값을 쓴다.
 * 다크는 러닝 화면(S09)에서만 쓴다.
 */
@Immutable
data class TdrColors(
    val bgCanvas: Color,
    val bgSurface: Color,
    val bgSubtle: Color,
    val bgMap: Color,
    /** 카드·목록 구분선. 장식용(1.2:1)이라 컨트롤의 유일한 테두리로 쓰지 않는다. */
    val border: Color,
    /** 입력창·외곽선 버튼 테두리 (3:1). */
    val borderControl: Color,
    val scrim: Color,
    val ink: Color,
    val inkMuted: Color,
    /** 플레이스홀더·아이콘 전용. 본문 글자에 쓰지 않는다. */
    val inkSubtle: Color,
    val inkOnBrand: Color,
    /** 남색 카드(brandStrong) 위 보조 글자. */
    val inkOnBrandMuted: Color,
    val brand: Color,
    val brandStrong: Color,
    val brandSoft: Color,
    /** 큰 숫자·강조 글자. 다크에서 더 밝다. */
    val brandBright: Color,
    /** 진행 바 보조 채움, 지난주 막대 등 옅은 파랑. */
    val brandLight: Color,
    val actionJoin: Color,
    val actionJoinSoft: Color,
    val highlightSoft: Color,
    val highlightInk: Color,
    val danger: Color,
    /** 지도 위 남은 길. */
    val routeRemaining: Color,
    /** 지도 자리표시의 도로. */
    val mapRoad: Color,
    /** 잠긴 지역 카드 바탕. */
    val locked: Color,
    /** GPS 정상 점. */
    val live: Color,
    /** MVP 왕관. */
    val mvp: Color,
    val isDark: Boolean,
)

val LightTdrColors = TdrColors(
    bgCanvas = Color(0xFFF3F6FB),
    bgSurface = Color(0xFFFFFFFF),
    bgSubtle = Color(0xFFF5F6F8),
    bgMap = Color(0xFFE8F1FB),
    border = Color(0xFFE3E8F2),
    borderControl = Color(0xFF8A909C),
    scrim = Color(0x850B1730),
    ink = Color(0xFF171717),
    inkMuted = Color(0xFF5F6672),
    inkSubtle = Color(0xFF8A909C),
    inkOnBrand = Color(0xFFFFFFFF),
    inkOnBrandMuted = Color(0xFFC9D3E6),
    brand = Color(0xFF1D5FE0),
    brandStrong = Color(0xFF0E2A5C),
    brandSoft = Color(0xFFE6EEFC),
    brandBright = Color(0xFF1D5FE0),
    brandLight = Color(0xFF9DBEF7),
    actionJoin = Color(0xFF0B6B76),
    actionJoinSoft = Color(0xFFDDF4F6),
    highlightSoft = Color(0xFFFFF4D6),
    highlightInk = Color(0xFF8A6100),
    danger = Color(0xFFD6342C),
    routeRemaining = Color(0xFFB7C4D8),
    mapRoad = Color(0xFFD6DDE8),
    locked = Color(0xFFECEEF2),
    live = Color(0xFF3DDC84),
    mvp = Color(0xFFD9A400),
    isDark = false,
)

val DarkTdrColors = LightTdrColors.copy(
    bgCanvas = Color(0xFF0B1730),
    bgSurface = Color(0xFF12213F),
    bgSubtle = Color(0xFF16264A),
    bgMap = Color(0xFF16264A),
    border = Color(0xFF1E3157),
    borderControl = Color(0xFF5B76AF),
    scrim = Color(0x99000000),
    ink = Color(0xFFF3F6FB),
    inkMuted = Color(0xFF9AA3B5),
    brandSoft = Color(0xFF1E3157),
    brandBright = Color(0xFF5B9BFF),
    actionJoinSoft = Color(0xFF123A44),
    highlightSoft = Color(0xFF3A2F12),
    highlightInk = Color(0xFFFFD27A),
    danger = Color(0xFFFF6B62),
    mapRoad = Color(0xFF1E3157),
    isDark = true,
)
