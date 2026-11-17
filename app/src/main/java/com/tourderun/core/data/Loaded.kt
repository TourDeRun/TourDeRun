package com.tourderun.core.data

/**
 * 실시간 리스너가 내는 값. 오류도 예외 대신 값([Failed])으로 낸다(#32).
 *
 * - [Ready]: 받은 값과 출처. [Ready.isFromCache]가 true면 기기 캐시(오프라인)에서 온 값이라 화면에 오프라인 배지를 단다.
 *   Firestore 구현에서는 `snapshot.metadata.isFromCache`를 그대로 넣는다.
 * - [Failed]: 첫 값을 받기 전에 리스너가 실패했다. 리스너는 이미 끝났으므로 다시 시도하려면 다시 구독한다.
 *   값을 받은 뒤의 오류는 [Failed]로 내지 않는다(마지막 [Ready]를 유지하고 로그만 남긴다).
 */
sealed interface Loaded<out T> {
    data class Ready<out T>(val value: T, val isFromCache: Boolean) : Loaded<T>

    data class Failed(val error: DataError) : Loaded<Nothing>
}

/** [Ready][Loaded.Ready]의 값을 바꾼다. [Failed][Loaded.Failed]는 그대로. */
inline fun <T, R> Loaded<T>.mapReady(transform: (T) -> R): Loaded<R> = when (this) {
    is Loaded.Ready -> Loaded.Ready(transform(value), isFromCache)
    is Loaded.Failed -> this
}
