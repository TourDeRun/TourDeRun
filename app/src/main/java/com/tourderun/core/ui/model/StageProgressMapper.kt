package com.tourderun.core.ui.model

import com.tourderun.domain.journey.locate
import com.tourderun.domain.model.Route
import com.tourderun.domain.model.TeamPosition

/**
 * 팀 거리 → [StageProgressUi] (JRN-02). 홈·로드뷰·러닝·러닝 결과·스테이지 순위가 같이 쓴다.
 *
 * 예: 18_999m → 스테이지 1(서울 → 안양), 안양까지 1m / 19_000m → 스테이지 2 시작, 도착 1
 * / 350_000m 이상 → 스테이지 7, 완주, 도착 7.
 */
fun stageProgressUi(teamDistanceM: Long, route: Route): StageProgressUi =
    locate(teamDistanceM, route).toStageProgressUi(route, teamDistanceM)

/**
 * 이미 계산한 [TeamPosition]으로 만든다. [teamDistanceM]은 이 위치를 계산한 그 팀 거리여야 한다.
 */
fun TeamPosition.toStageProgressUi(route: Route, teamDistanceM: Long): StageProgressUi {
    val stage = route.stages[stageIndex]
    val doneInStageM = if (finished) stage.lengthM else stage.lengthM - toNextM
    return StageProgressUi(
        stageNumber = stageIndex + 1,
        stageCount = route.stages.size,
        fromCity = stage.from.nameKo,
        toCity = stage.to.nameKo,
        startKm = (route.stageStartM(stageIndex) / 1_000L).toInt(),
        endKm = (route.stageEndM(stageIndex) / 1_000L).toInt(),
        teamDistanceM = teamDistanceM.coerceAtLeast(0L),
        doneInStageM = doneInStageM,
        toNextM = toNextM,
        progress = progress.toFloat().coerceIn(0f, 1f),
        arrivals = arrivals,
        finished = finished,
    )
}
