package com.tourderun.feature.run.ui

import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.model.TeamStanding
import com.tourderun.domain.record.saveOutcome
import com.tourderun.domain.route.routeFor
import com.tourderun.feature.run.data.SavedRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** S10: 저장한 러닝 → 결과 화면 상태. */
class RunResultUiStateTest {

    private val route = routeFor(1)

    private fun team(id: String, distanceM: Long) =
        MyTeam(season = 1, standing = TeamStanding(teamId = id, name = "뛰뛰크루", distanceM = distanceM, reachedAtMs = 0L))

    private fun saved(distanceM: Long, movingSec: Long, team: MyTeam?) = SavedRun(
        title = "11월 18일 저녁 러닝",
        result = RunResult(
            runId = "run1",
            startedAtMs = 0L,
            endedAtMs = movingSec * 1_000,
            movingSec = movingSec,
            distanceM = distanceM.toDouble(),
            acceptedPoints = emptyList(),
        ),
        distanceM = distanceM,
        team = team,
        outcome = team?.let { saveOutcome(it.standing.distanceM, distanceM, route) },
    )

    @Test fun `팀과 함께 저장 → 저장 결과의 팀 거리 전·후`() {
        val state = requireNotNull(runResultUiState(saved(3_240, 1_122, team("t1", 10_000))))
        assertEquals("11월 18일 저녁 러닝", state.title)
        assertEquals(3_240L, state.distanceM)
        assertEquals(1_122L, state.movingSec)
        assertEquals(346L, state.paceSecPerKm)
        assertEquals(10_000L, state.teamBeforeM)
        assertEquals(13_240L, state.stageAfter.teamDistanceM)
        assertNull(state.overtake)
    }

    @Test fun `방금 만든 팀(0km)과 함께 저장 → 0에서 이번 거리까지`() {
        val state = requireNotNull(runResultUiState(saved(3_240, 1_122, team("t1", 0))))
        assertEquals(0L, state.teamBeforeM)
        assertEquals(3_240L, state.stageAfter.teamDistanceM)
        assertEquals(1, state.stageAfter.stageNumber)
    }

    @Test fun `새 도시 도착 → 러닝 후 스테이지가 다음 스테이지`() {
        // 18,000m + 2,000m = 20,000m: 안양(19km) 도착, 스테이지 2
        val state = requireNotNull(runResultUiState(saved(2_000, 600, team("t1", 18_000))))
        assertEquals(18_000L, state.teamBeforeM)
        assertEquals(2, state.stageAfter.stageNumber)
        assertEquals(1, state.stageAfter.arrivals)
    }

    @Test fun `팀 없이 저장 → null(화면을 닫고 홈으로)`() {
        assertNull(runResultUiState(saved(3_240, 1_122, team = null)))
    }

    @Test fun `저장 거리 0m → null`() {
        assertNull(runResultUiState(saved(0, 60, team("t1", 10_000))))
    }
}
