package com.tourderun.core.data.firestore

import android.util.Log
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * 쿼리 실시간 리스너를 Flow로 바꾼다. 수집을 멈추면 리스너를 뗀다.
 *
 * - 메타데이터 변경도 받는다: 데이터가 같아도 온라인·오프라인이 바뀌면 `metadata.isFromCache`가 바뀐 스냅샷을 다시 낸다.
 * - 오류는 던지지 않는다: 로그를 남기고 Flow를 정상 종료한다. 받는 쪽은 마지막 값을 유지한다.
 *
 * @param tag 로그 태그(`TDR/<feature>`)
 */
fun Query.snapshotFlow(tag: String): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
        if (error != null) {
            Log.w(tag, "실시간 리스너 오류: ${error.code}", error)
            close()
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}
