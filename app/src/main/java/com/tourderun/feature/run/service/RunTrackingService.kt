package com.tourderun.feature.run.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.tourderun.MainActivity
import com.tourderun.R
import com.tourderun.core.data.Clock
import com.tourderun.core.ui.Format
import com.tourderun.domain.model.GpsPoint
import com.tourderun.feature.run.session.RunPhase
import com.tourderun.feature.run.session.RunSessionManager
import com.tourderun.feature.run.session.RunState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * RUN-04 러닝 측정 포그라운드 서비스 (location 타입).
 *
 * - 화면이 꺼져도 1초마다 위치를 받아 [RunSessionManager.onLocation]에 넘기고, 1초마다 [RunSessionManager.tick]을 부른다.
 * - 카운트다운 중에도 위치 수신을 먼저 시작해 GPS를 준비한다(RUN-02). 카운트다운 중 점은 매니저가 거리에 넣지 않는다.
 * - 알림은 러닝이 끝날 때까지 지울 수 없고, 누르면 앱(S09)으로 돌아간다.
 * - 매니저 상태가 IDLE·FINISHED가 되면 위치 요청과 서비스를 바로 멈춘다.
 *
 * Android 12 이상은 백그라운드에서 location 서비스를 시작할 수 없으므로 [start]는 앱 화면이 보일 때만 부른다.
 * 시스템이 서비스를 죽이면 다시 띄우지 않고(START_NOT_STICKY), 다음에 앱을 열 때 RUN-07 복구 안내로 이어간다.
 */
@AndroidEntryPoint
class RunTrackingService : Service() {
    @Inject lateinit var manager: RunSessionManager
    @Inject lateinit var clock: Clock

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var locationClient: FusedLocationProviderClient
    private var tracking = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val points = result.locations.map { it.toGpsPoint(clock.nowMs()) }
            scope.launch {
                for (point in points) {
                    try {
                        manager.onLocation(point, clock.nowMs())
                    } catch (error: Exception) {
                        // 기기 저장 실패. 매니저가 다음 계산 전에 저장된 점을 다시 읽는다.
                        Log.w(TAG, "위치점 저장 실패", error)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService 후 5초 안에 startForeground를 불러야 하므로 무엇보다 먼저 한다.
        val started = runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification(manager.state.value),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
            )
        }
        if (started.isFailure || !hasFineLocation()) {
            Log.w(TAG, "포그라운드 서비스 시작 실패", started.exceptionOrNull())
            stopSelf()
            return START_NOT_STICKY
        }
        if (!tracking) {
            tracking = true
            requestLocation()
            launchTicker()
            launchNotificationUpdates()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        locationClient.removeLocationUpdates(callback)
        scope.cancel()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission") // hasFineLocation()으로 확인한 뒤에만 부른다.
    private fun requestLocation() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
            .setMinUpdateIntervalMillis(LOCATION_MIN_INTERVAL_MS)
            .setWaitForAccurateLocation(false)
            .build()
        locationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun launchTicker() = scope.launch {
        while (isActive) {
            manager.tick(clock.nowMs())
            delay(TICK_MS)
        }
    }

    private fun launchNotificationUpdates() = scope.launch {
        manager.state
            // 알림은 초 단위로만 바뀌면 되므로 화면용 값(현재 페이스 등)의 변화는 거른다.
            .map { NotificationContent(it.phase, it.distanceM.roundToLong() / 10, it.movingSec, it.countdownSec) }
            .distinctUntilChanged()
            .collect { content ->
                if (content.phase == RunPhase.IDLE || content.phase == RunPhase.FINISHED) {
                    stopTracking()
                } else {
                    val notificationManager = getSystemService(NotificationManager::class.java)
                    notificationManager.notify(NOTIFICATION_ID, notification(manager.state.value))
                }
            }
    }

    private fun stopTracking() {
        locationClient.removeLocationUpdates(callback)
        tracking = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(state: RunState): Notification {
        val km = Format.km(state.distanceM.roundToLong(), decimals = 2)
        val time = Format.duration(state.movingSec)
        val text = when (state.phase) {
            RunPhase.COUNTDOWN -> getString(R.string.run_notification_countdown)
            RunPhase.PAUSED -> getString(R.string.run_notification_paused, km, time)
            else -> getString(R.string.run_notification_running, km, time)
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            // 이미 열린 화면(대개 S09)을 그대로 앞으로 가져온다.
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_run_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.run_notification_channel), NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun hasFineLocation(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private data class NotificationContent(val phase: RunPhase, val distance10M: Long, val movingSec: Long, val countdownSec: Int)

    companion object {
        private const val TAG = "TDR/run"
        private const val CHANNEL_ID = "run_tracking"
        private const val NOTIFICATION_ID = 9001
        private const val LOCATION_INTERVAL_MS = 1_000L
        private const val LOCATION_MIN_INTERVAL_MS = 1_000L
        private const val TICK_MS = 1_000L

        /** 앱 화면이 보일 때만 부른다. 이미 실행 중이면 아무 일도 하지 않는다. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, RunTrackingService::class.java))
        }
    }
}

/**
 * 위치 → [GpsPoint]. 오차를 모르면 버려지도록 큰 값으로 둔다.
 * GPS 시각이 기기 시계보다 조금 앞설 수 있어 현재 시각을 넘지 않게 자른다(매니저는 미래 시각 점을 버린다).
 */
internal fun Location.toGpsPoint(nowMs: Long) = GpsPoint(
    lat = latitude,
    lng = longitude,
    timeMs = minOf(time, nowMs),
    accuracyM = if (hasAccuracy()) accuracy else UNKNOWN_ACCURACY_M,
)

private const val UNKNOWN_ACCURACY_M = 999f
