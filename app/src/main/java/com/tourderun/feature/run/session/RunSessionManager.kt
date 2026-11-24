package com.tourderun.feature.run.session

import com.tourderun.core.data.local.ActiveRunStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import com.tourderun.core.data.run.RunActivity
import com.tourderun.core.data.local.PendingRunSave
import com.tourderun.core.data.local.PendingSaveState
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
import com.tourderun.domain.run.isGpsWeak
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
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
) : RunActivity {
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
    // RUN-08: 이번 구간(재개 이후)에 마지막으로 채택된 점의 시각
    private var lastAcceptedMs: Long? = null
    // 삽입 실패 또는 취소 시 실제 커밋 여부가 불확실하므로 다음 상태 계산 전에 다시 읽는다.
    private var pointsNeedReload = false

    // ── RUN-02 다른 영역에 알리는 러닝 활동 ────────────────────
    override val isBusy: Flow<Boolean> =
        combine(state, store.session, store.pendingSaves) { current, stored, pending ->
            isRunBusy(current.phase, hasStoredSession = stored != null, pendingSaveCount = pending.size)
        }.distinctUntilChanged()

    override suspend fun clearDeviceData() = mutex.withLock {
        withContext(NonCancellable) {
            pointDao.deleteAll()
            store.clearAll()
            resetInMemory()
            mutableState.value = RunState()
        }
    }

    /**
     * 새 러닝의 카운트다운을 시작한다. 기기에 진행 중인 러닝이 남아 있으면 시작하지 않는다.
     * 저장 대기 러닝([ActiveRunStore.pendingSaves])은 따로 보관하므로 막지 않는다(#99).
     */
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
            lastAcceptedMs = point.timeMs
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
            lastAcceptedMs = null
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

    /**
     * 측정 서비스를 시작하지 못했거나 위치 요청이 실패했을 때 부른다. 위치를 받을 수 없으므로
     * 측정 중이면 일시정지(RUNNING → PAUSED), 카운트다운 중이면 취소(COUNTDOWN → IDLE)한다.
     * 일시정지·종료 상태는 그대로 둔다(종료 결과도 유지).
     *
     * @return 상태를 바꿨으면 true
     */
    suspend fun stopForServiceFailure(nowMs: Long): Boolean = when (state.value.phase) {
        RunPhase.RUNNING -> pause(nowMs)
        RunPhase.COUNTDOWN -> cancelCountdown()
        RunPhase.IDLE, RunPhase.PAUSED, RunPhase.FINISHED -> false
    }

    /**
     * 사용자가 러닝을 저장하지 않고 버리기로 선택한 경우에만 호출한다.
     * 종료 후(짧은 러닝의 "저장 없이 종료")와 복구 안내의 "버리기"(일시정지 상태)에서 쓴다.
     */
    suspend fun discard(runId: String): Boolean = mutex.withLock {
        if (session?.runId != runId || mutableState.value.phase !in setOf(RunPhase.PAUSED, RunPhase.FINISHED)) {
            return@withLock false
        }
        clearLocal(runId)
        true
    }

    /** 저장 대기 러닝(#99). 진행 중인 러닝과 따로 앱 재시작 뒤에도 남는다. */
    val pendingSaves: Flow<List<PendingRunSave>> = store.pendingSaves

    /**
     * REC-01 저장 배치를 시작한 직후 호출한다(#99). 진행 상태를 저장 대기 목록으로 옮기고 메모리 상태는 IDLE로 돌려
     * 서버 확인 전(오프라인)에도 새 러닝을 시작할 수 있게 한다. 위치점은 서버 확인 뒤 [completeSave]가 지운다.
     *
     * @return 지금 종료된 러닝이 [runId]가 아니면 false (이미 정리했거나 다른 러닝이 진행 중)
     */
    suspend fun markSaveStarted(runId: String): Boolean = mutex.withLock {
        if (session?.runId != runId || mutableState.value.phase != RunPhase.FINISHED) return@withLock false
        withContext(NonCancellable) {
            if (!store.moveToPendingSave(runId)) return@withContext false
            resetInMemory()
            mutableState.value = RunState()
            true
        }
    }

    /**
     * REC-01 저장이 서버에 반영된 것을 확인한 뒤 호출한다(#23). [runId]의 위치점과 진행·대기 기록을 지운다.
     * - 아직 종료 화면에 있는 러닝이면 [discard]처럼 메모리 상태까지 지우고 IDLE로 돌아간다.
     * - 저장 대기 러닝이면 그 러닝만 지운다. 지금 측정 중인 다른 러닝의 상태와 경로는 건드리지 않는다.
     *
     * @return 지웠으면 true. 지금 측정 중인 러닝이 [runId]이거나 이미 정리했으면 false
     */
    suspend fun completeSave(runId: String): Boolean = mutex.withLock {
        if (session?.runId == runId) {
            if (mutableState.value.phase != RunPhase.FINISHED) return@withLock false
            clearLocal(runId)
            withContext(NonCancellable) { store.removePendingSave(runId) }
            return@withLock true
        }
        if (store.pendingSaves.first().none { it.runId == runId }) return@withLock false
        withContext(NonCancellable) {
            // 점을 먼저 지운다. 그 사이 앱이 꺼지면 대기 기록이 남아 다음 실행에서 다시 확인한다.
            pointDao.deleteForRun(runId)
            store.removePendingSave(runId)
        }
    }

    /**
     * 서버가 저장을 받아들이지 않은 대기 러닝을 [PendingSaveState.REJECTED]로 보존한다(#99).
     * 진행 중인 러닝이 없을 때 [recoverRejected]로 종료 상태에 올려 다시 저장하거나 버린다.
     *
     * @return 대기 목록에 [runId]가 있으면 true
     */
    suspend fun rejectPendingSave(runId: String): Boolean = mutex.withLock {
        withContext(NonCancellable) { store.setPendingSaveState(runId, PendingSaveState.REJECTED) }
    }

    /**
     * 서버가 거부한 러닝 하나를 원래 runId 그대로 종료(FINISHED) 상태에 올린다(#99). 기기에 남은 위치점으로 거리를 다시 계산한다.
     * 이후 결과 화면에서 다시 저장하거나 [discard]로 버린다. 측정 중이거나 진행 중인 러닝이 있으면 하지 않는다(그 러닝을 먼저 끝낸다).
     *
     * @return 올린 러닝. 없으면 null
     */
    suspend fun recoverRejected(): RecoveredRun? = mutex.withLock {
        if (mutableState.value.phase != RunPhase.IDLE || store.session.first() != null) return@withLock null
        val rejected = store.pendingSaves.first().firstOrNull { it.state == PendingSaveState.REJECTED } ?: return@withLock null
        val restored = withContext(NonCancellable) { store.restorePendingSave(rejected.runId) } ?: return@withLock null
        recoverLocked(restored).copy(saveRejected = true)
    }

    /**
     * 진행 중·저장 대기 어디에도 없는 러닝의 위치점을 지운다(#99). 앱을 열 때 한 번 부른다. 측정 중(IDLE이 아님)이면 하지 않는다.
     * 저장 대기 러닝의 점은 서버 확인([completeSave])이나 버리기([discard]) 전까지 지우지 않는다.
     */
    suspend fun pruneOrphanPoints(): Unit = mutex.withLock {
        if (mutableState.value.phase != RunPhase.IDLE) return@withLock
        val keep = listOfNotNull(store.session.first()?.runId) + store.pendingSaves.first().map { it.runId }
        withContext(NonCancellable) {
            if (keep.isEmpty()) pointDao.deleteAll() else pointDao.deleteExcept(keep)
        }
    }

    /** 앱 프로세스가 살아 있는 동안에는 메모리 상태가 기준이라, IDLE일 때만 기기에 남은 러닝을 본다. */
    suspend fun pendingRecovery(): StoredRunSession? = mutex.withLock {
        if (mutableState.value.phase != RunPhase.IDLE) return@withLock null
        store.session.first()
    }

    /**
     * RUN-07 강제 종료 후 복구. 기기에 남은 위치점으로 거리를 다시 계산하고 일시정지(또는 종료) 상태로 올린다.
     * 저장 대기 러닝은 여기서 다루지 않는다([recoverRejected], `RunSaveCoordinator.settleSaved`).
     * 앱이 꺼져 있던 시간은 일시정지로 본다: 마지막으로 살아 있던 시각(생존 시각·마지막 점 중 늦은 것)에 멈춘 것으로 둔다.
     * 이후 사용자 선택에 따라 [resume](이어하기), [finish]에 [RecoveredRun.endMs](지금까지 저장), [discard](버리기)를 부른다.
     */
    suspend fun recover(): RecoveredRun? = mutex.withLock {
        if (mutableState.value.phase != RunPhase.IDLE) return@withLock null
        val stored = store.session.first() ?: return@withLock null
        recoverLocked(stored)
    }

    /** [stored]를 메모리 상태로 올린다. [mutex] 안에서만 부른다. */
    private suspend fun recoverLocked(stored: StoredRunSession): RecoveredRun {
        resetInMemory()
        session = stored
        rebuildFromPoints(pointDao.pointsForRun(stored.runId))
        val lastAliveMs = maxOf(stored.lastHeartbeatMs, lastPointMs ?: stored.startedAtMs)
        val recovered = when (stored.status) {
            StoredRunStatus.RUNNING -> stored.copy(
                status = StoredRunStatus.PAUSED,
                pausedAtMs = lastAliveMs,
                lastHeartbeatMs = lastAliveMs,
            )
            StoredRunStatus.PAUSED, StoredRunStatus.FINISHED -> stored
        }
        val endMs = recovered.pausedAtMs ?: lastAliveMs
        trackingResumedMs = recovered.startedAtMs
        withContext(NonCancellable) {
            if (recovered != stored) store.save(recovered)
            session = recovered
            mutableState.value = if (recovered.status == StoredRunStatus.FINISHED) {
                snapshot(RunPhase.FINISHED, recovered, endMs).copy(result = resultAt(recovered, endMs))
            } else {
                snapshot(RunPhase.PAUSED, recovered, endMs)
            }
        }
        return RecoveredRun(
            runId = recovered.runId,
            distanceM = filter.totalM,
            movingSec = movingSecAt(recovered, endMs),
            endMs = endMs,
            finished = recovered.status == StoredRunStatus.FINISHED,
        )
    }

    private suspend fun clearLocal(runId: String) {
        withContext(NonCancellable) {
            pointDao.deleteForRun(runId)
            store.clearIfRunIdMatches(runId)
            resetInMemory()
            mutableState.value = RunState()
        }
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
            gpsWeak = phase == RunPhase.RUNNING && isGpsWeak(lastAcceptedMs, trackingResumedMs, nowMs),
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
        lastAcceptedMs = null
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
                    lastAcceptedMs = point.timeMs
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
            lastAcceptedMs = null
        }
    }

    private fun resetInMemory() {
        filter = GpsDistanceFilter()
        accepted.clear()
        recent.clear()
        session = null
        lastPointMs = null
        lastAcceptedMs = null
        pointsNeedReload = false
        trackingResumedMs = 0
    }

    private companion object {
        const val COUNTDOWN_MS = 3_000L
        const val HEARTBEAT_MS = 30_000L
        const val RECENT_WINDOW_MS = 30_000L
    }
}
