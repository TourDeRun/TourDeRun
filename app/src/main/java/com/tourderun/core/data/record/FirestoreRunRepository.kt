package com.tourderun.core.data.record

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.RunFields
import com.tourderun.core.data.firestore.runListOf
import com.tourderun.core.data.firestore.snapshotFlow
import com.tourderun.core.data.firestore.titleMemoFields
import com.tourderun.core.data.firestore.toRunDto
import com.tourderun.core.data.firestore.toRunRecord
import com.tourderun.domain.record.RunRecord
import com.tourderun.domain.record.RunTextCheck
import com.tourderun.domain.record.checkRunText
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * 내 러닝 기록의 Firestore 구현 (REC-02·03). 규칙은 [RunRepository] 참고.
 *
 * - 목록: `runs`에서 `uid ==` + `startedAt` 내림차순 + `limit`. 인덱스 runs(uid↑, startedAt↓).
 *   보안 규칙은 필터가 아니라서 `uid` 조건이 없으면 쿼리 전체가 거부된다.
 * - 수정·삭제는 오프라인 대기 쓰기라 `await()`하지 않는다. 커밋을 시작하면 성공을 돌려주고, 서버 거부는 로그만 남긴다.
 * - 리스너 오류는 로그를 남기고 Flow를 끝낸다(받는 쪽은 마지막 값을 유지).
 *   TODO(REC-02): #32에서 리스너 오류를 값으로 내보내는 방식이 정해지면 이 Repository도 같이 바꾼다.
 */
class FirestoreRunRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : RunRepository {

    private val runs get() = firestore.collection(Collections.RUNS)

    override fun observeRuns(uid: String, limit: Long): Flow<List<RunRecord>> {
        if (limit <= 0) return flowOf(emptyList())
        return runs
            .whereEqualTo(RunFields.UID, uid)
            .orderBy(RunFields.STARTED_AT, Query.Direction.DESCENDING)
            .limit(limit)
            .listFlow()
            .map { it.toRunList() }
            .distinctUntilChanged()
    }

    override fun observeRun(runId: String): Flow<RunRecord?> =
        runs.document(runId)
            .snapshotFlow(TAG)
            .map { it.toRunDto()?.toRunRecord() }
            .distinctUntilChanged()

    override fun updateTitleMemo(runId: String, title: String, memo: String?): Result<Unit> =
        when (val check = checkRunText(title, memo)) {
            is RunTextCheck.Invalid -> Result.failure(InvalidRunTextException(check.errors))
            is RunTextCheck.Valid -> startWrite("제목·메모 수정") {
                runs.document(runId).update(titleMemoFields(check))
                    .addOnFailureListener { Log.w(TAG, "제목·메모 수정 거부", it) }
            }
        }

    // 팀 거리·주간 통계·기여는 빼지 않는다(M-01). runs 문서만 지운다.
    override fun delete(runId: String): Result<Unit> = startWrite("기록 삭제") {
        runs.document(runId).delete()
            .addOnFailureListener { Log.w(TAG, "기록 삭제 거부", it) }
    }

    /** 쓰기를 시작만 한다. 잘못된 문서 ID처럼 시작할 때 바로 던지는 예외만 [Result]로 바꾼다. */
    private inline fun startWrite(what: String, block: () -> Unit): Result<Unit> =
        try {
            block()
            Result.success(Unit)
        } catch (e: RuntimeException) {
            Log.w(TAG, "$what 시작 실패", e)
            Result.failure(e)
        }

    /**
     * 쿼리 실시간 리스너. 목록은 오프라인 배지를 쓰지 않아 메타데이터 변경은 받지 않는다.
     * 쿼리 리스너 공용 확장 함수는 #29(`Query.snapshotFlow`)에 있어서, 머지 전까지 여기서만 쓴다.
     */
    private fun Query.listFlow(): Flow<QuerySnapshot> = callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "기록 목록 리스너 오류: ${error.code}", error)
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(snapshot)
        }
        awaitClose { registration.remove() }
    }

    private fun QuerySnapshot.toRunList(): List<RunRecord> =
        runListOf(documents.map { it.id to it.data.orEmpty() })

    private companion object {
        const val TAG = "TDR/record"
    }
}
