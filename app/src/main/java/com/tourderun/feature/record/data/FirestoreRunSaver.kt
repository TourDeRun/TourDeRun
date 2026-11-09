package com.tourderun.feature.record.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.RunFields
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.record.thinEvenly
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToLong

class FirestoreRunSaver @Inject constructor(
    private val firestore: FirebaseFirestore,
) : RunSaver {

    override fun newRunId(): String = firestore.collection(Collections.RUNS).document().id

    override fun save(result: RunResult, uid: String, teamId: String?, title: String): Result<Unit> =
        try {
            val batch = firestore.batch()
            val runRef = firestore.collection(Collections.RUNS).document(result.runId)
            batch.set(runRef, runDocument(result, uid, teamId, title))
            // TODO(REC-01): #10에서 teams·members·teamStats·users 증가 쓰기를 같은 배치에 추가한다.

            // 오프라인이면 기기에 대기했다가 온라인이 되면 올라간다. 기다리지 않는다.
            batch.commit().addOnFailureListener { e -> Log.w(TAG, "runs 저장 실패", e) }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "runs 배치 만들기 실패", e)
            Result.failure(e)
        }

    private fun runDocument(result: RunResult, uid: String, teamId: String?, title: String): Map<String, Any?> {
        val distanceM = result.distanceM.roundToLong()
        return mapOf(
            RunFields.UID to uid,
            RunFields.TEAM_ID to teamId,
            RunFields.STARTED_AT to timestampOf(result.startedAtMs),
            RunFields.ENDED_AT to timestampOf(result.endedAtMs),
            RunFields.MOVING_SEC to result.movingSec,
            RunFields.DISTANCE_M to distanceM,
            RunFields.PACE_SEC_PER_KM to paceSecPerKm(result.movingSec, distanceM),
            // TODO(REC-01): PR #15(weekId) 머지 후 weekId(result.startedAtMs)로 바꾼다. 그 전에는 null.
            RunFields.PERIOD_ID to null,
            RunFields.TITLE to title,
            RunFields.MEMO to null,
            RunFields.PATH to thinEvenly(result.acceptedPoints).map { GeoPoint(it.lat, it.lng) },
        )
    }

    private fun timestampOf(epochMs: Long) = Timestamp(Date(epochMs))

    /**
     * 평균 페이스(초/km). 거리가 0이면 null.
     * TODO(REC-01): 최한솔의 :domain/run 페이스 함수가 생기면 그것으로 바꾼다.
     */
    private fun paceSecPerKm(movingSec: Long, distanceM: Long): Long? =
        if (distanceM > 0) (movingSec * 1_000.0 / distanceM).roundToLong() else null

    private companion object {
        const val TAG = "TDR/record"
    }
}
