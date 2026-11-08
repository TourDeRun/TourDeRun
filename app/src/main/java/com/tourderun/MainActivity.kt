package com.tourderun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tourderun.domain.journey.locate
import com.tourderun.domain.route.routeFor
import dagger.hilt.android.AndroidEntryPoint

/**
 * 뼈대 확인용 임시 화면. :domain 연결과 Hilt가 동작하는지만 보여준다.
 * 전병국이 AppNavHost(하단 탭 5칸)로 교체한다.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val route = routeFor(1)
        val pos = locate(66_500, route)
        val stage = route.stages[pos.stageIndex]

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Tour De Run 뼈대 OK")
                        Text("66.5km → 스테이지 ${pos.stageIndex + 1} · ${stage.from.nameKo} → ${stage.to.nameKo}")
                        Text("${stage.to.nameKo}까지 ${pos.toNextM / 1000.0}km")
                    }
                }
            }
        }
    }
}
