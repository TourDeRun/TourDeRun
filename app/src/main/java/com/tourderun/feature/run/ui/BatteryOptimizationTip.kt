package com.tourderun.feature.run.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tourderun.R
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrButtonStyle
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.theme.TdrSize
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

private fun Context.ignoresBatteryOptimizations(): Boolean =
    getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) ?: true

/**
 * 배터리 최적화 예외 안내 (기능 명세 12장). 갤럭시 절전 모드에서 화면을 끄면 측정이 멈출 수 있어,
 * 앱이 이미 예외가 아니면 설정 화면으로 가는 버튼을 보여준다.
 * 바로 예외를 요청하는 권한(REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)은 쓰지 않고, 사용자가 목록에서 직접 고르게 한다.
 */
@Composable
fun BatteryOptimizationTip(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var ignoring by remember(context) { mutableStateOf(context.ignoresBatteryOptimizations()) }
    DisposableEffect(lifecycle, context) {
        // 설정에서 돌아오면 다시 확인한다.
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) ignoring = context.ignoresBatteryOptimizations()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (ignoring) return

    TdrCard(modifier, verticalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
        Text(stringResource(R.string.run_battery_title), style = TdrTheme.typography.subtitle)
        Text(stringResource(R.string.run_battery_body), style = TdrTheme.typography.caption, color = TdrTheme.colors.inkMuted)
        TdrButton(
            text = stringResource(R.string.run_battery_open),
            onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            },
            style = TdrButtonStyle.Soft,
            height = TdrSize.touch,
        )
    }
}
