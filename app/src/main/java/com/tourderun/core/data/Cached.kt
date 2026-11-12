package com.tourderun.core.data

/**
 * 값과 그 값의 출처. [isFromCache]가 true면 기기 캐시(오프라인)에서 온 값이라 화면에 오프라인 배지를 단다.
 * Firestore 구현에서는 `snapshot.metadata.isFromCache`를 그대로 넣는다.
 */
data class Cached<T>(val value: T, val isFromCache: Boolean)
