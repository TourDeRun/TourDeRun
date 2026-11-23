package com.tourderun.core.data.card

import com.google.firebase.Timestamp
import com.tourderun.core.data.firestore.UserFields

/**
 * `users/{uid}` 문서 필드 → 가진 카드 목록. 처음 받은 시각 순(같으면 도시 ID 순).
 *
 * - [fields]가 null(문서 없음)이거나 `cards`가 없으면 빈 목록.
 * - 카드 하나가 Map이 아니거나 `at`·`teamName`이 없어도 **카드는 가진 것으로 센다**(시각 0, 팀 이름 "").
 *   빼면 다음 도착 때 그 도시를 새 카드로 다시 써서 첫 기록이 덮어써진다.
 * - `at`은 문서를 읽을 때 서버 시각 추정값(`ServerTimestampBehavior.ESTIMATE`)으로 받은 [Timestamp]를 기대한다.
 *   방금 쓴 대기 쓰기도 기기 추정 시각이 들어온다(`TeamDto.reachedAt`과 같은 방식).
 */
fun ownedCardsOf(fields: Map<String, Any?>?): List<OwnedCard> {
    val cards = fields?.get(UserFields.CARDS) as? Map<*, *> ?: return emptyList()
    return cards.mapNotNull { (key, value) ->
        val cityId = key as? String ?: return@mapNotNull null
        val card = value as? Map<*, *>
        OwnedCard(
            cityId = cityId,
            earnedAtMs = (card?.get(UserFields.CARD_AT) as? Timestamp)?.toDate()?.time ?: 0L,
            teamName = card?.get(UserFields.CARD_TEAM_NAME) as? String ?: "",
        )
    }.sortedWith(compareBy({ it.earnedAtMs }, { it.cityId }))
}

/**
 * users 문서 스냅샷 하나 → 낼 카드 목록. null이면 이 스냅샷은 내지 않는다.
 *
 * 기기 캐시에서 온 "없는 문서"는 null: 캐시에 아직 문서가 없는 것일 수 있어서 "카드 없음"으로 단정하지 않는다
 * (가입한 사용자의 users 문서는 항상 있다). 서버가 확인한 "없는 문서"는 빈 목록.
 */
fun ownedCardsOrNull(exists: Boolean, isFromCache: Boolean, fields: Map<String, Any?>?): List<OwnedCard>? = when {
    !exists && isFromCache -> null
    !exists -> emptyList()
    else -> ownedCardsOf(fields)
}
