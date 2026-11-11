package com.tourderun.domain.record

import java.time.Instant
import java.time.ZoneId

/**
 * 러닝 시작 시각의 시간대 구간 (REC-01 기본 제목). 시작 이상, 끝 미만.
 * TODO(REC-01): 구간 경계를 기능 명세서와 대조해 확정한다.
 */
enum class TimeOfDay(val label: String, val startHour: Int) {
    DAWN("새벽", 0),
    MORNING("아침", 6),
    LUNCH("점심", 11),
    AFTERNOON("오후", 14),
    EVENING("저녁", 18),
    NIGHT("밤", 22);

    companion object {
        fun of(hour: Int): TimeOfDay {
            require(hour in 0..23) { "시(hour)는 0~23: $hour" }
            return entries.last { hour >= it.startHour }
        }
    }
}

/**
 * 기본 러닝 제목. 예: "11월 3일 저녁 러닝".
 * 사용자가 제목을 비워 두면 이 값으로 저장한다. Firestore에 그대로 저장되는 값이라 :domain에 둔다.
 *
 * @param startedAtMs 러닝 **시작** 시각(epoch ms)
 * @param zone 날짜·시간대를 정할 시간대(한국 시간)
 */
fun defaultRunTitle(startedAtMs: Long, zone: ZoneId): String {
    val t = Instant.ofEpochMilli(startedAtMs).atZone(zone)
    return "${t.monthValue}월 ${t.dayOfMonth}일 ${TimeOfDay.of(t.hour).label} 러닝"
}
