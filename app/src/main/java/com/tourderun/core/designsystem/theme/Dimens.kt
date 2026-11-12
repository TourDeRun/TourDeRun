package com.tourderun.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** 간격 토큰 (space-1 ~ space-8). */
object TdrSpacing {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
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
}
