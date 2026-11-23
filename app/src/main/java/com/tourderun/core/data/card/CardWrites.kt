package com.tourderun.core.data.card

import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.DocWrite
import com.tourderun.core.data.firestore.FieldOp
import com.tourderun.core.data.firestore.UserFields
import com.tourderun.core.data.firestore.WriteMode

/*
 * 지역 카드 쓰기 (EXT-01). 도착 확인 배치(`arrivalSeenPlan`)와 팀 만들기·참여 배치(서울 카드)가 같이 쓴다.
 * 어느 쪽이든 카드 도시 ID는 `:domain`의 `newCardCityIds()` 결과(이미 가진 도시를 뺀 목록)를 넘긴다.
 */

/**
 * users 문서에 합칠 카드 필드: `cards.{cityId} = { at: 서버 시각, teamName }`. 같은 ID가 두 번 와도 한 번만.
 *
 * **`WriteMode.SET_MERGE`로만 쓴다.** 중첩 Map이라 set merge에서는 안쪽 키만 합쳐지지만,
 * `WriteMode.UPDATE`에 넣으면 `cards` 필드 전체가 이 값으로 바뀌어 이미 받은 카드가 지워진다.
 * 그래서 다른 필드를 update하는 배치(예: 팀 만들기의 `users.teamId`)에서는 [cardWrite]로 같은 문서에 한 줄 더 쓴다.
 *
 * @param cityIds 새로 줄 카드 도시 ID. 이미 가진 도시를 넣으면 첫 획득 기록(`at`·`teamName`)이 덮어써진다
 * @param teamName 카드에 남길 팀 이름(카드를 받는 시점의 팀)
 */
fun cardFields(cityIds: List<String>, teamName: String): Map<String, Any?> = mapOf(
    UserFields.CARDS to cityIds.distinct().associateWith {
        mapOf(UserFields.CARD_AT to FieldOp.ServerTime, UserFields.CARD_TEAM_NAME to teamName)
    },
)

/**
 * 카드를 주는 `users/{uid}` 쓰기 하나([cardFields], set merge). [cityIds]가 비면 null(쓸 것 없음).
 * 배치 계획(`List<DocWrite>`)에 그대로 더한다: `listOfNotNull(..., cardWrite(uid, ids, teamName))`.
 */
fun cardWrite(uid: String, cityIds: List<String>, teamName: String): DocWrite? {
    if (cityIds.isEmpty()) return null
    return DocWrite(
        path = "${Collections.USERS}/$uid",
        mode = WriteMode.SET_MERGE,
        fields = cardFields(cityIds, teamName),
    )
}
