package com.tourderun.domain.race

import com.tourderun.domain.journey.locate
import com.tourderun.domain.model.Route
import com.tourderun.domain.model.StageRanking

/**
 * 같은 스테이지 팀 조회 범위 (RACE-01). `distanceM`이 [startM] 이상, [endExclusiveM] 미만인 팀.
 *
 * @property stageIndex 0부터
 * @property endExclusiveM 상한. 마지막 스테이지는 null(상한 없음): 완주 팀(350km 이상)도 위치가 부산에 멈추므로 같은 스테이지다
 */
data class StageRange(
    val stageIndex: Int,
    val startM: Long,
    val endExclusiveM: Long?,
)

/** 팀 거리 → 그 팀이 있는 스테이지의 조회 범위. 스테이지는 `locate`와 같은 기준(시작 이상, 끝 미만)이다. */
fun stageRange(distanceM: Long, route: Route): StageRange {
    val stageIndex = locate(distanceM, route).stageIndex
    return StageRange(
        stageIndex = stageIndex,
        startM = route.stageStartM(stageIndex),
        endExclusiveM = if (stageIndex == route.stages.lastIndex) null else route.stageEndM(stageIndex),
    )
}

/**
 * 다른 스테이지 대체 팀 조회 조건 (RACE-02). null인 쪽은 스테이지 안에 팀이 있어서 조회하지 않는다.
 *
 * @property aboveM 앞 팀: `distanceM > aboveM` 중 가장 가까운 팀(거리 오름차순 1개)
 * @property belowM 뒤 팀: `distanceM < belowM` 중 가장 가까운 팀(거리 내림차순 1개)
 */
data class FallbackQuery(
    val aboveM: Long?,
    val belowM: Long?,
) {
    companion object {
        val NONE = FallbackQuery(aboveM = null, belowM = null)
    }
}

/**
 * 스테이지 안에 앞(뒤) 팀이 없을 때만 대체 팀을 찾는다. 1위면 앞, 꼴찌면 뒤, 혼자면 둘 다.
 *
 * 스테이지 안에 앞 팀이 없으면 내 거리보다 먼 팀은 모두 다음 스테이지 이후에 있다(같은 거리면 같은 스테이지).
 * 뒤 팀도 마찬가지다. 그래서 거리 조건만으로 다른 스테이지의 가장 가까운 팀이 나온다.
 *
 * @param ranking `rankInStage` 결과. null(목록에 내 팀이 없음)이면 조회하지 않는다
 * @param myDistanceM 리스너가 준 내 팀의 현재 거리
 */
fun fallbackQuery(ranking: StageRanking?, myDistanceM: Long): FallbackQuery {
    if (ranking == null) return FallbackQuery.NONE
    return FallbackQuery(
        aboveM = if (ranking.ahead == null) myDistanceM else null,
        belowM = if (ranking.behind == null) myDistanceM else null,
    )
}
