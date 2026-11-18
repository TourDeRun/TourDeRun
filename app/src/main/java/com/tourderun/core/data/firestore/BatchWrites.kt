package com.tourderun.core.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
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
    writes.forEach { write ->
        val ref = document(write.path)
        val fields = write.fields.mapValues { (_, value) -> firestoreValue(value) }
        when (write.mode) {
            WriteMode.SET -> batch.set(ref, fields)
            WriteMode.UPDATE -> batch.update(ref, fields)
            WriteMode.SET_MERGE -> batch.set(ref, fields, SetOptions.merge())
        }
    }
    return batch
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
