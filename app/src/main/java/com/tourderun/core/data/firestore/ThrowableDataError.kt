package com.tourderun.core.data.firestore

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.tourderun.core.data.DataError
import kotlinx.coroutines.TimeoutCancellationException

/**
 * 예외 → 화면에 알릴 실패 종류. Firebase 예외 타입은 여기서만 본다.
 * ViewModel은 Repository가 돌려준 `Result`의 실패에 이 함수를 써서 문구만 고른다(Firebase를 몰라도 된다).
 *
 * - Firestore 오류 코드: 권한 거절 → [DataError.PermissionDenied], 연결 문제 → [DataError.Unavailable]
 * - 네트워크 예외, 제한 시간 초과(`withTimeout`) → [DataError.Unavailable]
 */
fun Throwable.toDataError(): DataError = when (this) {
    is FirebaseFirestoreException -> code.toDataError()
    is FirebaseNetworkException -> DataError.Unavailable
    is TimeoutCancellationException -> DataError.Unavailable
    else -> DataError.Other
}
