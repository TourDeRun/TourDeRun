package com.tourderun.feature.run.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tourderun.R
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrButtonStyle
import com.tourderun.core.designsystem.theme.TdrSize
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

private fun Context.activityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activityOrNull()
    else -> null
}

/** 정확한 위치와(Android 13 이상) 알림 권한이 모두 있으면 true. 서비스를 시작하기 전에 확인한다. */
internal fun Context.hasRunPermissions(): Boolean {
    val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val notifications = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    return fine && notifications
}

/**
 * 위치·알림 권한 안내. 서비스는 사용자가 앱을 보고 있을 때 시작한다.
 *
 * @param continueLabel 권한이 모두 있을 때 보이는 버튼 글자 (S09에서는 "시작")
 */
@Composable
fun RunPermissionGate(
    onReady: () -> Unit,
    modifier: Modifier = Modifier,
    continueLabel: String = stringResource(R.string.run_permission_continue),
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val preferences = remember(context) { context.getSharedPreferences("run_permissions", Context.MODE_PRIVATE) }
    var granted by remember(context) { mutableStateOf(context.hasRunPermissions()) }
    var requested by remember { mutableStateOf(preferences.getBoolean("requested", false)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = context.hasRunPermissions()
    }
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = context.hasRunPermissions()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val activity = context.activityOrNull()
    val fineDenied = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
    val notificationDenied = Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    val showSettings = requested && activity != null &&
        ((fineDenied && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) ||
            (notificationDenied && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)))

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TdrSpacing.s3)) {
        Text(stringResource(R.string.run_permission_explanation), style = TdrTheme.typography.body, color = TdrTheme.colors.inkMuted)
        if (granted) {
            TdrButton(continueLabel, onReady, Modifier.fillMaxWidth(), height = TdrSize.buttonMd)
        } else {
            TdrButton(stringResource(R.string.run_permission_request), onClick = {
                preferences.edit().putBoolean("requested", true).apply()
                requested = true
                val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                launcher.launch(permissions.toTypedArray())
            }, modifier = Modifier.fillMaxWidth(), height = TdrSize.buttonMd)
            if (showSettings) {
                TdrButton(stringResource(R.string.run_permission_settings), onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }, modifier = Modifier.fillMaxWidth(), style = TdrButtonStyle.Outline, height = TdrSize.buttonMd)
            }
        }
    }
}
