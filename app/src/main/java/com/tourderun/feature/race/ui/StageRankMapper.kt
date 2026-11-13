package com.tourderun.feature.race.ui

import com.tourderun.core.data.Cached
import com.tourderun.core.data.race.RaceSummary
import com.tourderun.domain.model.Route

/**
 * 리스너 값 하나 → S15 상태. 스테이지가 바뀌는 순간([RaceSummary.ranking] == null)에는 [previous]를 그대로 둔다
 * (직전 값이 없으면 [StageRankUiState.Loading]).
 */
fun nextStageRankState(
    previous: StageRankUiState,
    race: Cached<RaceSummary>,
    myTeamId: String,
    route: Route,
): StageRankUiState =
    stageRankContent(race, myTeamId, route)
        ?: previous as? StageRankUiState.Content
        ?: StageRankUiState.Loading

/**
 * [RaceSummary] → [StageRankUiState.Content]. 순위는 [RaceSummary.stageTeams] 순서(이미 순위순)를 그대로 쓰고,
 * 헤더의 우리 순위·팀 수도 같은 목록에서 읽어 줄과 헤더가 어긋나지 않게 한다.
 *
 * @return 내 팀 순위가 없거나(스테이지 전환 중) 목록에 내 팀이 없으면 null
 */
fun stageRankContent(
    race: Cached<RaceSummary>,
    myTeamId: String,
    route: Route,
): StageRankUiState.Content? {
    val summary = race.value
    if (summary.ranking == null) return null
    val teams = summary.stageTeams
    val ourIndex = teams.indexOfFirst { it.standing.teamId == myTeamId }
    if (ourIndex < 0) return null

    val stage = route.stages[summary.stageIndex]
    val rows = teams.mapIndexed { i, team ->
        val ahead = teams.getOrNull(i - 1)
        RankRowUi(
            rank = i + 1,
            name = team.standing.name,
            iconIndex = team.icon,
            colorIndex = team.color,
            memberCount = team.memberCount,
            distanceM = team.standing.distanceM,
            gapToAheadM = ahead?.let { (it.standing.distanceM - team.standing.distanceM).coerceAtLeast(0L) },
            isOurTeam = i == ourIndex,
        )
    }
    return StageRankUiState.Content(
        stageNumber = summary.stageIndex + 1,
        fromCity = stage.from.nameKo,
        toCity = stage.to.nameKo,
        startKm = (route.stageStartM(summary.stageIndex) / 1_000L).toInt(),
        endKm = (route.stageEndM(summary.stageIndex) / 1_000L).toInt(),
        ourRank = ourIndex + 1,
        rows = rows,
        isOffline = race.isFromCache,
    )
}
