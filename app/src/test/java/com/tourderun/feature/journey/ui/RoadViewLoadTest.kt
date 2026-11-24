package com.tourderun.feature.journey.ui

import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.race.MyRace
import com.tourderun.core.data.race.RaceSummary
import com.tourderun.core.data.race.RaceTeam
import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.model.StageRanking
import com.tourderun.domain.model.TeamStanding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** S08: 리스너 값 → 로드뷰 상태 (JRN-03 불러오기 실패). */
class RoadViewLoadTest {

    private val ours = RaceTeam(TeamStanding("me", "팀me", 66_800, reachedAtMs = 0L), icon = 0, color = 0, memberCount = 2)
    private val myTeam = MyTeam(season = 1, standing = ours.standing)

    private fun ready(ranking: StageRanking?) =
        MyRace(myTeam, Loaded.Ready(RaceSummary(2, listOf(ours), ranking, null, null), isFromCache = false))

    @Test fun `첫 값 전 실패 → 실패`() {
        val load = nextRoadViewLoad(RoadViewLoad.Loading, MyRace(myTeam, Loaded.Failed(DataError.PermissionDenied)))

        assertEquals(RoadViewLoad.Failed(DataError.PermissionDenied), load)
    }

    @Test fun `팀이 없으면 불러오는 중`() {
        assertEquals(RoadViewLoad.Loading, nextRoadViewLoad(RoadViewLoad.Loading, null))
    }

    @Test fun `순위가 비는 스테이지 전환 중 → 직전 내용 유지`() {
        val content = nextRoadViewLoad(RoadViewLoad.Loading, ready(StageRanking(1, 1, null, null)))

        assertTrue(content is RoadViewLoad.Content)
        assertEquals(content, nextRoadViewLoad(content, ready(ranking = null)))
    }

    @Test fun `실패 뒤 다시 받으면 내용`() {
        val failed = nextRoadViewLoad(RoadViewLoad.Loading, MyRace(myTeam, Loaded.Failed(DataError.Unavailable)))

        assertTrue(nextRoadViewLoad(failed, ready(StageRanking(1, 1, null, null))) is RoadViewLoad.Content)
    }
}
