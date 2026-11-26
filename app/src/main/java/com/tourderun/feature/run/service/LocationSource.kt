package com.tourderun.feature.run.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.tourderun.core.data.Clock
import com.tourderun.domain.model.GpsPoint

/**
 * RUN-04 위치 수신. [RunTrackingController]가 실패를 다룰 수 있게 Fused Location을 감싼다.
 *
 * 실패는 두 가지로 온다.
 * - [request]가 바로 예외를 던진다(예: 그사이 위치 권한이 꺼져 `SecurityException`)
 * - 요청 Task가 나중에 실패한다 → [request]의 `onFailure`
 */
interface LocationSource {
    fun request(onLocations: (List<GpsPoint>) -> Unit, onFailure: (Exception) -> Unit)
    fun remove()
}

/** Fused Location 구현. 1초 간격, 높은 정확도. */
class FusedLocationSource(context: Context, private val clock: Clock) : LocationSource {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission") // 권한이 없으면 SecurityException을 던지고, 호출자(RunTrackingController)가 잡는다.
    override fun request(onLocations: (List<GpsPoint>) -> Unit, onFailure: (Exception) -> Unit) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)
            .setMinUpdateIntervalMillis(INTERVAL_MS)
            .setWaitForAccurateLocation(false)
            .build()
        val newCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val nowMs = clock.nowMs()
                onLocations(result.locations.map { it.toGpsPoint(nowMs) })
            }
        }
        callback = newCallback
        client.requestLocationUpdates(request, newCallback, Looper.getMainLooper())
            .addOnFailureListener { onFailure(it) }
    }

    override fun remove() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
    }

    private companion object {
        const val INTERVAL_MS = 1_000L
    }
}
