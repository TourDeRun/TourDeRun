package com.tourderun.domain.race

import com.tourderun.domain.journey.locate
import com.tourderun.domain.model.Route
import com.tourderun.domain.model.TeamStanding

/**
 * 이번 러닝으로 같은 스테이지 앞 팀을 추월한 결과 (REC-06 결과 화면, RACE-02).
 *
 * @property teamName 러닝 전 바로 앞에 있던 팀(넘은 팀 중 가장 가까웠던 팀)
 * @property fromRank 러닝 전 스테이지 순위(1부터)
 * @property toRank 러닝 후 같은 목록에서 다시 매긴 순위
 * @property gapBeforeM 러닝 전 그 팀과의 거리 차이(m, 0 이상. 거리가 같았는데 그 팀이 먼저 도달했으면 0)
 * @property leadNowM 지금 그 팀보다 앞선 거리(m, 양수)
 */
data class Overtake(
    val teamName: String,
    val fromRank: Int,
    val toRank: Int,
    val gapBeforeM: Long,
    val leadNowM: Long,
)

/**
 * 러닝 전·후 내 거리로 같은 스테이지 추월을 판정한다. 추월이 없으면 null.
 *
 * - 순위는 [StandingOrder](거리↓ → 도달 시각↑ → teamId↑)로 매긴다. 다른 팀은 [stageTeams]의 값을 그대로 쓰고
 *   내 팀만 러닝 전 값(거리 [beforeM], 도달 시각 [beforeReachedAtMs])과 러닝 후 값(거리 [afterM])으로 바꿔 두 번 매긴다.
 * - 러닝 후 내 도달 시각은 지금(저장 시각)이라 목록의 어느 팀보다 늦다고 본다. 그래서 **거리가 같아지면 추월이 아니다**.
 * - 여러 팀을 한 번에 넘으면 그중 러닝 전 바로 앞 팀(가장 가까웠던 팀)을 보인다. 순위는 넘은 팀 수만큼 오른다.
 * - **이번 러닝으로 스테이지를 넘어가면 null**: 결과 화면의 "N위 → M위"는 지금 스테이지 순위로 읽히는데,
 *   새 스테이지 순위는 이 목록으로 알 수 없다(이전 스테이지 안에서만 계산하면 화면과 맞지 않는다).
 *   완주 뒤(마지막 스테이지에 머묾)는 같은 스테이지로 본다.
 * - [stageTeams]에 내 팀이 있으면 그 값은 쓰지 않는다(저장하면 기기 캐시의 내 거리가 바로 올라 믿을 수 없다).
 *
 * @param stageTeams 러닝 전 내 스테이지의 팀 목록(순서 상관없음, 내 팀이 있어도 되고 없어도 된다)
 * @param beforeReachedAtMs 러닝 전 내 팀의 도달 시각. 러닝 전 거리가 같은 팀과의 앞뒤를 정한다
 * @param route 스테이지를 넘었는지 판단하는 경로
 */
fun overtakeOf(
    stageTeams: List<TeamStanding>,
    myTeamId: String,
    beforeM: Long,
    afterM: Long,
    beforeReachedAtMs: Long,
    route: Route,
): Overtake? {
    if (afterM <= beforeM) return null
    if (locate(beforeM, route).stageIndex != locate(afterM, route).stageIndex) return null

    val others = stageTeams.filter { it.teamId != myTeamId }
    val myBefore = TeamStanding(myTeamId, name = "", distanceM = beforeM, reachedAtMs = beforeReachedAtMs)
    val myAfter = myBefore.copy(distanceM = afterM, reachedAtMs = Long.MAX_VALUE)

    // 러닝 전 내 앞에 있던 팀 중 러닝 후 내 뒤로 간 팀
    val passed = others.filter { StandingOrder.compare(it, myBefore) < 0 && StandingOrder.compare(it, myAfter) > 0 }
    if (passed.isEmpty()) return null
    val closest = passed.sortedWith(StandingOrder).last()

    val fromRank = others.count { StandingOrder.compare(it, myBefore) < 0 } + 1
    val toRank = others.count { StandingOrder.compare(it, myAfter) < 0 } + 1
    return Overtake(
        teamName = closest.name,
        fromRank = fromRank,
        toRank = toRank,
        gapBeforeM = closest.distanceM - beforeM,
        leadNowM = afterM - closest.distanceM,
    )
}
