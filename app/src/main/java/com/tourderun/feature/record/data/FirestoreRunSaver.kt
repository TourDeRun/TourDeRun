package com.tourderun.feature.record.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.record.SaveOutcome
import com.tourderun.domain.record.saveOutcome
import com.tourderun.domain.route.routeFor
import java.util.Date
import javax.inject.Inject

class FirestoreRunSaver @Inject constructor(
    private val firestore: FirebaseFirestore,
) : RunSaver {

    override fun newRunId(): String = firestore.collection(Collections.RUNS).document().id

    override fun save(result: RunResult, uid: String, team: MyTeam?, title: String): Result<SaveOutcome?> {
        val distanceM = savedDistanceM(result)
        // 페이스를 계산할 수 없는 거리 0은 저장하지 않는다(100m 미만은 원래 저장 대상이 아님, M-05).
        if (distanceM <= 0) {
            return Result.failure(IllegalArgumentException("거리가 0m인 러닝은 저장하지 않는다"))
        }
        return try {
            val batch = firestore.batch()
            runSavePlan(result, distanceM, uid, team, title).forEach { write ->
                val ref = firestore.document(write.path)
                val fields = write.fields.toFirestore()
                when (write.mode) {
                    WriteMode.SET -> batch.set(ref, fields)
                    WriteMode.UPDATE -> batch.update(ref, fields)
                    WriteMode.SET_MERGE -> batch.set(ref, fields, SetOptions.merge())
                }
            }
            // 오프라인이면 기기에 대기했다가 온라인이 되면 올라간다. 기다리지 않는다.
            batch.commit()
                .addOnSuccessListener {
                    // TODO(REC-01): 서버 확인 후 Room 경로 점·DataStore 진행 중 러닝 정리(#23)
                }
                .addOnFailureListener { e -> Log.w(TAG, "러닝 저장 배치 실패", e) }
            Result.success(team?.let { saveOutcome(it.standing.distanceM, distanceM, routeFor(it.season)) })
        } catch (e: Exception) {
            Log.w(TAG, "러닝 저장 배치 만들기 실패", e)
            Result.failure(e)
        }
    }

    /** [FieldOp]·중첩 Map을 Firestore 값으로 바꾼다. */
    private fun Map<String, Any?>.toFirestore(): Map<String, Any?> = mapValues { (_, v) -> firestoreValue(v) }

    private fun firestoreValue(value: Any?): Any? = when (value) {
        is FieldOp.Increment -> FieldValue.increment(value.by)
        FieldOp.ServerTime -> FieldValue.serverTimestamp()
        is FieldOp.Time -> Timestamp(Date(value.epochMs))
        is FieldOp.Points -> value.points.map { GeoPoint(it.lat, it.lng) }
        is Map<*, *> -> value.entries.associate { (k, v) -> k.toString() to firestoreValue(v) }
        else -> value
    }

    private companion object {
        const val TAG = "TDR/record"
    }
}
