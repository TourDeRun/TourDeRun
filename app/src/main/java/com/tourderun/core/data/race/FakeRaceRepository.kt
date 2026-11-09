package com.tourderun.core.data.race

import com.tourderun.core.data.Cached
import com.tourderun.domain.journey.locate
import com.tourderun.domain.model.StageRanking
import com.tourderun.domain.model.TeamStanding
import com.tourderun.domain.route.routeFor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 화면 연결용 가짜 구현 (#8). Firestore 구현은 #11.
 *
 * 스테이지 3(오산 → 천안, 48~85km)에 고정 팀 데이터를 낸다. 순위([StageRanking])는 계산하지 않고
 * [scenario]의 고정값을 그대로 쓴다. 목록의 "우리 팀" 항목은 `observeRace`에 넘긴 내 팀의 ID·이름으로 바꿔서 낸다
 * (화면에서 `teamId == myTeam.teamId`로 우리 팀을 표시할 수 있게). 거리는 시나리오 값 그대로다.
 *
 * Hilt는 기본 시나리오([RaceScenario.MIDDLE])로 만든다. 다른 경우는 Preview·테스트에서 직접 만든다:
 * `FakeRaceRepository(RaceScenario.FIRST)`, `FakeRaceRepository(RaceScenario.ALONE, fromCache = true)`.
 */
@Singleton
class FakeRaceRepository(
    private val scenario: RaceScenario,
    private val fromCache: Boolean = false,
) : RaceRepository {

    @Inject constructor() : this(RaceScenario.MIDDLE)

    override fun observeRace(season: Int, myTeam: TeamStanding): Flow<Cached<RaceSummary>> {
        val ourDistanceM = scenario.stageTeams.first { it.teamId == OUR_TEAM_ID }.distanceM
        val stageIndex = locate(ourDistanceM, routeFor(season)).stageIndex
        val summary = RaceSummary(
            stageIndex = stageIndex,
            stageTeams = scenario.stageTeams.map {
                if (it.teamId == OUR_TEAM_ID) it.copy(teamId = myTeam.teamId, name = myTeam.name) else it
            },
            ranking = scenario.ranking,
            aheadOtherStage = scenario.aheadOtherStage,
            behindOtherStage = scenario.behindOtherStage,
        )
        return flowOf(Cached(summary, isFromCache = fromCache))
    }

    companion object {
        /** 시나리오 안의 "우리 팀" 자리표시 ID. 내보낼 때 내 팀 ID로 바뀐다. */
        const val OUR_TEAM_ID = "fake-our-team"
    }
}

/**
 * 가짜 경쟁 상황. [stageTeams]는 순위순, [ranking]은 그 목록에 맞춘 고정값이다(`rankInStage`를 부르지 않는다).
 */
enum class RaceScenario(
    val stageTeams: List<TeamStanding>,
    val ranking: StageRanking,
    val aheadOtherStage: TeamStanding?,
    val behindOtherStage: TeamStanding?,
) {
    /** 4팀 중 2위. 스테이지 안에 앞·뒤 팀이 다 있어서 대체 팀은 없다. */
    MIDDLE(
        stageTeams = listOf(Teams.engineering, Teams.ours(66_800), Teams.beginners, Teams.dawn),
        ranking = StageRanking(rank = 2, total = 4, ahead = Teams.engineering, behind = Teams.beginners),
        aheadOtherStage = null,
        behindOtherStage = null,
    ),

    /** 4팀 중 1위. 앞 팀은 다른 스테이지(스테이지 4)에서 대체. */
    FIRST(
        stageTeams = listOf(Teams.ours(74_100), Teams.engineering, Teams.beginners, Teams.dawn),
        ranking = StageRanking(rank = 1, total = 4, ahead = null, behind = Teams.engineering),
        aheadOtherStage = Teams.nextStage,
        behindOtherStage = null,
    ),

    /** 4팀 중 꼴찌. 뒤 팀은 다른 스테이지(스테이지 2)에서 대체. */
    LAST(
        stageTeams = listOf(Teams.engineering, Teams.beginners, Teams.dawn, Teams.ours(49_200)),
        ranking = StageRanking(rank = 4, total = 4, ahead = Teams.dawn, behind = null),
        aheadOtherStage = null,
        behindOtherStage = Teams.prevStage,
    ),

    /** 스테이지에 우리 팀 혼자. 앞·뒤 모두 다른 스테이지에서 대체. */
    ALONE(
        stageTeams = listOf(Teams.ours(66_800)),
        ranking = StageRanking(rank = 1, total = 1, ahead = null, behind = null),
        aheadOtherStage = Teams.nextStage,
        behindOtherStage = Teams.prevStage,
    ),
}

/** 가짜 팀. reachedAtMs는 순서만 의미 있는 고정값이다. */
private object Teams {
    private const val BASE_MS = 1_794_150_000_000L

    val engineering = TeamStanding("fake-engineering", "공대러너", 69_200, BASE_MS + 1_000)
    val beginners = TeamStanding("fake-beginners", "런린이들", 58_300, BASE_MS + 2_000)
    val dawn = TeamStanding("fake-dawn", "새벽조깅", 50_900, BASE_MS + 3_000)

    /** 스테이지 4(천안 → 세종, 85~125km). 스테이지 3 바로 앞의 가장 가까운 팀. */
    val nextStage = TeamStanding("fake-next-stage", "세종가는길", 88_400, BASE_MS + 4_000)

    /** 스테이지 2(안양 → 오산, 19~48km). 스테이지 3 바로 뒤의 가장 가까운 팀. */
    val prevStage = TeamStanding("fake-prev-stage", "주말러너", 41_700, BASE_MS + 5_000)

    fun ours(distanceM: Long) =
        TeamStanding(FakeRaceRepository.OUR_TEAM_ID, "우리 팀", distanceM, BASE_MS)
}
