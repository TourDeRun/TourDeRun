package com.tourderun.core.ui.model

import com.tourderun.core.data.Cached
import com.tourderun.core.data.race.MyRace
import com.tourderun.core.data.race.RaceSummary
import com.tourderun.core.data.race.RaceTeam
import com.tourderun.core.data.team.MyTeam
import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.designsystem.component.TeamIcon
import com.tourderun.domain.model.StageRanking
import com.tourderun.domain.model.TeamStanding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MyRaceUiTest {
    private fun team(id: String, distanceM: Long, color: Int = 0, icon: Int = 0, members: Int = 2) =
        RaceTeam(TeamStanding(id, "팀$id", distanceM, reachedAtMs = 0L), icon, color, members)

    private val ours = team("me", 66_800, color = 2, icon = 3, members = 4)
    private val front = team("a", 69_200, color = 5)
    private val back = team("b", 65_700)

    /** [connectedDistanceM] = 리스너를 연결할 때의 내 팀 거리(옛 값일 수 있다). */
    private fun myRace(
        stageTeams: List<RaceTeam>,
        ranking: StageRanking?,
        aheadOther: RaceTeam? = null,
        behindOther: RaceTeam? = null,
        fromCache: Boolean = false,
        connectedDistanceM: Long = 66_800,
    ) = MyRace(
        MyTeam(season = 1, standing = TeamStanding("me", "팀me", connectedDistanceM, 0L)),
        Cached(RaceSummary(2, stageTeams, ranking, aheadOther, behindOther), isFromCache = fromCache),
    )

    private fun ranking(rank: Int, total: Int, ahead: RaceTeam?, behind: RaceTeam?) =
        StageRanking(rank, total, ahead?.standing, behind?.standing)

    @Test fun `스테이지 중간이면 순위와 앞뒤 팀이 같은 스테이지에서 온다`() {
        val ui = requireNotNull(myRaceUi(myRace(listOf(front, ours, back), ranking(2, 3, front, back))))
        assertEquals(2, ui.rank)
        assertEquals(3, ui.totalTeams)
        assertEquals("팀a", ui.ahead?.name)
        assertEquals("팀b", ui.behind?.name)
        assertFalse(ui.aheadIsOtherStage)
        assertFalse(ui.behindIsOtherStage)
        assertEquals(listOf("팀a", "팀b"), ui.rivalsInStage.map { it.name })
        assertEquals(ui.rivalsInStage, ui.rivalsOnMap)
    }

    @Test fun `우리 팀 정보는 목록의 값으로 채운다`() {
        val ui = requireNotNull(myRaceUi(myRace(listOf(front, ours, back), ranking(2, 3, front, back))))
        assertEquals("팀me", ui.team.name)
        assertEquals(4, ui.team.memberCount)
        assertEquals(TeamColor.entries[2], ui.team.appearance.color)
        assertEquals(TeamIcon.entries[3], ui.team.appearance.icon)
        assertEquals(3, ui.stage.stageNumber)
    }

    @Test fun `팀 거리는 연결할 때 값이 아니라 리스너가 준 목록의 값`() {
        val moved = team("me", 70_000)
        val ui = requireNotNull(
            myRaceUi(myRace(listOf(moved, front), ranking(1, 2, null, front), connectedDistanceM = 66_800)),
        )
        assertEquals(70_000L, ui.team.distanceM)
        assertEquals(70_000L, ui.stage.teamDistanceM)
    }

    @Test fun `스테이지에 우리뿐이면 다른 스테이지 팀을 대신 보이고 지도에도 올린다`() {
        val next = team("n", 90_000)
        val prev = team("p", 40_000)
        val ui = requireNotNull(myRaceUi(myRace(listOf(ours), ranking(1, 1, null, null), next, prev)))
        assertEquals("팀n", ui.ahead?.name)
        assertTrue(ui.aheadIsOtherStage)
        assertEquals("팀p", ui.behind?.name)
        assertTrue(ui.behindIsOtherStage)
        assertTrue(ui.rivalsInStage.isEmpty())
        assertEquals(listOf("팀n", "팀p"), ui.rivalsOnMap.map { it.name })
    }

    @Test fun `앱에 팀이 하나뿐이면 앞뒤 팀이 없다`() {
        val ui = requireNotNull(myRaceUi(myRace(listOf(ours), ranking(1, 1, null, null))))
        assertNull(ui.ahead)
        assertNull(ui.behind)
        assertTrue(ui.rivalsOnMap.isEmpty())
    }

    @Test fun `스테이지 전환 중이라 순위가 없으면 null`() {
        assertNull(myRaceUi(myRace(listOf(front, back), ranking = null)))
    }

    @Test fun `목록에 우리 팀이 없으면 null`() {
        assertNull(myRaceUi(myRace(listOf(front, back), ranking(1, 2, null, back))))
    }

    @Test fun `캐시 값이면 오프라인`() {
        val ui = requireNotNull(myRaceUi(myRace(listOf(ours), ranking(1, 1, null, null), fromCache = true)))
        assertTrue(ui.isOffline)
    }

    @Test fun `색과 아이콘 번호가 범위 밖이면 기본값`() {
        val appearance = teamAppearanceOf(colorIndex = 99, iconIndex = -1)
        assertEquals(TeamColor.BLUE, appearance.color)
        assertEquals(TeamIcon.RUNNER, appearance.icon)
    }
}
