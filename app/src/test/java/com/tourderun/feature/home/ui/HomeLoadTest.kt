package com.tourderun.feature.home.ui

import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.race.MyRace
import com.tourderun.core.data.race.RaceSummary
import com.tourderun.core.data.race.RaceTeam
import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.model.StageRanking
import com.tourderun.domain.model.TeamStanding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** S07: 리스너 값 → 홈 상태 (JRN-03 불러오기 실패). */
class HomeLoadTest {

    private val ours = RaceTeam(TeamStanding("me", "팀me", 66_800, reachedAtMs = 0L), icon = 0, color = 0, memberCount = 2)
    private val myTeam = MyTeam(season = 1, standing = ours.standing)

    private fun ready(ranking: StageRanking?) =
        MyRace(myTeam, Loaded.Ready(RaceSummary(2, listOf(ours), ranking, null, null), isFromCache = false))

    private val ranked = ready(StageRanking(1, 1, null, null))
    private val weekly = WeeklySummaryUi(42_500, 9, null, null, null)

    @Test fun `경쟁 상황 첫 값 전 실패 → 실패`() {
        val race = nextHomeRace(HomeRaceLoad.Loading, MyRace(myTeam, Loaded.Failed(DataError.Unavailable)))

        assertEquals(HomeRaceLoad.Failed(DataError.Unavailable), race)
        assertEquals(HomeLoad.Failed(DataError.Unavailable), homeLoad(race, WeeklyLoad.Loading))
    }

    @Test fun `팀이 없으면 불러오는 중`() {
        assertEquals(HomeRaceLoad.Loading, nextHomeRace(HomeRaceLoad.Loading, null))
    }

    @Test fun `순위가 비는 스테이지 전환 중 → 직전 내용 유지`() {
        val content = nextHomeRace(HomeRaceLoad.Loading, ranked)

        val next = nextHomeRace(content, ready(ranking = null))

        assertTrue(content is HomeRaceLoad.Content)
        assertEquals(content, next)
    }

    @Test fun `직전 내용 없이 순위가 비면 불러오는 중`() {
        assertEquals(HomeRaceLoad.Loading, nextHomeRace(HomeRaceLoad.Loading, ready(ranking = null)))
    }

    @Test fun `실패 뒤 다시 받으면 내용`() {
        val failed = nextHomeRace(HomeRaceLoad.Loading, MyRace(myTeam, Loaded.Failed(DataError.Other)))

        assertTrue(nextHomeRace(failed, ranked) is HomeRaceLoad.Content)
    }

    @Test fun `이번 주만 실패 → 홈은 보이고 이번 주 자리에 실패`() {
        val race = nextHomeRace(HomeRaceLoad.Loading, ranked)

        val ui = (homeLoad(race, WeeklyLoad.Failed(DataError.PermissionDenied)) as HomeLoad.Content).ui

        assertNull(ui.weekly)
        assertEquals(DataError.PermissionDenied, ui.weeklyError)
    }

    @Test fun `이번 주를 받으면 카드 값이 있고 실패는 없다`() {
        val race = nextHomeRace(HomeRaceLoad.Loading, ranked)

        val ui = (homeLoad(race, WeeklyLoad.Content(weekly)) as HomeLoad.Content).ui

        assertEquals(weekly, ui.weekly)
        assertNull(ui.weeklyError)
    }

    @Test fun `이번 주를 받기 전이면 카드를 숨긴다`() {
        val race = nextHomeRace(HomeRaceLoad.Loading, ranked)

        val ui = (homeLoad(race, WeeklyLoad.Loading) as HomeLoad.Content).ui

        assertNull(ui.weekly)
        assertNull(ui.weeklyError)
    }
}
