package com.tourderun.domain.stats

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.util.Locale

/** 주간 ID 기준 시간대. 한국 시간. */
val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")

/**
 * 주간 통계 기간 ID (STAT-03). "YYYY-Www" 형식(예: 2026-W46, 2027-W01).
 * 월요일 0시 시작, ISO 주차 연도 기준이라 2027-01-01은 2026-W53이다.
 *
 * @param atMs 러닝 **시작** 시각(epoch ms). 현재 시각을 함수 안에서 읽지 않는다.
 */
fun weekId(atMs: Long, zone: ZoneId = SEOUL): String {
    val date = Instant.ofEpochMilli(atMs).atZone(zone).toLocalDate()
    val year = date.get(IsoFields.WEEK_BASED_YEAR)
    val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    return String.format(Locale.ROOT, "%d-W%02d", year, week)
}

/**
 * 지난주 기간 ID (STAT-01). [nowMs]가 속한 주의 바로 앞 주. 연도 경계도 ISO 주차 연도를 따른다
 * (2027-01-04 월요일 → 2026-W53).
 *
 * @param nowMs 현재 시각(epoch ms). 함수 안에서 읽지 않는다.
 */
fun lastWeekId(nowMs: Long, zone: ZoneId = SEOUL): String {
    val weekAgo = Instant.ofEpochMilli(nowMs).atZone(zone).minusWeeks(1)
    return weekId(weekAgo.toInstant().toEpochMilli(), zone)
}
