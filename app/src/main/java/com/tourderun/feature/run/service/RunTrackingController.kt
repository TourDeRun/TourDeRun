package com.tourderun.feature.run.service

import com.tourderun.core.data.Clock
import com.tourderun.domain.model.GpsPoint
import com.tourderun.feature.run.session.RunSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * RUN-04 측정 서비스의 위치 수신과 실패 처리. Android 서비스 생명주기와 떼어 단위 테스트한다.
 *
 * 위치 요청이 바로 실패하든(예외) 나중에 실패하든(Task 실패) [fail]로 모은다.
 * [fail]은 매니저를 되돌리고([RunSessionManager.stopForServiceFailure]), 위치 요청을 지운 뒤 [onStop]을 부른다.
 * 서비스는 [onStop]에서 알림과 서비스를 정리한다.
 */
class RunTrackingController(
    private val manager: RunSessionManager,
    private val clock: Clock,
    private val locationSource: LocationSource,
    private val scope: CoroutineScope,
    private val onStop: () -> Unit,
    /** 로그 출력. 단위 테스트에서 android.util.Log를 부르지 않도록 서비스가 넘긴다. 좌표·uid는 넘기지 않는다. */
    private val log: (message: String, error: Throwable?) -> Unit = { _, _ -> },
) {
    private var started = false
    private var failed = false

    fun start() {
        if (started) return
        started = true
        try {
            locationSource.request(onLocations = ::onLocations, onFailure = ::fail)
        } catch (error: Exception) {
            fail(error)
        }
    }

    /** 서비스를 시작하지 못했거나 위치를 받을 수 없을 때. 여러 번 불려도 한 번만 처리한다. */
    fun fail(error: Exception?) {
        if (failed) return
        failed = true
        log("측정 서비스 실패, 러닝을 멈춘다", error)
        scope.launch {
            manager.stopForServiceFailure(clock.nowMs())
            locationSource.remove()
            onStop()
        }
    }

    /** 러닝이 정상적으로 끝났을 때. */
    fun stop() {
        locationSource.remove()
    }

    private fun onLocations(points: List<GpsPoint>) {
        scope.launch {
            for (point in points) {
                try {
                    manager.onLocation(point, clock.nowMs())
                } catch (error: Exception) {
                    // 기기 저장 실패. 매니저가 다음 계산 전에 저장된 점을 다시 읽는다.
                    log("위치점 저장 실패", error)
                }
            }
        }
    }
}
