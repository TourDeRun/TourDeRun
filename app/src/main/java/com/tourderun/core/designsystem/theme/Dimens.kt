package com.tourderun.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** 간격 토큰 (space-1 ~ space-8). 반 단계(s05·s15·s25·s35)는 글줄 사이·칩 안쪽처럼 촘촘한 곳에 쓴다. */
object TdrSpacing {
    val s05 = 2.dp
    val s1 = 4.dp
    val s15 = 6.dp
    val s2 = 8.dp
    val s25 = 10.dp
    val s3 = 12.dp
    val s35 = 14.dp
    val s4 = 16.dp
    val s5 = 20.dp
    val s6 = 24.dp
    val s8 = 32.dp

    /** 화면 좌우 여백 (space-5). */
    val screenHorizontal = s5
}

/** 모서리 토큰 (radius-sm ~ radius-full). */
object TdrShapes {
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(24.dp)
    val full = CircleShape
}

/** 컴포넌트 고정 크기. */
object TdrSize {
    /** 화면 하단 주 버튼 높이. */
    val buttonLg = 52.dp
    /** 모달 안 버튼 높이. */
    val buttonMd = 48.dp
    /** 터치 영역 최소. */
    val touch = 44.dp
    /** 러닝 시작·일시정지 원형 버튼. */
    val runButton = 80.dp
    /** [runButton] 안 아이콘. */
    val runButtonIcon = 28.dp
    /** 러닝 종료처럼 작은 원형 버튼. */
    val runButtonSm = 56.dp
    /** 줄 끝 화살표·작은 버튼 안 아이콘. */
    val iconSm = 18.dp
}
