package com.tourderun.feature.record.ui

import com.tourderun.core.ui.model.RunRowUi
import com.tourderun.domain.record.RunRecord
import com.tourderun.domain.record.RunTextCheck
import com.tourderun.domain.record.checkRunText
import com.tourderun.domain.stats.SEOUL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 목록 한 줄. 날짜는 러닝 시작 시각의 [zone] 기준 월·일. */
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

/**
 * 기록 목록 → S11 상태. [runs]는 최신순으로 최대 [limit]개.
 * 받은 개수가 [limit]만큼이면 더 있을 수 있다고 본다(딱 [limit]개뿐이면 한 번 더 불러 보고 끝난다).
 *
 * TODO(AUTH-03): 총 횟수·총 거리는 `users` 문서의 `runCount`·`totalM`으로 바꾼다. 지금은 불러온 기록만 더한 값이라
 *  기록이 한 쪽(20개)을 넘으면 실제보다 작다.
 */
fun recordsUiState(runs: List<RunRecord>, limit: Long, zone: ZoneId = SEOUL): RecordsUiState =
    RecordsUiState(
        totalRuns = runs.size,
        totalDistanceM = runs.sumOf { it.distanceM },
        runs = runs.map { it.toRowUi(zone) },
        canLoadMore = runs.size >= limit,
    )

private val STARTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy. M. d (E) HH:mm", Locale.KOREAN)

/** "2026. 11. 6 (금) 19:02" */
fun startedAtText(startedAtMs: Long, zone: ZoneId = SEOUL): String =
    STARTED_AT_FORMAT.format(Instant.ofEpochMilli(startedAtMs).atZone(zone))

/**
 * 저장된 기록 + 고치는 중인 값 → S12 상태. [draftTitle]·[draftMemo]가 null이면 아직 손대지 않은 칸이다.
 * 저장 버튼은 저장된 값과 다르고 `checkRunText`를 통과할 때만 켠다(공백만 다른 경우는 바뀐 것으로 보지 않는다).
 */
fun recordDetailUiState(
    record: RunRecord,
    draftTitle: String?,
    draftMemo: String?,
    showDeleteConfirm: Boolean,
    zone: ZoneId = SEOUL,
): RecordDetailUiState {
    val title = draftTitle ?: record.title
    val memo = draftMemo ?: record.memo.orEmpty()
    val check = checkRunText(title, memo)
    return RecordDetailUiState(
        startedAtText = startedAtText(record.startedAtMs, zone),
        title = title,
        memo = memo,
        distanceM = record.distanceM,
        movingSec = record.movingSec,
        paceSecPerKm = record.paceSecPerKm,
        path = record.path,
        canSave = check is RunTextCheck.Valid && (check.title != record.title || check.memo != record.memo),
        showDeleteConfirm = showDeleteConfirm,
    )
}
