package com.tourderun.core.data.firestore

import com.tourderun.domain.model.LatLngValue

/*
 * 배치 쓰기 계획을 Firebase 타입 없이 나타내는 값들. 계획을 만드는 순수 함수(REC-01 `runSavePlan`,
 * JRN-05 `arrivalSeenPlan`)를 JVM 테스트로 확인하고, Firestore 구현은 이 값을 배치로 옮기기만 한다.
 */

/** 배치 안 문서 하나를 쓰는 방식. */
enum class WriteMode {
    /** 새 문서 생성(`set`). */
    SET,

    /** 있는 문서의 필드만 바꾼다(`update`). 문서가 없으면 배치 전체가 실패한다. */
    UPDATE,

    /** 없으면 만들고 있으면 합친다(`set(merge)`). 중첩 Map은 안쪽 키만 합쳐진다. */
    SET_MERGE,

    /** 문서를 지운다(`delete`). [DocWrite.fields]는 비워 둔다 (TEAM-07). */
    DELETE,
}

/**
 * Firebase 타입을 쓰지 않는 필드 값. 공용 `batchOf()`가 Firestore 값으로 바꾼다.
 * 그래서 배치 내용을 Firestore 없이 테스트할 수 있다.
 */
sealed interface FieldOp {
    /** `FieldValue.increment(by)` */
    data class Increment(val by: Long) : FieldOp

    /** `FieldValue.serverTimestamp()` */
    data object ServerTime : FieldOp

    /** `Timestamp` (epoch ms) */
    data class Time(val epochMs: Long) : FieldOp

    /** `List<GeoPoint>` */
    data class Points(val points: List<LatLngValue>) : FieldOp
}

/**
 * 배치에 들어갈 문서 하나.
 *
 * @property path `runs/{runId}`처럼 컬렉션부터 시작하는 문서 경로
 * @property fields 키는 최상위 필드 이름(점 표기 없음). 값은 일반 값·[FieldOp]·중첩 Map
 */
data class DocWrite(
    val path: String,
    val mode: WriteMode,
    val fields: Map<String, Any?>,
)
