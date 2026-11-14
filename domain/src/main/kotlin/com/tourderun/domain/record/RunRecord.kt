package com.tourderun.domain.record

import com.tourderun.domain.model.LatLngValue

/**
 * 저장된 러닝 한 건 (REC-02 내 기록 목록·상세). `runs/{runId}` 문서에서 RunRepository가 만든다.
 *
 * @property memo 없으면 null
 * @property distanceM 저장된 정수 m
 * @property path 상세에서만 채운다. 목록에서는 빈 리스트(경로를 읽지 않음)
 */
data class RunRecord(
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
