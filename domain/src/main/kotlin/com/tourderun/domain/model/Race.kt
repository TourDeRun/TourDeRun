package com.tourderun.domain.model

/** 순위 계산 입력. Firestore teams 문서에서 RaceRepository가 만든다. */
data class TeamStanding(
    val teamId: String,
    val name: String,
    val distanceM: Long,
    /** 현재 거리에 도달한 서버 시각. 동점이면 작은 쪽이 앞 */
    val reachedAtMs: Long,
)

/**
 * 같은 스테이지 안 순위 (RACE-01·02).
 * ahead/behind가 null이면 RaceRepository가 다른 스테이지에서 가장 가까운 팀을 따로 찾는다.
 */
data class StageRanking(
    val rank: Int,
    val total: Int,
    val ahead: TeamStanding?,
    val behind: TeamStanding?,
)
