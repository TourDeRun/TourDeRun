package com.tourderun.feature.run.session

import com.tourderun.core.data.local.ActiveRunStore
import com.tourderun.core.data.local.RunPointDao
import com.tourderun.core.data.local.RunPointEntity
import com.tourderun.core.data.local.StoredRunSession
import com.tourderun.core.data.local.StoredRunStatus
import com.tourderun.core.data.local.toGpsPoint
import com.tourderun.core.data.local.toRunPointEntity
import com.tourderun.domain.model.GpsPoint
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.run.FilterDecision
import com.tourderun.domain.run.GpsDistanceFilter
import com.tourderun.domain.run.averagePaceSecPerKm
import com.tourderun.domain.run.currentPaceSecPerKm
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** 서비스와 화면이 공유하는 단일 러닝 상태. 시각은 호출자가 전달한다. */
@Singleton
class RunSessionManager @Inject constructor(
    private val pointDao: RunPointDao,
    private val store: ActiveRunStore,
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(RunState())
    val state: StateFlow<RunState> = mutableState.asStateFlow()

    private var filter = GpsDistanceFilter()
    private val accepted = mutableListOf<GpsPoint>()
    private val recent = mutableListOf<GpsPoint>()
    private var session: StoredRunSession? = null
    private var countdownStartedMs = 0L
    private var trackingResumedMs = 0L
    private var lastPointMs: Long? = null
    // 삽입 실패 또는 취소 시 실제 커밋 여부가 불확실하므로 다음 상태 계산 전에 다시 읽는다.
    private var pointsNeedReload = false

    suspend fun beginCountdown(runId: String, nowMs: Long): Boolean = mutex.withLock {
        require(runId.isNotBlank()) { "러닝 ID가 비어 있다" }
        if (mutableState.value.phase != RunPhase.IDLE || store.session.first() != null) return@withLock false
        resetInMemory()
        countdownStartedMs = nowMs
        mutableState.value = RunState(phase = RunPhase.COUNTDOWN, runId = runId, countdownSec = 3)
        true
    }

    /** 1초마다 호출해 카운트다운, 경과 시간과 생존 시각을 갱신한다. */
    suspend fun tick(nowMs: Long) = mutex.withLock {
        when (val phase = mutableState.value.phase) {
            RunPhase.COUNTDOWN -> {
                val passedMs = (nowMs - countdownStartedMs).coerceAtLeast(0)
                if (passedMs < COUNTDOWN_MS) {
                    val remaining = ((COUNTDOWN_MS - passedMs + 999) / 1_000).toInt()
                    mutableState.value = mutableState.value.copy(countdownSec = remaining)
                    return@withLock
                }
                val runId = requireNotNull(mutableState.value.runId)
                val started = StoredRunSession(runId, StoredRunStatus.RUNNING, nowMs, 0, null, nowMs)
                withContext(NonCancellable) {
                    store.save(started)
                    session = started
                    trackingResumedMs = nowMs
                    // 카운트다운 중 GPS는 준비하지만, 시작 전 위치로 거리를 더하지 않는다.
                    updateSnapshot(RunPhase.RUNNING, started, nowMs)
                }
            }
            RunPhase.RUNNING -> {
                var current = session ?: return@withLock
                reloadPointsIfNeeded(current.runId)
                if (nowMs - current.lastHeartbeatMs >= HEARTBEAT_MS) {
                    current = current.copy(lastHeartbeatMs = nowMs)
                    withContext(NonCancellable) {
                        store.save(current)
                        session = current
                    }
                }
                updateSnapshot(phase, current, nowMs)
            }
            else -> Unit
        }
    }

    /** 유효한 원본 위치와 판정 여부를 바로 저장한다. */
    suspend fun onLocation(point: GpsPoint, nowMs: Long) = mutex.withLock {
        if (mutableState.value.phase != RunPhase.RUNNING) return@withLock
        val current = session ?: return@withLock
        reloadPointsIfNeeded(current.runId)
        if (point.timeMs > nowMs || point.timeMs < trackingResumedMs || lastPointMs?.let { point.timeMs <= it } == true) return@withLock
        val decision = filter.accept(point)
        if (decision == FilterDecision.InvalidPoint) return@withLock
        val isAccepted = decision == FilterDecision.FirstPoint || decision is FilterDecision.Accepted
        try {
            pointDao.insert(point.toRunPointEntity(current.runId, current.segment, isAccepted))
        } catch (error: Exception) {
            pointsNeedReload = true
            throw error
        }
        lastPointMs = point.timeMs
        if (isAccepted) {
            // FirstPoint도 경로의 시작점이므로 REC-01에 함께 전달한다.
            accepted.add(point)
        }
        recent.add(point)
        updateSnapshot(RunPhase.RUNNING, current, nowMs)
    }

    suspend fun cancelCountdown(): Boolean = mutex.withLock {
        if (mutableState.value.phase != RunPhase.COUNTDOWN) return@withLock false
        resetInMemory()
        mutableState.value = RunState()
        true
    }

    suspend fun pause(nowMs: Long): Boolean = mutex.withLock {
        if (mutableState.value.phase != RunPhase.RUNNING) return@withLock false
        val previous = session ?: return@withLock false
        reloadPointsIfNeeded(previous.runId)
        if (nowMs < previous.lastHeartbeatMs || nowMs < trackingResumedMs ||
            lastPointMs?.let { nowMs < it } == true
        ) return@withLock false
        val paused = previous.copy(status = StoredRunStatus.PAUSED, pausedAtMs = nowMs, lastHeartbeatMs = nowMs)
        withContext(NonCancellable) {
            store.save(paused)
            session = paused
            recent.clear()
            updateSnapshot(RunPhase.PAUSED, paused, nowMs)
        }
        true
    }

    suspend fun resume(nowMs: Long): Boolean = mutex.withLock {
        if (mutableState.value.phase !in setOf(RunPhase.PAUSED, RunPhase.FINISHED)) return@withLock false
        val previous = session ?: return@withLock false
        val pausedAt = previous.pausedAtMs ?: previous.lastHeartbeatMs
        if (nowMs < pausedAt || nowMs < previous.lastHeartbeatMs) return@withLock false
        val resumed = previous.copy(
            status = StoredRunStatus.RUNNING,
            pausedTotalMs = previous.pausedTotalMs + (nowMs - pausedAt).coerceAtLeast(0),
            pausedAtMs = null,
            lastHeartbeatMs = nowMs,
            segment = previous.segment + 1,
        )
        withContext(NonCancellable) {
            store.save(resumed)
            session = resumed
            filter.resetAnchor()
            recent.clear()
            trackingResumedMs = nowMs
            lastPointMs = null
            updateSnapshot(RunPhase.RUNNING, resumed, nowMs)
        }
        true
    }

    suspend fun finish(nowMs: Long): RunResult? = mutex.withLock {
        if (mutableState.value.phase !in setOf(RunPhase.RUNNING, RunPhase.PAUSED)) return@withLock null
        val previous = session ?: return@withLock null
        reloadPointsIfNeeded(previous.runId)
        if (nowMs < previous.lastHeartbeatMs || nowMs < trackingResumedMs || lastPointMs?.let { nowMs < it } == true ||
            previous.pausedAtMs?.let { nowMs < it } == true
        ) return@withLock null
        val result = resultAt(previous, nowMs)
        val finished = previous.copy(
            status = StoredRunStatus.FINISHED,
            pausedAtMs = previous.pausedAtMs ?: nowMs,
            lastHeartbeatMs = nowMs,
        )
        withContext(NonCancellable) {
            store.save(finished)
            session = finished
            mutableState.value = snapshot(RunPhase.FINISHED, finished, nowMs).copy(result = result)
        }
        result
    }

    /** 사용자가 종료된 러닝을 저장하지 않고 버리기로 선택한 경우에만 호출한다. */
    suspend fun discard(runId: String): Boolean = mutex.withLock {
        if (session?.runId != runId || mutableState.value.phase != RunPhase.FINISHED) return@withLock false
        pointDao.deleteForRun(runId)
        store.clearIfRunIdMatches(runId)
        resetInMemory()
        mutableState.value = RunState()
        true
    }

    private fun resultAt(current: StoredRunSession, end: Long) = RunResult(
        runId = current.runId,
        startedAtMs = current.startedAtMs,
        endedAtMs = end,
        movingSec = movingSecAt(current, end),
        distanceM = filter.totalM,
        acceptedPoints = accepted.toList(),
    )

    private fun movingSecAt(current: StoredRunSession, nowMs: Long): Long {
        val effectiveEnd = if (current.status == StoredRunStatus.RUNNING) nowMs else minOf(nowMs, current.pausedAtMs ?: nowMs)
        return ((effectiveEnd - current.startedAtMs - current.pausedTotalMs).coerceAtLeast(0)) / 1_000L
    }

    private fun updateSnapshot(phase: RunPhase, current: StoredRunSession, nowMs: Long) {
        recent.removeAll { it.timeMs < nowMs - RECENT_WINDOW_MS }
        mutableState.value = snapshot(phase, current, nowMs)
    }

    private fun snapshot(phase: RunPhase, current: StoredRunSession, nowMs: Long): RunState {
        val movingSec = movingSecAt(current, nowMs)
        return RunState(
            phase = phase,
            runId = current.runId,
            distanceM = filter.totalM,
            movingSec = movingSec,
            averagePaceSecPerKm = averagePaceSecPerKm(movingSec, filter.totalM),
            currentPaceSecPerKm = if (phase == RunPhase.RUNNING) currentPaceSecPerKm(recent) else null,
            acceptedPoints = accepted.toList(),
        )
    }

    private suspend fun reloadPointsIfNeeded(runId: String) {
        if (!pointsNeedReload) return
        rebuildFromPoints(pointDao.pointsForRun(runId))
        pointsNeedReload = false
    }

    private fun rebuildFromPoints(rows: List<RunPointEntity>) {
        filter = GpsDistanceFilter()
        accepted.clear()
        recent.clear()
        lastPointMs = null
        var lastSegment: Int? = null
        for (row in rows) {
            if (row.segment != lastSegment) {
                filter.resetAnchor()
                recent.clear()
                lastSegment = row.segment
            }
            val point = row.toGpsPoint()
            val decision = filter.accept(point)
            when (decision) {
                FilterDecision.FirstPoint, is FilterDecision.Accepted -> {
                    accepted.add(point)
                }
                else -> Unit
            }
            if (decision != FilterDecision.InvalidPoint) {
                recent.add(point)
                lastPointMs = point.timeMs
            }
        }
        if (lastSegment != session?.segment) {
            filter.resetAnchor()
            recent.clear()
            lastPointMs = null
        }
    }

    private fun resetInMemory() {
        filter = GpsDistanceFilter()
        accepted.clear()
        recent.clear()
        session = null
        lastPointMs = null
        pointsNeedReload = false
        trackingResumedMs = 0
    }

    private companion object {
        const val COUNTDOWN_MS = 3_000L
        const val HEARTBEAT_MS = 30_000L
        const val RECENT_WINDOW_MS = 30_000L
    }
}
