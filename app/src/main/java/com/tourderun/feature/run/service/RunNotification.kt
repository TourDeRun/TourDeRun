package com.tourderun.feature.run.service

import com.tourderun.feature.run.session.RunPhase

/** 알림에 보이는 값 중 경과 시간을 뺀 것. 경과 시간은 크로노미터가 센다. */
internal data class NotificationContent(val phase: RunPhase, val distance10M: Long, val countdownSec: Int)

/** 마지막으로 실제 표시한 알림과 그 시각. */
internal data class ShownNotification(val content: NotificationContent, val atMs: Long)

/** 거리만 바뀌었을 때 알림을 다시 그리는 최소 간격(배터리). */
internal const val DISTANCE_NOTIFY_MS = 5_000L

/**
 * 크로노미터 기준 시각. 크로노미터는 `지금 - when`을 표시하므로, 움직인 시간(일시정지 제외)만큼 앞으로 둔다.
 * 일시정지했다가 재개하면 movingSec가 그대로라 기준 시각이 정지한 시간만큼 늦어지고, 표시 시간은 이어진다.
 */
internal fun chronometerBaseMs(nowMs: Long, movingSec: Long): Long = nowMs - movingSec * 1_000

/**
 * 알림을 다시 그릴지. 단계(측정·일시정지·카운트다운)나 카운트다운 숫자가 바뀌면 바로,
 * 거리만 바뀌었으면 마지막 표시로부터 [DISTANCE_NOTIFY_MS]가 지났을 때만 그린다.
 */
internal fun shouldUpdateNotification(shown: ShownNotification?, next: NotificationContent, nowMs: Long): Boolean {
    if (shown == null) return true
    val prev = shown.content
    if (prev == next) return false
    if (prev.phase != next.phase || prev.countdownSec != next.countdownSec) return true
    return nowMs - shown.atMs >= DISTANCE_NOTIFY_MS
}
