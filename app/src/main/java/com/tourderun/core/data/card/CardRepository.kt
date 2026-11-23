package com.tourderun.core.data.card

import com.tourderun.core.data.Loaded
import kotlinx.coroutines.flow.Flow

/**
 * 내가 가진 지역 카드 하나 (EXT-01, `users/{uid}.cards.{cityId}`).
 *
 * @property cityId 도시 ID(`seoul` … `busan`, 경로 도시 ID와 공용)
 * @property earnedAtMs 처음 받은 시각(epoch ms). 서버 시각이 아직 정해지지 않은 대기 쓰기면 기기 추정값, 값이 없으면 0
 * @property teamName 받을 때 소속 팀 이름. 없으면 빈 문자열
 */
data class OwnedCard(
    val cityId: String,
    val earnedAtMs: Long,
    val teamName: String,
)

/**
 * 가진 지역 카드 읽기 (EXT-01). S17 지역 도감·S18 카드 상세와 스테이지 클리어 연출(새 카드 판정)이 쓴다.
 *
 * - uid는 인자로 받는다. ViewModel이 `SessionRepository`에서 읽어 넘긴다.
 * - 실시간 리스너라 카드를 받으면(스테이지 클리어 확인 배치) 바로 새 목록이 온다. 오프라인 대기 쓰기도 바로 보인다.
 * - 실패는 [Loaded.Failed]로 낸다(첫 값 전). 값을 받은 뒤의 오류는 마지막 값을 유지한다(공용 `snapshotFlow`).
 * - 쓰기는 여기 없다. 카드는 도착 확인 배치(`ArrivalRepository.markSeen`)나 팀 만들기·참여 배치에서
 *   [cardWrite]·[cardFields]로 같이 쓴다.
 */
interface CardRepository {
    /**
     * [uid]의 가진 카드. 처음 받은 시각 순(같으면 도시 ID 순). [uid]가 null이면 `Ready(빈 목록)`.
     *
     * 기기 캐시에 users 문서가 없어서 "없는 문서"로 온 첫 값은 내지 않는다(서버 값을 기다린다).
     * 그 값을 "카드 없음"으로 보면 이미 가진 카드를 새 카드로 다시 써서 첫 기록이 덮어써지기 때문이다.
     */
    fun observeOwned(uid: String?): Flow<Loaded<List<OwnedCard>>>
}
