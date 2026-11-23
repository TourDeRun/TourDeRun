package com.tourderun.feature.record.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.batchOf
import com.tourderun.core.data.record.RunSaver
import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.record.SaveOutcome
import com.tourderun.domain.record.saveOutcome
import com.tourderun.domain.route.routeFor
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirestoreRunSaver @Inject constructor(
    private val firestore: FirebaseFirestore,
) : RunSaver {

    override fun newRunId(): String = firestore.collection(Collections.RUNS).document().id

    override fun save(
        result: RunResult,
        uid: String,
        team: MyTeam?,
        title: String,
        onServerConfirmed: () -> Unit,
        onServerRejected: () -> Unit,
    ): Result<SaveOutcome?> {
        val distanceM = savedDistanceM(result)
        // 페이스를 계산할 수 없는 거리 0은 저장하지 않는다(100m 미만은 원래 저장 대상이 아님, M-05).
        if (distanceM <= 0) {
            return Result.failure(IllegalArgumentException("거리가 0m인 러닝은 저장하지 않는다"))
        }
        return try {
            val batch = firestore.batchOf(runSavePlan(result, distanceM, uid, team, title))
            // 오프라인이면 기기에 대기했다가 온라인이 되면 올라간다. 기다리지 않는다.
            // commit() Task는 서버 응답으로 끝난다: 오프라인 동안은 대기(성공·실패 둘 다 안 불림), 거부되면 실패.
            // 공식 문서는 "쓰기는 네트워크가 돌아올 때까지 쌓인다"(enable-offline)와
            // "쓰기가 끝나면 Task가 끝난다"(WriteBatch.commit)까지만 적고, 오프라인에서 실패가 안 불린다고 직접 적지는 않는다.
            batch.commit()
                .addOnSuccessListener { onServerConfirmed() }
                .addOnFailureListener { e ->
                    Log.w(TAG, "러닝 저장 배치 거부", e)
                    onServerRejected()
                }
            Result.success(team?.let { saveOutcome(it.standing.distanceM, distanceM, routeFor(it.season)) })
        } catch (e: Exception) {
            Log.w(TAG, "러닝 저장 배치 만들기 실패", e)
            Result.failure(e)
        }
    }

    override suspend fun isSavedOnDevice(runId: String): Boolean = try {
        // 대기 중인 쓰기도 캐시에 보인다. 캐시에 문서가 없으면 예외 또는 exists() == false.
        firestore.collection(Collections.RUNS).document(runId).get(Source.CACHE).await().exists()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    override suspend fun awaitPendingWrites(): Boolean = try {
        firestore.waitForPendingWrites().await()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "대기 중인 쓰기 확인 실패", e)
        false
    }

    private companion object {
        const val TAG = "TDR/record"
    }
}
