package com.tourderun.feature.run.service

import com.tourderun.feature.run.session.FakeActiveRunStore
import com.tourderun.feature.run.session.FakeRunPointDao
import com.tourderun.feature.run.session.RunPhase
import com.tourderun.feature.run.session.RunSessionManager
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunNotificationTest {
    // 12:00:00을 하루 시작 기준 ms로 둔다(날짜는 계산에 영향이 없다).
    private fun at(time: String): Long = LocalTime.parse(time).toSecondOfDay() * 1_000L

    @Test fun `재개하면 정지 시간만큼 기준 시각이 늦어지고, 표시 시간은 이어진다`() = runBlocking {
        val manager = RunSessionManager(FakeRunPointDao(), FakeActiveRunStore())
        manager.beginCountdown("run1", at("11:59:57"))
        manager.tick(at("12:00:00")) // 측정 시작
        val startBase = chronometerBaseMs(at("12:00:00"), manager.state.value.movingSec)

        manager.pause(at("12:01:00")) // 60초 달림
        manager.resume(at("12:01:30")) // 30초 쉼
        val movingSec = manager.state.value.movingSec
        val resumeBase = chronometerBaseMs(at("12:01:30"), movingSec)

        assertEquals(at("12:00:00"), startBase)
        assertEquals(60L, movingSec)
        assertEquals(at("12:00:30"), resumeBase)
        // 크로노미터는 지금 - when을 표시한다: 재개 직후 1:00에서 이어진다.
        assertEquals(60_000L, at("12:01:30") - resumeBase)
    }

    private fun running(distance10M: Long) = NotificationContent(RunPhase.RUNNING, distance10M, countdownSec = 0)

    @Test fun `거리가 바뀌면 마지막 표시 후 4999ms에는 건너뛰고 5000ms에는 새 거리를 표시한다`() {
        var shown = ShownNotification(running(10), atMs = 0)

        assertFalse(shouldUpdateNotification(shown, running(11), nowMs = 1_000))
        assertFalse(shouldUpdateNotification(shown, running(12), nowMs = 4_999))
        assertTrue(shouldUpdateNotification(shown, running(12), nowMs = 5_000))
        // 서비스는 true일 때 그 순간의 새 거리를 표시하고 기준을 바꾼다.
        shown = ShownNotification(running(12), atMs = 5_000)
        assertFalse(shouldUpdateNotification(shown, running(12), nowMs = 20_000))
        assertFalse(shouldUpdateNotification(shown, running(13), nowMs = 9_999))
        assertTrue(shouldUpdateNotification(shown, running(13), nowMs = 10_000))
    }

    @Test fun `일시정지와 재개는 시간이 지나지 않아도 바로 반영한다`() {
        val shownRunning = ShownNotification(running(10), atMs = 1_000)
        val paused = NotificationContent(RunPhase.PAUSED, 10, countdownSec = 0)
        assertTrue(shouldUpdateNotification(shownRunning, paused, nowMs = 1_000))

        val shownPaused = ShownNotification(paused, atMs = 2_000)
        assertTrue(shouldUpdateNotification(shownPaused, running(10), nowMs = 2_000))
    }

    @Test fun `처음 표시는 항상 그린다`() {
        assertTrue(shouldUpdateNotification(null, running(0), nowMs = 0))
    }
}
