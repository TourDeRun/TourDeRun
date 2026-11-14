package com.tourderun.core.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import com.tourderun.domain.model.LatLngValue
import com.tourderun.domain.record.RunRecord
import com.tourderun.domain.record.RunTextCheck
import com.tourderun.domain.record.defaultRunTitle
import com.tourderun.domain.stats.SEOUL
import java.util.Date

/**
 * `runs/{runId}` 문서 중 내 기록 화면이 읽는 필드. 거리는 정수 m, 시각은 epoch ms.
 *
 * 문서는 `FirestoreRunSaver`가 쓰지만, 필드가 빠졌거나 타입이 달라도 화면이 죽지 않도록 [runDtoOf]가 기본값으로 채운다.
 *
 * @property title 비어 있으면 [runDtoOf]가 시작 시각으로 기본 제목을 만든다
 * @property memo 없거나 공백뿐이면 null
 * @property path 목록 조회에서는 빈 리스트
 */
data class RunDto(
    val id: String,
    val title: String,
    val memo: String?,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val movingSec: Long,
    val distanceM: Long,
    val paceSecPerKm: Long,
    val path: List<LatLngValue>,
)

/**
 * 문서 필드 → [RunDto]. Firestore 인스턴스 없이 테스트할 수 있게 `Map`을 받는다.
 *
 * 기본값: 숫자 없음 → 0, `startedAt` 없음 → 0, `endedAt` 없음 → 시작 시각, 제목 없음·공백 → `defaultRunTitle`,
 * 메모 공백 → null, `path` 없음·`GeoPoint`가 아닌 점 → 빼고 읽는다.
 *
 * @param includePath false면 `path`를 읽지 않는다(목록). Android SDK는 필드만 골라 받을 수 없어서 변환만 건너뛴다
 */
fun runDtoOf(id: String, fields: Map<String, Any?>, includePath: Boolean = true): RunDto {
    val startedAtMs = epochMsOf(fields[RunFields.STARTED_AT]) ?: 0L
    val title = (fields[RunFields.TITLE] as? String)?.trim().orEmpty()
    return RunDto(
        id = id,
        title = title.ifEmpty { defaultRunTitle(startedAtMs, SEOUL) },
        memo = (fields[RunFields.MEMO] as? String)?.trim()?.ifEmpty { null },
        startedAtMs = startedAtMs,
        endedAtMs = epochMsOf(fields[RunFields.ENDED_AT]) ?: startedAtMs,
        movingSec = longOf(fields[RunFields.MOVING_SEC]),
        distanceM = longOf(fields[RunFields.DISTANCE_M]),
        paceSecPerKm = longOf(fields[RunFields.PACE_SEC_PER_KM]),
        path = if (includePath) pathOf(fields[RunFields.PATH]) else emptyList(),
    )
}

/** 문서 → [RunDto]. 문서가 없으면(지워짐) null. */
fun DocumentSnapshot.toRunDto(includePath: Boolean = true): RunDto? {
    if (!exists()) return null
    return runDtoOf(id, data.orEmpty(), includePath)
}

/** 목록 문서들(문서 ID, 필드) → 기록 목록. 순서를 지키고 `path`는 항상 비운다(REC-02 목록). */
fun runListOf(docs: List<Pair<String, Map<String, Any?>>>): List<RunRecord> =
    docs.map { (id, fields) -> runDtoOf(id, fields, includePath = false).toRunRecord() }

fun RunDto.toRunRecord(): RunRecord = RunRecord(
    id = id,
    title = title,
    memo = memo,
    startedAtMs = startedAtMs,
    endedAtMs = endedAtMs,
    movingSec = movingSec,
    distanceM = distanceM,
    paceSecPerKm = paceSecPerKm,
    path = path,
)

/**
 * 검사를 통과한 제목·메모 → `update()`에 넘길 필드. 보안 규칙이 다른 필드 변경을 막으므로 이 두 필드만 쓴다.
 * 빈 메모는 빈 문자열 `""`로 쓴다(null·필드 삭제가 아니라 항상 문자열). 읽을 때 [runDtoOf]가 다시 null로 바꾼다.
 */
fun titleMemoFields(valid: RunTextCheck.Valid): Map<String, String> = mapOf(
    RunFields.TITLE to valid.title,
    RunFields.MEMO to valid.memo.orEmpty(),
)

/** `path`(List<GeoPoint>) → 좌표 목록. 순서를 지킨다. */
fun pathOf(value: Any?): List<LatLngValue> =
    (value as? List<*>).orEmpty().mapNotNull { (it as? GeoPoint)?.let { p -> LatLngValue(p.latitude, p.longitude) } }

/** 좌표 목록 → `path`에 쓰는 GeoPoint 목록. */
fun List<LatLngValue>.toGeoPoints(): List<GeoPoint> = map { GeoPoint(it.lat, it.lng) }

private fun epochMsOf(value: Any?): Long? = when (value) {
    is Timestamp -> value.seconds * 1_000 + value.nanoseconds / 1_000_000
    is Date -> value.time
    else -> null
}

private fun longOf(value: Any?): Long = (value as? Number)?.toLong() ?: 0L
