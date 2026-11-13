package com.tourderun.feature.record.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.RunFields
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.record.thinEvenly
import com.tourderun.domain.stats.weekId
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToLong

class FirestoreRunSaver @Inject constructor(
    private val firestore: FirebaseFirestore,
) : RunSaver {

    override fun newRunId(): String = firestore.collection(Collections.RUNS).document().id

    override fun save(result: RunResult, uid: String, teamId: String?, title: String): Result<Unit> {
        val distanceM = result.distanceM.roundToLong()
        // 페이스를 계산할 수 없는 거리 0은 저장하지 않는다(100m 미만은 원래 저장 대상이 아님, M-05).
        if (distanceM <= 0) {
            return Result.failure(IllegalArgumentException("거리가 0m인 러닝은 저장하지 않는다"))
        }
        return try {
            val batch = firestore.batch()
            val runRef = firestore.collection(Collections.RUNS).document(result.runId)
            batch.set(runRef, runDocument(result, distanceM, uid, teamId, title))
            // TODO(REC-01): #10에서 teams·members·teamStats·users 증가 쓰기를 같은 배치에 추가한다.

            // 오프라인이면 기기에 대기했다가 온라인이 되면 올라간다. 기다리지 않는다.
            batch.commit().addOnFailureListener { e -> Log.w(TAG, "runs 저장 실패", e) }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "runs 배치 만들기 실패", e)
            Result.failure(e)
        }
    }

    private fun runDocument(
        result: RunResult,
        distanceM: Long,
        uid: String,
        teamId: String?,
        title: String,
    ): Map<String, Any?> =
        mapOf(
            RunFields.UID to uid,
            RunFields.TEAM_ID to teamId,
            RunFields.STARTED_AT to timestampOf(result.startedAtMs),
            RunFields.ENDED_AT to timestampOf(result.endedAtMs),
            RunFields.MOVING_SEC to result.movingSec,
            RunFields.DISTANCE_M to distanceM,
            RunFields.PACE_SEC_PER_KM to paceSecPerKm(result.movingSec, distanceM),
            // 주간 통계와 같은 기준: 러닝 시작 시각의 한국 시간 ISO 주차(STAT-03)
            RunFields.PERIOD_ID to weekId(result.startedAtMs),
            RunFields.TITLE to title,
            RunFields.MEMO to null,
            RunFields.PATH to thinEvenly(result.acceptedPoints).map { GeoPoint(it.lat, it.lng) },
        )

    private fun timestampOf(epochMs: Long) = Timestamp(Date(epochMs))

    /**
     * 평균 페이스(초/km). [distanceM]은 0보다 커야 한다([save]에서 거른다).
     * TODO(REC-01): 최한솔의 :domain/run 페이스 함수가 생기면 그것으로 바꾼다.
     */
    private fun paceSecPerKm(movingSec: Long, distanceM: Long): Long =
        (movingSec * 1_000.0 / distanceM).roundToLong()

    private companion object {
        const val TAG = "TDR/record"
    }
}
