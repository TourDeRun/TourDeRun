package com.tourderun.feature.run.ui

import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.race.RaceRepository
import com.tourderun.core.data.race.RaceSummary
import com.tourderun.core.data.race.RaceTeam
import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.model.TeamStanding
import com.tourderun.domain.record.saveOutcome
import com.tourderun.domain.route.routeFor
import com.tourderun.feature.run.data.SavedRun
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** S10: 저장 결과 + 순위 목록 → 추월 카드. */
class OvertakeUiTest {
    private val route = routeFor(1)

    private fun standing(id: String, name: String, distanceM: Long, reachedAtMs: Long = 100L) =
        TeamStanding(teamId = id, name = name, distanceM = distanceM, reachedAtMs = reachedAtMs)

    private fun raceTeam(s: TeamStanding) = RaceTeam(s, icon = 0, color = 0, memberCount = 2)

    /** 뛰뛰크루 66,800m에서 3,240m 달림 → 70,040m */
    private val me = MyTeam(season = 1, standing = standing("t1", "뛰뛰크루", 66_800, reachedAtMs = 500L))

    private val saved = SavedRun(
        title = "11월 23일 저녁 러닝",
        result = RunResult(
            runId = "run1",
            startedAtMs = 0L,
            endedAtMs = 1_122_000L,
            movingSec = 1_122,
            distanceM = 3_240.0,
            acceptedPoints = emptyList(),
        ),
        distanceM = 3_240,
        team = me,
        outcome = saveOutcome(66_800, 3_240, route),
    )

    /** 저장 뒤 캐시라 내 팀은 이미 70,040m로 들어 있다. */
    private val summary = RaceSummary(
        stageIndex = 2,
        stageTeams = listOf(
            raceTeam(standing("t1", "뛰뛰크루", 70_040, reachedAtMs = 9_999)),
            raceTeam(standing("o", "주황이들", 69_200)),
            raceTeam(standing("d", "새벽러너스", 60_000)),
        ),
        ranking = null,
        aheadOtherStage = null,
        behindOtherStage = null,
    )

    @Test fun `추월 있음 → OvertakeUi 값`() {
        assertEquals(
            OvertakeUi("주황이들", fromRank = 2, toRank = 1, gapBeforeM = 2_400, leadNowM = 840),
            overtakeUi(saved, Loaded.Ready(summary, isFromCache = true)),
        )
    }

    @Test fun `순위 목록 없음·실패 → null`() {
        assertNull(overtakeUi(saved, null))
        assertNull(overtakeUi(saved, Loaded.Failed(DataError.Unavailable)))
    }

    @Test fun `목록이 다른 스테이지 것 → null`() {
        assertNull(overtakeUi(saved, Loaded.Ready(summary.copy(stageIndex = 3), isFromCache = false)))
    }

    @Test fun `팀 없이 저장 → null`() {
        assertNull(overtakeUi(saved.copy(team = null, outcome = null), Loaded.Ready(summary, isFromCache = false)))
    }

    @Test fun `loadOvertake - 러닝 전 거리로 리스너를 열고 첫 값으로 계산`() = runBlocking {
        val race = RecordingRace(flowOf(Loaded.Ready(summary, isFromCache = true)))
        assertEquals("주황이들", loadOvertake(saved, race, timeoutMs = 1_000)?.teamName)
        assertEquals(listOf(66_800L), race.openedWithM)
    }

    @Test fun `loadOvertake - 제한 시간 안에 첫 값이 없으면 null`() = runBlocking {
        assertNull(loadOvertake(saved, RecordingRace(flow { awaitCancellation() }), timeoutMs = 20))
        assertNull(loadOvertake(saved, RecordingRace(emptyFlow()), timeoutMs = 20))
    }

    @Test fun `loadOvertake - 실패 → null`() = runBlocking {
        assertNull(loadOvertake(saved, RecordingRace(flowOf(Loaded.Failed(DataError.PermissionDenied))), timeoutMs = 1_000))
    }
}

private class RecordingRace(private val values: Flow<Loaded<RaceSummary>>) : RaceRepository {
    val openedWithM = mutableListOf<Long>()
    override fun observeRace(season: Int, myTeam: TeamStanding): Flow<Loaded<RaceSummary>> {
        openedWithM += myTeam.distanceM
        return values
    }
}
