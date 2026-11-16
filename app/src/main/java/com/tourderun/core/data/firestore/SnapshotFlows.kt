package com.tourderun.core.data.firestore

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/*
 * Firestore 실시간 리스너 → Flow (쿼리·문서). 수집을 멈추면 리스너를 뗀다.
 *
 * - 메타데이터 변경도 받는다: 데이터가 같아도 온라인·오프라인이 바뀌면 `isFromCache`가 바뀐 값을 다시 낸다.
 * - 오류는 던지지 않는다.
 *   - 첫 값 전 오류: [Loaded.Failed]를 한 번 내고 끝난다(화면이 "불러오는 중"에 멈추지 않게). 다시 시도는 다시 구독.
 *   - 값을 받은 뒤 오류: 로그만 남기고 끝난다. 받는 쪽은 마지막 [Loaded.Ready]를 유지한다.
 */

/** 쿼리 리스너. 문서가 없어도 빈 스냅샷을 낸다. @param tag 로그 태그(`TDR/<feature>`) */
fun Query.snapshotFlow(tag: String): Flow<Loaded<QuerySnapshot>> =
    listenerFlow<QuerySnapshot, FirebaseFirestoreException>(
        register = { onEvent -> addSnapshotListener(MetadataChanges.INCLUDE) { s, e -> onEvent(s, e) } },
        isFromCache = { it.metadata.isFromCache },
        toDataError = { it.code.toDataError() },
        onError = { e, afterValue -> logListenerError(tag, "쿼리", e, afterValue) },
    )

/** 문서 리스너. 문서가 없어도 `exists() == false`인 스냅샷을 낸다. @param tag 로그 태그(`TDR/<feature>`) */
fun DocumentReference.snapshotFlow(tag: String): Flow<Loaded<DocumentSnapshot>> =
    listenerFlow<DocumentSnapshot, FirebaseFirestoreException>(
        register = { onEvent -> addSnapshotListener(MetadataChanges.INCLUDE) { s, e -> onEvent(s, e) } },
        isFromCache = { it.metadata.isFromCache },
        toDataError = { it.code.toDataError() },
        onError = { e, afterValue -> logListenerError(tag, "문서", e, afterValue) },
    )

/** Firestore 오류 코드 → 화면에 알릴 실패 종류. */
fun Code.toDataError(): DataError = dataErrorOf(value())

/**
 * gRPC 상태 번호(`FirebaseFirestoreException.Code.value()`) → 실패 종류.
 * `Code` enum은 초기화에 Android 클래스가 필요해서 JVM 테스트에서 못 만들기 때문에 번호로 나눈다.
 */
internal fun dataErrorOf(codeValue: Int): DataError = when (codeValue) {
    GrpcCode.PERMISSION_DENIED, GrpcCode.UNAUTHENTICATED -> DataError.PermissionDenied
    GrpcCode.UNAVAILABLE, GrpcCode.DEADLINE_EXCEEDED -> DataError.Unavailable
    else -> DataError.Other
}

/** `FirebaseFirestoreException.Code`의 번호(gRPC 상태 코드). */
internal object GrpcCode {
    const val DEADLINE_EXCEEDED = 4
    const val PERMISSION_DENIED = 7
    const val FAILED_PRECONDITION = 9
    const val UNAVAILABLE = 14
    const val UNAUTHENTICATED = 16
}

/**
 * 리스너 공통 처리. Firebase 없이 테스트할 수 있게 등록·오류 변환·로그를 함수로 받는다.
 *
 * @param register 리스너를 붙이고 (스냅샷, 오류) 콜백을 연결한다
 * @param toDataError 첫 값 전 오류 → [Loaded.Failed]에 넣을 실패 종류
 * @param onError 오류 로그. 두 번째 인자는 값을 받은 뒤의 오류인지
 */
internal fun <T : Any, E : Exception> listenerFlow(
    register: (onEvent: (T?, E?) -> Unit) -> ListenerRegistration,
    isFromCache: (T) -> Boolean,
    toDataError: (E) -> DataError,
    onError: (E, afterValue: Boolean) -> Unit,
): Flow<Loaded<T>> = callbackFlow {
    var received = false
    val registration = register { snapshot, error ->
        when {
            error != null -> {
                onError(error, received)
                if (!received) trySend(Loaded.Failed(toDataError(error)))
                // 오류가 난 리스너는 더 값을 내지 않는다.
                close()
            }
            snapshot != null -> {
                received = true
                trySend(Loaded.Ready(snapshot, isFromCache(snapshot)))
            }
        }
    }
    awaitClose { registration.remove() }
}

private fun logListenerError(tag: String, kind: String, e: FirebaseFirestoreException, afterValue: Boolean) {
    val state = if (afterValue) "마지막 값 유지" else "첫 값 전"
    Log.w(tag, "$kind 리스너 오류(${e.code}, $state)", e)
}
