package com.tourderun.domain.record

import com.tourderun.domain.journey.arrivals
import com.tourderun.domain.journey.newArrivals
import com.tourderun.domain.model.City
import com.tourderun.domain.model.Route

/**
 * 러닝 저장 직후 결과 화면(S10) 값 (REC-01). 서버를 기다리지 않고 기기에서 계산한다.
 *
 * @property beforeTeamM 저장 전 팀 거리(캐시된 값)
 * @property afterTeamM 저장 후 팀 거리 = [beforeTeamM] + 이번 러닝 거리
 * @property newCities 이번 러닝으로 새로 도착한 도시(도착 순서). 없으면 빈 리스트
 */
data class SaveOutcome(
    val beforeTeamM: Long,
    val afterTeamM: Long,
    val newCities: List<City>,
)

/**
 * 저장 전 팀 거리와 이번 러닝 거리로 [SaveOutcome]을 만든다.
 * 도착 판정은 `arrivals()`와 같다(도시 누적 거리 이하면 도착, 19,000m = 안양 도착).
 * 이미 완주한 팀은 거리만 늘고 새 도시는 없다.
 *
 * @param beforeTeamM 0 이상
 * @param runM 저장되는 정수 m, 0 이상
 */
fun saveOutcome(beforeTeamM: Long, runM: Long, route: Route): SaveOutcome {
    require(beforeTeamM >= 0) { "저장 전 팀 거리는 0 이상: $beforeTeamM" }
    require(runM >= 0) { "러닝 거리는 0 이상: $runM" }
    val afterTeamM = beforeTeamM + runM
    return SaveOutcome(
        beforeTeamM = beforeTeamM,
        afterTeamM = afterTeamM,
        newCities = newArrivals(arrivals(beforeTeamM, route), arrivals(afterTeamM, route), route),
    )
}
