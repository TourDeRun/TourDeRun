package com.tourderun.core.data.journey

import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.DocWrite
import com.tourderun.core.data.firestore.FieldOp
import com.tourderun.core.data.firestore.MemberFields
import com.tourderun.core.data.firestore.UserFields
import com.tourderun.core.data.firestore.WriteMode

/**
 * [ArrivalRepository.markSeen] 배치 한 번에 쓸 문서 목록(순서대로).
 *
 * ① 내 `teams/{teamId}/members/{uid}`: `seenArrivals = current`만 바꾼다(update).
 *    보안 규칙이 본인에게 허용하는 필드는 `nickname`·`seenArrivals`뿐이다.
 * ② 카드가 있으면 내 `users/{uid}`: `cards.{cityId} = { at: 서버 시각, teamName }` (set merge, EXT-01).
 *    `update`로 `cards` Map을 쓰면 이미 받은 카드가 지워지므로, 중첩 Map + set merge로 안쪽 키만 합친다.
 *
 * @param seen 쓸 자리(uid·팀 ID)와 본 도착 수. `teamName`은 카드에 남길 팀 이름(카드를 받은 시점의 팀)
 * @param newCardCityIds 새로 줄 카드 도시 ID. 같은 ID가 두 번 오면 한 번만 쓴다.
 *   이미 가진 도시를 뺀 목록이어야 한다(`:domain`의 [com.tourderun.domain.card.newCardCityIds] 결과).
 *   가진 도시를 넣으면 첫 획득 기록(`at`·`teamName`)이 덮어써진다
 */
fun arrivalSeenPlan(seen: PendingSeen, newCardCityIds: List<String>): List<DocWrite> {
    val (uid, teamId, teamName, pending) = seen
    val member = DocWrite(
        path = "${Collections.TEAMS}/$teamId/${Collections.MEMBERS}/$uid",
        mode = WriteMode.UPDATE,
        fields = mapOf(MemberFields.SEEN_ARRIVALS to pending.current),
    )
    val cardIds = newCardCityIds.distinct()
    if (cardIds.isEmpty()) return listOf(member)
    val cards = DocWrite(
        path = "${Collections.USERS}/$uid",
        mode = WriteMode.SET_MERGE,
        fields = mapOf(
            UserFields.CARDS to cardIds.associateWith {
                mapOf(UserFields.CARD_AT to FieldOp.ServerTime, UserFields.CARD_TEAM_NAME to teamName)
            },
        ),
    )
    return listOf(member, cards)
}
