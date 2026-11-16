package com.tourderun.feature.record.data

import com.tourderun.core.data.firestore.MemberFields
import com.tourderun.core.data.firestore.RunFields
import com.tourderun.core.data.firestore.TeamFields
import com.tourderun.core.data.firestore.TeamStatFields
import com.tourderun.core.data.firestore.UserFields
import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.model.GpsPoint
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.model.TeamStanding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunSavePlanTest {
    // 2026-11-15(일) 23:50 KST 시작 → 11-16(월) 00:20 KST 끝. 시작은 2026-W46, 끝은 2026-W47
    private val startMs = 1_794_754_200_000L
    private val endMs = 1_794_756_000_000L

    private val team = MyTeam(
        season = 1,
        standing = TeamStanding(teamId = "t1", name = "뛰뛰크루", distanceM = 66_800, reachedAtMs = 0L),
    )

    private fun result(distanceM: Double = 5_000.5) = RunResult(
        runId = "r1",
        startedAtMs = startMs,
        endedAtMs = endMs,
        movingSec = 1_800,
        distanceM = distanceM,
        acceptedPoints = listOf(GpsPoint(37.5, 127.0, startMs, 5f), GpsPoint(37.51, 127.01, endMs, 5f)),
    )

    private fun plan(team: MyTeam? = this.team, r: RunResult = result()) =
        runSavePlan(r, savedDistanceM(r), uid = "u1", team = team, title = "일요일 밤 러닝")

    private fun List<DocWrite>.at(path: String) = single { it.path == path }

    @Test fun `팀 있음 → 문서 5개, runs·teams·members·teamStats·users 순서`() {
        val writes = plan()

        assertEquals(
            listOf("runs/r1", "teams/t1", "teams/t1/members/u1", "teamStats/t1_2026-W46", "users/u1"),
            writes.map { it.path },
        )
        assertEquals(
            listOf(WriteMode.SET, WriteMode.UPDATE, WriteMode.UPDATE, WriteMode.SET_MERGE, WriteMode.UPDATE),
            writes.map { it.mode },
        )
    }

    @Test fun `teams 필드 → distanceM·reachedAt·lastRunId 정확히 세 개`() {
        val fields = plan().at("teams/t1").fields

        assertEquals(setOf(TeamFields.DISTANCE_M, TeamFields.REACHED_AT, TeamFields.LAST_RUN_ID), fields.keys)
        assertEquals(FieldOp.ServerTime, fields[TeamFields.REACHED_AT])
        assertEquals("r1", fields[TeamFields.LAST_RUN_ID])
    }

    @Test fun `members 필드 → contributionM·lastRunId 정확히 두 개`() {
        val fields = plan().at("teams/t1/members/u1").fields

        assertEquals(setOf(MemberFields.CONTRIBUTION_M, MemberFields.LAST_RUN_ID), fields.keys)
        assertEquals("r1", fields[MemberFields.LAST_RUN_ID])
    }

    @Test fun `teamStats 필드 → 일곱 개, memberM은 중첩 Map, 점 표기 키 없음`() {
        val fields = plan().at("teamStats/t1_2026-W46").fields

        assertEquals(
            setOf(
                TeamStatFields.DISTANCE_M, TeamStatFields.RUN_COUNT, TeamStatFields.MEMBER_M, TeamStatFields.LAST_RUN_ID,
                TeamStatFields.TEAM_ID, TeamStatFields.SEASON, TeamStatFields.PERIOD_ID,
            ),
            fields.keys,
        )
        assertEquals(mapOf("u1" to FieldOp.Increment(5_001)), fields[TeamStatFields.MEMBER_M])
        assertEquals(FieldOp.Increment(1), fields[TeamStatFields.RUN_COUNT])
        assertEquals("t1", fields[TeamStatFields.TEAM_ID])
        assertEquals(1, fields[TeamStatFields.SEASON])
        assertEquals("2026-W46", fields[TeamStatFields.PERIOD_ID])
        assertEquals("r1", fields[TeamStatFields.LAST_RUN_ID])
        assertEquals(emptyList<String>(), plan().flatMap { it.fields.keys }.filter { '.' in it })
    }

    @Test fun `users 필드 → totalM·runCount 두 개`() {
        val fields = plan().at("users/u1").fields

        assertEquals(setOf(UserFields.TOTAL_M, UserFields.RUN_COUNT), fields.keys)
        assertEquals(FieldOp.Increment(1), fields[UserFields.RUN_COUNT])
    }

    @Test fun `자정을 넘긴 러닝 → runs·teamStats 모두 시작 시각의 주`() {
        val writes = plan()

        assertEquals("2026-W46", writes.at("runs/r1").fields[RunFields.PERIOD_ID])
        assertEquals("teamStats/t1_2026-W46", writes[3].path)
    }

    @Test fun `거리 5000점5m → 반올림한 5001이 5개 문서에서 모두 같다`() {
        val writes = plan(r = result(5_000.5))
        val inc = FieldOp.Increment(5_001)

        assertEquals(5_001L, writes.at("runs/r1").fields[RunFields.DISTANCE_M])
        assertEquals(inc, writes.at("teams/t1").fields[TeamFields.DISTANCE_M])
        assertEquals(inc, writes.at("teams/t1/members/u1").fields[MemberFields.CONTRIBUTION_M])
        assertEquals(inc, writes.at("teamStats/t1_2026-W46").fields[TeamStatFields.DISTANCE_M])
        assertEquals(mapOf("u1" to inc), writes.at("teamStats/t1_2026-W46").fields[TeamStatFields.MEMBER_M])
        assertEquals(inc, writes.at("users/u1").fields[UserFields.TOTAL_M])
    }

    @Test fun `팀 없음 → runs와 users 문서 2개, runs의 teamId는 null 명시`() {
        val writes = plan(team = null)

        assertEquals(listOf("runs/r1", "users/u1"), writes.map { it.path })
        val run = writes.at("runs/r1").fields
        assertEquals(true, RunFields.TEAM_ID in run)
        assertNull(run[RunFields.TEAM_ID])
    }

    @Test fun `runs 문서 → 필드 열한 개, null 필드도 명시`() {
        val run = plan().at("runs/r1").fields

        assertEquals(
            setOf(
                RunFields.UID, RunFields.TEAM_ID, RunFields.STARTED_AT, RunFields.ENDED_AT, RunFields.MOVING_SEC,
                RunFields.DISTANCE_M, RunFields.PACE_SEC_PER_KM, RunFields.PERIOD_ID, RunFields.TITLE, RunFields.MEMO,
                RunFields.PATH,
            ),
            run.keys,
        )
        assertEquals("t1", run[RunFields.TEAM_ID])
        assertEquals(FieldOp.Time(startMs), run[RunFields.STARTED_AT])
        assertEquals(FieldOp.Time(endMs), run[RunFields.ENDED_AT])
        assertEquals(true, RunFields.MEMO in run)
        assertNull(run[RunFields.MEMO])
        // 1,800초 / 5.001km = 359.9… → 360
        assertEquals(360L, run[RunFields.PACE_SEC_PER_KM])
    }

    @Test fun `1800초·5000m → 페이스 360초, Long으로 저장`() {
        val run = plan(r = result(5_000.0)).at("runs/r1").fields

        assertEquals(360L, run[RunFields.PACE_SEC_PER_KM])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `저장 거리 0 → 예외`() {
        runSavePlan(result(0.4), distanceM = 0, uid = "u1", team = team, title = "x")
    }
}
