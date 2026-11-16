package com.tourderun.core.data.journey

import com.tourderun.domain.journey.PendingArrivals
import kotlinx.coroutines.flow.Flow

/**
 * 못 본 도착과 확인 처리 (JRN-05 스테이지 클리어, EXT-01 지역 카드).
 *
 * 구현 규칙
 * - 못 본 도착은 내 팀 거리와 내 `members.seenArrivals`로 [com.tourderun.domain.journey.pendingArrivals]가 계산한다.
 *   도착 도시는 저장하지 않는다.
 * - [markSeen]은 오프라인 대기 쓰기라 `await()`하지 않는다. 커밋을 시작하면 바로 [Result.success]를 돌려주고,
 *   배치를 시작하지 못한 경우만 [Result.failure]. 쓰기 내용은 [arrivalSeenPlan]
 * - 오류를 던지지 않는다.
 *
 * 사용 예(홈 ViewModel):
 * ```
 * arrivalRepository.observePending()   // 값이 오면 클리어 연출을 띄운다
 * arrivalRepository.markSeen(pending)  // 연출에서 "확인"을 누르면
 * ```
 */
interface ArrivalRepository {
    /** 아직 연출을 못 본 도착. 새 도착이 없거나 팀이 없으면 null. [markSeen] 뒤에는 null을 낸다. */
    fun observePending(): Flow<PendingArrivals?>

    /**
     * 연출을 봤다고 기록한다: 내 `seenArrivals`를 [PendingArrivals.current]로.
     *
     * @param newCardCityIds 같은 배치에서 줄 지역 카드 도시 ID(EXT-01, #21에서 채운다). 지금은 항상 빈 목록.
     *   이미 가진 도시를 뺀 목록이어야 한다(`:domain`의 [com.tourderun.domain.card.newCardCityIds] 결과).
     *   가진 도시를 넣으면 첫 획득 기록(`at`·`teamName`)이 덮어써진다
     */
    fun markSeen(pending: PendingArrivals, newCardCityIds: List<String> = emptyList()): Result<Unit>
}
