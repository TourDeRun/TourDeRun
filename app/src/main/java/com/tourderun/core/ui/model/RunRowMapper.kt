package com.tourderun.core.ui.model

import com.tourderun.domain.record.RunRecord
import com.tourderun.domain.stats.SEOUL
import java.time.Instant
import java.time.ZoneId

/** 기록 목록 한 줄(S11, S13 최근 기록). 날짜는 러닝 시작 시각의 [zone] 기준 월·일. */
fun RunRecord.toRowUi(zone: ZoneId = SEOUL): RunRowUi {
    val date = Instant.ofEpochMilli(startedAtMs).atZone(zone)
    return RunRowUi(
        runId = id,
        month = date.monthValue,
        day = date.dayOfMonth,
        title = title,
        movingSec = movingSec,
        paceSecPerKm = paceSecPerKm,
        distanceM = distanceM,
    )
}
