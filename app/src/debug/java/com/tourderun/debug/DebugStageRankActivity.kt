package com.tourderun.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.feature.race.ui.StageRankScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * S15 스테이지 순위 확인용(RACE-03). 디버그 빌드에만 있고, 내비게이션(AppNavHost)이 생기기 전까지 adb로 연다.
 * 가짜 바인딩(FakeRaceRepository 기본 시나리오, 고정 내 팀)이라 Firebase를 쓰지 않는다.
 */
@AndroidEntryPoint
class DebugStageRankActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TdrTheme {
                StageRankScreen(onBack = ::finish)
            }
        }
    }
}
