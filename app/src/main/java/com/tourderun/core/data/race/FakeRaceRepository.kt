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
 * 스테이지 3(오산 → 천안, 48~85km)에 고정 팀 데이터를 낸다(PR #2 Preview와 같은 이름·색·인원). 순위([StageRanking])는 계산하지 않고
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
        val ourDistanceM = scenario.stageTeams.first { it.standing.teamId == OUR_TEAM_ID }.standing.distanceM
        val stageIndex = locate(ourDistanceM, routeFor(season)).stageIndex
        val summary = RaceSummary(
            stageIndex = stageIndex,
            stageTeams = scenario.stageTeams.map {
                if (it.standing.teamId == OUR_TEAM_ID) {
                    it.copy(standing = it.standing.copy(teamId = myTeam.teamId, name = myTeam.name))
                } else {
                    it
                }
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
    val stageTeams: List<RaceTeam>,
    val ranking: StageRanking,
    val aheadOtherStage: RaceTeam?,
    val behindOtherStage: RaceTeam?,
) {
    /** 5팀 중 2위(PR #2 홈·S15 Preview와 같은 배치). 스테이지 안에 앞·뒤 팀이 다 있어서 대체 팀은 없다. */
    MIDDLE(
        stageTeams = listOf(Teams.orange, Teams.ours(66_800), Teams.purple, Teams.green, Teams.teal),
        ranking = StageRanking(
            rank = 2,
            total = 5,
            ahead = Teams.orange.standing,
            behind = Teams.purple.standing,
        ),
        aheadOtherStage = null,
        behindOtherStage = null,
    ),

    /** 5팀 중 1위. 앞 팀은 다른 스테이지(스테이지 4)에서 대체. */
    FIRST(
        stageTeams = listOf(Teams.ours(74_100), Teams.orange, Teams.purple, Teams.green, Teams.teal),
        ranking = StageRanking(rank = 1, total = 5, ahead = null, behind = Teams.orange.standing),
        aheadOtherStage = Teams.nextStage,
        behindOtherStage = null,
    ),

    /** 5팀 중 꼴찌. 뒤 팀은 다른 스테이지(스테이지 2)에서 대체. */
    LAST(
        stageTeams = listOf(Teams.orange, Teams.purple, Teams.green, Teams.teal, Teams.ours(49_200)),
        ranking = StageRanking(rank = 5, total = 5, ahead = Teams.teal.standing, behind = null),
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

/**
 * 가짜 팀. 이름·색·인원·거리는 PR #2 `PreviewSamples`와 같다. reachedAtMs는 순서만 의미 있는 고정값이다.
 * 아이콘은 화면에서 종류가 구분되게 팀마다 다르게 둔다(우리 팀 0, 주황이들 2, 보라돌이 4, 새벽러너스 5, 한솔이네 7).
 *
 * 색 번호(M-06)는 PR #2 `TeamColor` 순서: 0 blue, 1 navy, 2 teal, 3 green, 4 purple, 5 orange, 6 pink, 7 gray.
 * 아이콘 번호는 `TeamIcon` 순서: 0 runner, 1 bolt, 2 mountain, 3 star, 4 flame, 5 wave, 6 heart, 7 flag.
 */
private object Teams {
    private const val BASE_MS = 1_794_150_000_000L

    private const val ICON_RUNNER = 0
    private const val ICON_BOLT = 1
    private const val ICON_MOUNTAIN = 2
    private const val ICON_FLAME = 4
    private const val ICON_WAVE = 5
    private const val ICON_FLAG = 7
    private const val COLOR_BLUE = 0
    private const val COLOR_TEAL = 2
    private const val COLOR_GREEN = 3
    private const val COLOR_PURPLE = 4
    private const val COLOR_ORANGE = 5
    private const val COLOR_PINK = 6
    private const val COLOR_GRAY = 7

    val orange = team("fake-orange", "주황이들", 69_200, 1_000, ICON_MOUNTAIN, COLOR_ORANGE, members = 3)
    val purple = team("fake-purple", "보라돌이", 65_700, 2_000, ICON_FLAME, COLOR_PURPLE, members = 2)
    val green = team("fake-green", "새벽러너스", 58_300, 3_000, ICON_WAVE, COLOR_GREEN, members = 4)
    val teal = team("fake-teal", "한솔이네", 50_900, 4_000, ICON_FLAG, COLOR_TEAL, members = 1)

    /** 스테이지 4(천안 → 세종, 85~125km). 스테이지 3 바로 앞의 가장 가까운 팀. */
    val nextStage = team("fake-next-stage", "세종가는길", 88_400, 5_000, ICON_BOLT, COLOR_PINK, members = 3)

    /** 스테이지 2(안양 → 오산, 19~48km). 스테이지 3 바로 뒤의 가장 가까운 팀. */
    val prevStage = team("fake-prev-stage", "주말러너", 41_700, 6_000, ICON_FLAG, COLOR_GRAY, members = 2)

    /** 우리 팀 = PR #2 Preview의 "뛰뛰크루"(파랑·러너·4명). 이름과 ID는 내보낼 때 내 팀 것으로 바뀐다. */
    fun ours(distanceM: Long) =
        team(FakeRaceRepository.OUR_TEAM_ID, "우리 팀", distanceM, 0, ICON_RUNNER, COLOR_BLUE, members = 4)

    private fun team(
        teamId: String,
        name: String,
        distanceM: Long,
        reachedOffsetMs: Long,
        icon: Int,
        color: Int,
        members: Int,
    ) = RaceTeam(TeamStanding(teamId, name, distanceM, BASE_MS + reachedOffsetMs), icon, color, members)
}
