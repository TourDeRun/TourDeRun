package com.tourderun.core.data.firestore

import com.google.firebase.firestore.DocumentSnapshot

/**
 * `teams/{teamId}/members/{uid}` 필드 → 본 도착 수(JRN-05 `seenArrivals`).
 *
 * - 문서가 없으면([fields]가 null) null: 내 팀 소속이 확인되지 않았으므로 연출을 띄우지 않는다.
 * - 필드가 없거나 숫자가 아니면 0(팀 만들기 기본값과 같다). 음수는 0으로 올린다.
 */
fun seenArrivalsOf(fields: Map<String, Any?>?): Int? {
    if (fields == null) return null
    val value = (fields[MemberFields.SEEN_ARRIVALS] as? Number)?.toLong() ?: 0L
    return value.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
}

/** members 문서 스냅샷 → 본 도착 수. 없는 문서면 null. */
fun DocumentSnapshot.seenArrivals(): Int? = seenArrivalsOf(if (exists()) data.orEmpty() else null)
