package com.tourderun.feature.run.ui

import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.model.GpsPoint
import com.tourderun.domain.model.LatLngValue
import com.tourderun.domain.model.TeamStanding
import com.tourderun.feature.run.session.RunPhase
import com.tourderun.feature.run.session.RunState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunUiStateTest {
    private val team = MyTeam(1, TeamStanding("t1", "뛰뛰크루", distanceM = 18_000, reachedAtMs = 0))

    @Test fun `이번 러닝 거리를 팀 거리에 더해 스테이지를 계산한다`() {
        val ui = runningUiState(RunState(phase = RunPhase.RUNNING, distanceM = 1_000.4), team, emptyList())
        assertEquals(19_000L, ui.stageWithRun.teamDistanceM)
        assertEquals(2, ui.stageWithRun.stageNumber)
    }

    @Test fun `페이스가 없으면 0으로 넘겨 화면에서 대시로 표시한다`() {
        val ui = runningUiState(RunState(phase = RunPhase.RUNNING), team, emptyList())
        assertEquals(0L, ui.avgPaceSecPerKm)
        assertEquals(0L, ui.currentPaceSecPerKm)
    }

    @Test fun `GPS 약함과 일시정지를 화면 값으로 옮긴다`() {
        val ui = runningUiState(RunState(phase = RunPhase.PAUSED, gpsWeak = true), team, emptyList())
        assertFalse(ui.gpsGood)
        assertTrue(ui.isPaused)
    }

    @Test fun `미니 지도 경로는 500개로 솎아내고 처음과 끝을 남긴다`() {
        val points = (0 until 1_200).map { GpsPoint(it.toDouble() / 10_000, 0.0, it * 1_000L, 5f) }
        val path = miniMapPath(points)
        assertEquals(MINI_MAP_MAX_POINTS, path.size)
        assertEquals(LatLngValue(0.0, 0.0), path.first())
        assertEquals(LatLngValue(0.1199, 0.0), path.last())
    }

    @Test fun `서비스 시작 실패, 권한 없음 → 권한 안내`() {
        assertEquals(ServiceStartFailureNotice.Permission, serviceStartFailureNotice(hasRunPermissions = false))
    }

    @Test fun `서비스 시작 실패, 권한 있음 → 다시 시도 안내`() {
        assertEquals(ServiceStartFailureNotice.Retry, serviceStartFailureNotice(hasRunPermissions = true))
    }

    @Test fun `일시정지 중 확인 창 요청 → 측정 화면 위에 짧은 러닝 확인 창`() {
        val ui = runScreenUiState(RunState(phase = RunPhase.PAUSED, distanceM = 50.0, movingSec = 30), team, emptyList(), tooShortPrompt = true)
        assertTrue((ui as RunScreenUiState.Running).tooShortPrompt)
    }

    @Test fun `측정 중에는 확인 창 요청이 있어도 보이지 않는다`() {
        val ui = runScreenUiState(RunState(phase = RunPhase.RUNNING), team, emptyList(), tooShortPrompt = true)
        assertFalse((ui as RunScreenUiState.Running).tooShortPrompt)
    }

    @Test fun `기준 미달인 채 종료된 러닝 → 일시정지 화면 + 확인 창`() {
        val ui = runScreenUiState(RunState(phase = RunPhase.FINISHED, distanceM = 50.0, movingSec = 30), team, emptyList(), tooShortPrompt = false)
        val running = ui as RunScreenUiState.Running
        assertTrue(running.tooShortPrompt)
        assertTrue(running.running.isPaused)
    }

    @Test fun `기준을 넘겨 종료된 러닝 → 결과 보기 안내`() {
        val ui = runScreenUiState(RunState(phase = RunPhase.FINISHED, distanceM = 150.0, movingSec = 90), team, emptyList(), tooShortPrompt = false)
        assertNotNull(ui as? RunScreenUiState.Finished)
    }
}
