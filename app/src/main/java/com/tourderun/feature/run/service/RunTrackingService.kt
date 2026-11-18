package com.tourderun.feature.run.service

import android.Manifest
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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
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
    private var controller: RunTrackingController? = null

    override fun onCreate() {
        super.onCreate()
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
        val current = controller ?: RunTrackingController(
            manager = manager,
            clock = clock,
            locationSource = FusedLocationSource(this, clock),
            scope = scope,
            onStop = ::stopTracking,
            log = { message, error -> Log.w(TAG, message, error) },
        ).also { controller = it }
        if (started.isFailure || !hasFineLocation()) {
            // 위치를 받을 수 없으니 화면이 측정 중으로 남지 않게 러닝을 되돌린 뒤 서비스를 끝낸다.
            current.fail(started.exceptionOrNull() as? Exception)
            return START_NOT_STICKY
        }
        if (!tracking) {
            tracking = true
            // 위치 요청이 바로 실패하거나(예외) 나중에 실패해도(Task) controller가 러닝을 되돌리고 stopTracking을 부른다.
            current.start()
            launchTicker()
            launchNotificationUpdates()
        }
        return START_NOT_STICKY
    }

    private var tracking = false

    override fun onDestroy() {
        controller?.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun launchTicker() = scope.launch {
        while (isActive) {
            manager.tick(clock.nowMs())
            delay(TICK_MS)
        }
    }

    /**
     * 경과 시간은 알림의 크로노미터가 스스로 세므로 다시 그리지 않는다.
     * 단계가 바뀌면 바로, 거리만 바뀌면 5초에 한 번만 갱신한다([shouldUpdateNotification]).
     * 매니저 상태는 tick마다 바뀌므로, 건너뛴 거리 변화도 5초가 지나면 다음 tick에서 표시된다.
     */
    private fun launchNotificationUpdates() = scope.launch {
        var shown: ShownNotification? = null
        manager.state.collect { state ->
            if (state.phase == RunPhase.IDLE || state.phase == RunPhase.FINISHED) {
                stopTracking()
                return@collect
            }
            val content = NotificationContent(state.phase, state.distanceM.roundToLong() / 10, state.countdownSec)
            val nowMs = clock.nowMs()
            if (!shouldUpdateNotification(shown, content, nowMs)) return@collect
            shown = ShownNotification(content, nowMs)
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(state))
        }
    }

    private fun stopTracking() {
        controller?.stop()
        tracking = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(state: RunState): Notification {
        val km = Format.km(state.distanceM.roundToLong(), decimals = 2)
        val running = state.phase == RunPhase.RUNNING
        val text = when (state.phase) {
            RunPhase.COUNTDOWN -> getString(R.string.run_notification_countdown)
            RunPhase.PAUSED -> getString(R.string.run_notification_paused, km, Format.duration(state.movingSec))
            else -> getString(R.string.run_notification_running, km)
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
            // 측정 중에는 경과 시간을 시스템 크로노미터가 센다(움직인 시간 기준). 일시정지 중에는 글자로 멈춘 시간을 보인다.
            .setUsesChronometer(running)
            .setShowWhen(running)
            .setWhen(chronometerBaseMs(clock.nowMs(), state.movingSec))
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

    companion object {
        private const val TAG = "TDR/run"
        private const val CHANNEL_ID = "run_tracking"
        private const val NOTIFICATION_ID = 9001
        private const val TICK_MS = 1_000L

        /**
         * 앱 화면이 보일 때만 부른다. 이미 실행 중이면 아무 일도 하지 않는다.
         *
         * 시스템이 시작 요청 자체를 거절하면(Android 12+ 백그라운드 시작 제한
         * `ForegroundServiceStartNotAllowedException`(IllegalStateException), 권한 문제 `SecurityException`)
         * onStartCommand까지 오지 않는다. 그때는 false를 돌려주고, 호출부가
         * [RunSessionManager.stopForServiceFailure]로 러닝을 되돌린다.
         */
        fun start(context: Context): Boolean = try {
            ContextCompat.startForegroundService(context, Intent(context, RunTrackingService::class.java))
            true
        } catch (error: IllegalStateException) {
            Log.w(TAG, "측정 서비스 시작 거절", error)
            false
        } catch (error: SecurityException) {
            Log.w(TAG, "측정 서비스 시작 거절", error)
            false
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
