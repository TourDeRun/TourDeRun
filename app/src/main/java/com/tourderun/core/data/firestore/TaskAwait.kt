package com.tourderun.core.data.firestore

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 서버 응답이 꼭 필요한 **읽기**(count 집계 등)의 결과를 기다린다. 실패하면 Task의 예외를 던진다(Repository에서 잡는다).
 *
 * 오프라인 대기 쓰기(REC-01 저장, 제목·메모 수정, 기록 삭제)에는 쓰지 않는다. 오프라인이면 끝나지 않는다.
 */
suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val error = task.exception
        when {
            error != null -> continuation.resumeWithException(error)
            task.isCanceled -> continuation.cancel()
            else -> continuation.resume(task.result)
        }
    }
}
