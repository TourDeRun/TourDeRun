package com.tourderun.core.data.race

import com.tourderun.core.data.Cached
import com.tourderun.domain.model.StageRanking
import com.tourderun.domain.model.TeamStanding
import kotlinx.coroutines.flow.Flow

/**
 * 같은 스테이지 경쟁 (RACE-01·02). S07 홈 경쟁 카드, S15 스테이지 순위가 쓴다.
 *
 * 구현 규칙
 * - **스테이지는 저장하지 않고 계산한다**: `locate(myTeam.distanceM, routeFor(season)).stageIndex`.
 *   스테이지 i의 팀 = 같은 시즌에서 `distanceM`이 [stageStartM(i), stageEndM(i)) 안인 팀(시작 이상, 끝 미만).
 *   마지막 스테이지는 완주 팀(350km 이상)도 포함한다(위치가 부산에 멈추므로).
 * - **순위는 `rankInStage(stageTeams, myTeam.teamId)`** 로만 계산한다(거리 내림차순 → `reachedAt` 이른 팀 → teamId).
 * - **리스너는 스테이지가 바뀔 때만 다시 연결한다.** 같은 스테이지 안에서 거리가 늘어나는 것은 리스너가 알려 준다.
 *   그래서 호출하는 쪽은 내 팀 스테이지가 바뀔 때만 다시 부른다(아래 사용 예).
 * - 다른 스테이지 대체 팀(RACE-02)은 [RaceSummary.ranking]의 ahead(behind)가 null일 때만 조회해서 채운다.
 * - 오류를 던지지 않는다. 연결이 끊기면 마지막 값을 캐시 출처([Cached.isFromCache] = true)로 낸다.
 *
 * 사용 예(ViewModel):
 * ```
 * myTeamFlow
 *     .distinctUntilChangedBy { locate(it.distanceM, route).stageIndex }
 *     .flatMapLatest { raceRepository.observeRace(season = 1, myTeam = it) }
 * ```
 */
interface RaceRepository {
    /**
     * @param myTeam 내 팀. 스테이지 계산과 순위의 기준 팀 ID로 쓴다. 이후 거리 변화는 리스너 값으로 반영된다.
     */
    fun observeRace(season: Int, myTeam: TeamStanding): Flow<Cached<RaceSummary>>
}

/**
 * 내 팀이 있는 스테이지의 경쟁 상황.
 *
 * @property stageIndex 0부터(화면에는 +1)
 * @property stageTeams 같은 스테이지 팀 전부, 순위순(내 팀 포함)
 * @property ranking 내 팀 순위. 스테이지가 바뀌는 순간 옛 목록에 내 팀이 없으면 null(`rankInStage` 참고)
 * @property aheadOtherStage 스테이지 안에 앞 팀이 없을 때만: 앞 스테이지들에서 거리가 가장 가까운 팀(RACE-02). 없으면 null
 * @property behindOtherStage 스테이지 안에 뒤 팀이 없을 때만: 뒤 스테이지들에서 거리가 가장 가까운 팀(RACE-02). 없으면 null
 */
data class RaceSummary(
    val stageIndex: Int,
    val stageTeams: List<TeamStanding>,
    val ranking: StageRanking?,
    val aheadOtherStage: TeamStanding?,
    val behindOtherStage: TeamStanding?,
)
