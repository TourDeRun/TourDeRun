package com.tourderun.core.ui.model

import com.tourderun.core.data.race.MyRace
import com.tourderun.core.data.race.RaceTeam
import com.tourderun.core.designsystem.component.TeamAppearance
import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.designsystem.component.TeamIcon
import com.tourderun.domain.route.routeFor

/**
 * 홈(S07)과 로드뷰(S08)가 같이 쓰는 우리 팀 경쟁 상황.
 *
 * @property rivalsInStage 같은 스테이지의 다른 팀, 순위순
 * @property ahead 바로 앞 팀. 전체 1위면 null
 * @property aheadIsOtherStage [ahead]가 다음 스테이지 팀(스테이지 안에 앞 팀이 없어 대신 보여 주는 팀, RACE-02)
 * @property isOffline 기기 캐시 값
 */
data class MyRaceUi(
    val team: TeamUi,
    val stage: StageProgressUi,
    val rank: Int,
    val totalTeams: Int,
    val rivalsInStage: List<RivalTeamUi>,
    val ahead: RivalTeamUi?,
    val aheadIsOtherStage: Boolean,
    val behind: RivalTeamUi?,
    val behindIsOtherStage: Boolean,
    val isOffline: Boolean,
) {
    /** 지도에 올릴 다른 팀: 같은 스테이지 팀 + 다른 스테이지에서 대신 보여 주는 앞·뒤 팀. */
    val rivalsOnMap: List<RivalTeamUi>
        get() = rivalsInStage +
            listOfNotNull(ahead.takeIf { aheadIsOtherStage }, behind.takeIf { behindIsOtherStage })
}

/** 팀 색·아이콘 번호(0~7, M-06) → 대표 이미지. 범위 밖이면 기본값. */
fun teamAppearanceOf(colorIndex: Int, iconIndex: Int): TeamAppearance = TeamAppearance(
    TeamColor.entries.getOrNull(colorIndex) ?: TeamColor.BLUE,
    TeamIcon.entries.getOrNull(iconIndex) ?: TeamIcon.RUNNER,
)

fun RaceTeam.toRivalUi(): RivalTeamUi =
    RivalTeamUi(standing.name, teamAppearanceOf(color, icon), memberCount, standing.distanceM)

/**
 * [MyRace] → 화면용 값. 팀 거리는 리스너가 준 목록의 우리 팀 거리를 쓴다(연결할 때의 [MyRace.team] 거리는 옛 값일 수 있다).
 *
 * @return 스테이지가 바뀌는 순간처럼 목록에 우리 팀이 없거나 순위가 없으면 null. 호출하는 쪽은 직전 값을 그대로 둔다
 */
fun myRaceUi(myRace: MyRace): MyRaceUi? {
    val summary = myRace.race.value
    val ranking = summary.ranking ?: return null
    val myId = myRace.team.standing.teamId
    val ours = summary.stageTeams.firstOrNull { it.standing.teamId == myId } ?: return null
    return MyRaceUi(
        team = TeamUi(
            name = ours.standing.name,
            appearance = teamAppearanceOf(ours.color, ours.icon),
            season = myRace.team.season,
            memberCount = ours.memberCount,
            distanceM = ours.standing.distanceM,
        ),
        stage = stageProgressUi(ours.standing.distanceM, routeFor(myRace.team.season)),
        rank = ranking.rank,
        totalTeams = ranking.total,
        rivalsInStage = summary.stageTeams.filter { it.standing.teamId != myId }.map { it.toRivalUi() },
        ahead = summary.ahead?.toRivalUi(),
        aheadIsOtherStage = summary.aheadIsOtherStage,
        behind = summary.behind?.toRivalUi(),
        behindIsOtherStage = summary.behindIsOtherStage,
        isOffline = myRace.race.isFromCache,
    )
}
