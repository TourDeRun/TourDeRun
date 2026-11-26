package com.tourderun.core.data.record

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.mapReady
import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.RunFields
import com.tourderun.core.data.firestore.batchOf
import com.tourderun.core.data.firestore.runListOf
import com.tourderun.core.data.firestore.snapshotFlow
import com.tourderun.core.data.firestore.titleMemoFields
import com.tourderun.core.data.firestore.toRunDto
import com.tourderun.core.data.firestore.toRunRecord
import com.tourderun.domain.record.RunRecord
import com.tourderun.domain.record.RunTextCheck
import com.tourderun.domain.record.checkRunText
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * 내 러닝 기록의 Firestore 구현 (REC-02·03). 규칙은 [RunRepository] 참고.
 *
 * - 목록: `runs`에서 `uid ==` + `startedAt` 내림차순 + `limit`. 인덱스 runs(uid↑, startedAt↓).
 *   보안 규칙은 필터가 아니라서 `uid` 조건이 없으면 쿼리 전체가 거부된다.
 * - 수정·삭제는 오프라인 대기 쓰기라 `await()`하지 않는다. 커밋을 시작하면 성공을 돌려주고, 서버 거부는 로그만 남긴다.
 * - 리스너는 공용 `snapshotFlow`([Loaded])를 쓴다. 메타데이터 변경도 받지만 값·출처가 같으면 `distinctUntilChanged()`로 거른다.
 */
class FirestoreRunRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : RunRepository {

    private val runs get() = firestore.collection(Collections.RUNS)

    override fun observeRuns(uid: String, limit: Long): Flow<Loaded<List<RunRecord>>> {
        if (limit <= 0) return flowOf(Loaded.Ready(emptyList(), isFromCache = false))
        return runs
            .whereEqualTo(RunFields.UID, uid)
            .orderBy(RunFields.STARTED_AT, Query.Direction.DESCENDING)
            .limit(limit)
            .snapshotFlow(TAG)
            .map { loaded -> loaded.mapReady { it.toRunList() } }
            .distinctUntilChanged()
    }

    override fun observeRun(runId: String): Flow<Loaded<RunRecord?>> =
        runs.document(runId)
            .snapshotFlow(TAG)
            .map { loaded -> loaded.mapReady { it.toRunDto()?.toRunRecord() } }
            .distinctUntilChanged()

    override fun updateTitleMemo(runId: String, title: String, memo: String?): Result<Unit> =
        when (val check = checkRunText(title, memo)) {
            is RunTextCheck.Invalid -> Result.failure(InvalidRunTextException(check.errors))
            is RunTextCheck.Valid -> startWrite("제목·메모 수정") {
                runs.document(runId).update(titleMemoFields(check))
                    .addOnFailureListener { Log.w(TAG, "제목·메모 수정 거부", it) }
            }
        }

    // runs 삭제와 내 누적 값 빼기를 한 배치로. 팀 거리·주간 통계·기여는 빼지 않는다(M-01).
    override fun delete(uid: String, runId: String, distanceM: Long): Result<Unit> = startWrite("기록 삭제") {
        firestore.batchOf(runDeletePlan(runId, uid, distanceM)).commit()
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

    private fun QuerySnapshot.toRunList(): List<RunRecord> =
        runListOf(documents.map { it.id to it.data.orEmpty() })

    private companion object {
        const val TAG = "TDR/record"
    }
}
