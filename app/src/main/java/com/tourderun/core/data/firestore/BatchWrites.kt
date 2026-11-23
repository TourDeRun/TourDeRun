package com.tourderun.core.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.WriteBatch
import java.util.Date

/**
 * 쓰기 계획([DocWrite] 목록, REC-01 `runSavePlan`·JRN-05 `arrivalSeenPlan`)을 새 배치에 순서대로 담는다.
 * `commit()`은 부르는 쪽이 한다(오프라인 대기 쓰기라 `await()`하지 않는다).
 *
 * 잘못된 문서 경로처럼 담는 중에 바로 던지는 예외는 그대로 던진다. 부르는 Repository가 잡아 [Result]로 바꾼다.
 */
fun FirebaseFirestore.batchOf(writes: List<DocWrite>): WriteBatch {
    val batch = batch()
    forEachWrite(writes) { ref, mode, fields ->
        when (mode) {
            WriteMode.SET -> batch.set(ref, fields)
            WriteMode.UPDATE -> batch.update(ref, fields)
            WriteMode.SET_MERGE -> batch.set(ref, fields, SetOptions.merge())
        }
    }
    return batch
}

/**
 * 쓰기 계획을 트랜잭션에 순서대로 담는다 (TEAM-03 팀 참여처럼 읽은 값으로 쓰는 경우).
 * `runTransaction { tx -> ... tx.applyWrites(firestore, plan) }` 안에서, **읽기(`tx.get`)를 모두 끝낸 뒤** 부른다
 * (Firestore 트랜잭션은 쓰기 다음에 읽을 수 없다).
 *
 * 트랜잭션은 온라인 필수라 기기에 대기하지 않는다. 결과는 `runTransaction(...).await()`로 기다린다.
 */
fun Transaction.applyWrites(firestore: FirebaseFirestore, writes: List<DocWrite>): Transaction {
    firestore.forEachWrite(writes) { ref, mode, fields ->
        when (mode) {
            WriteMode.SET -> set(ref, fields)
            WriteMode.UPDATE -> update(ref, fields)
            WriteMode.SET_MERGE -> set(ref, fields, SetOptions.merge())
        }
    }
    return this
}

/** 배치·트랜잭션 공용: 계획의 문서마다 경로 → 참조, 필드 값 → Firestore 값으로 바꿔 [write]에 넘긴다. */
private inline fun FirebaseFirestore.forEachWrite(
    writes: List<DocWrite>,
    write: (DocumentReference, WriteMode, Map<String, Any?>) -> Unit,
) {
    writes.forEach { doc ->
        write(document(doc.path), doc.mode, doc.fields.mapValues { (_, value) -> firestoreValue(value) })
    }
}

/** [FieldOp]·중첩 Map을 Firestore 값으로 바꾼다. 그 밖의 값은 그대로. */
private fun firestoreValue(value: Any?): Any? = when (value) {
    is FieldOp.Increment -> FieldValue.increment(value.by)
    FieldOp.ServerTime -> FieldValue.serverTimestamp()
    is FieldOp.Time -> Timestamp(Date(value.epochMs))
    is FieldOp.Points -> value.points.map { GeoPoint(it.lat, it.lng) }
    is Map<*, *> -> value.entries.associate { (k, v) -> k.toString() to firestoreValue(v) }
    else -> value
}
