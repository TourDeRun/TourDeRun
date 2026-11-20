package com.tourderun.feature.run.data

import android.util.Log
import com.tourderun.core.data.record.RunSaver
import com.tourderun.core.data.record.saveTeamOf
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.SessionState
import com.tourderun.core.data.session.uid
import com.tourderun.core.data.team.MyTeam
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.core.di.ApplicationScope
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.record.SaveOutcome
import com.tourderun.domain.record.defaultRunTitle
import com.tourderun.domain.stats.SEOUL
import com.tourderun.feature.run.session.RunPhase
import com.tourderun.feature.run.session.RunSessionManager
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 저장을 시작한 러닝 (S10 화면 값의 재료).
 *
 * @property distanceM 저장한 거리(정수 m). `RunSavePlan.savedDistanceM()`과 같은 반올림
 * @property team 저장에 넘긴 팀. 팀 없이 저장했거나 이전 실행에서 저장한 러닝이면 null
 * @property outcome [team]과 함께 저장했을 때의 화면 값(저장 전·후 팀 거리, 새 도착 도시). [team]이 null이면 null
 */
data class SavedRun(
    val title: String,
    val result: RunResult,
    val distanceM: Long,
    val team: MyTeam?,
    val outcome: SaveOutcome?,
)

/**
 * 종료된 러닝 저장과 서버 확인 뒤 기기 데이터 정리 (REC-01, #23).
 *
 * 결과 화면(S10) ViewModel이 사라져도 서버 확인을 받아 [RunSessionManager.completeSave]를 불러야 해서 앱 범위 객체로 둔다.
 * 한 러닝 ID는 한 번만 저장한다(화면 회전·결과 화면 재진입에서 다시 저장하지 않는다).
 */
@Singleton
class RunSaveCoordinator @Inject constructor(
    private val manager: RunSessionManager,
    // Firestore·FirebaseAuth는 google-services.json이 없으면 만들 수 없어서 필요할 때만 꺼낸다.
    private val runSaver: Lazy<RunSaver>,
    private val session: Lazy<SessionRepository>,
    private val myTeamSource: MyTeamSource,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val started = mutableMapOf<String, SavedRun>()

    /**
     * 지금 종료(FINISHED) 상태인 러닝을 저장한다. 같은 러닝을 이미 저장했으면 저장하지 않고 그때 값을 돌려준다.
     * 서버가 배치를 받아들이면 [RunSessionManager.completeSave]로 기기에 남긴 위치점·진행 상태를 지운다.
     *
     * @return 저장할 러닝이 없거나(종료 상태가 아님) 로그인 전이거나 저장을 시작하지 못하면(거리 0 등) null
     */
    suspend fun saveFinished(): SavedRun? = mutex.withLock {
        val state = manager.state.value
        val result = state.result?.takeIf { state.phase == RunPhase.FINISHED } ?: return null
        started[result.runId]?.let { return it }

        val saver = runCatching { runSaver.get() }.getOrElse { e ->
            Log.w(TAG, "Firestore 없이 저장할 수 없음", e)
            return null
        }
        val sessionState = currentSession() ?: return null
        val uid = sessionState.uid ?: return null
        val team = saveTeamOf(sessionState, myTeamSource.observeMyTeam().first())
        val title = defaultRunTitle(result.startedAtMs, SEOUL)
        val runId = result.runId
        // 실패 로그는 RunSaver가 남긴다.
        val outcome = saver.save(result, uid, team, title, onServerConfirmed = {
            appScope.launch { manager.completeSave(runId) }
        }).getOrElse { return null }
        SavedRun(title, result, savedDistanceM(result), team, outcome).also { started[runId] = it }
    }

    /**
     * 앱을 다시 켰을 때 `RunSessionManager.recover()`로 올린 종료 러닝이 이미 저장을 시작한 러닝인지 본다.
     * `runs/{runId}`가 기기 캐시에 있으면 다시 저장하지 않고, 대기 중인 쓰기가 서버에 반영되길 기다렸다가
     * [RunSessionManager.completeSave]를 부른다(기다리는 동안 이 함수는 바로 돌아온다).
     *
     * @return 이미 저장을 시작한 러닝이면 true(복구 안내를 띄우지 않는다)
     */
    suspend fun settleIfSavedOnDevice(runId: String): Boolean {
        val saver = runCatching { runSaver.get() }.getOrNull() ?: return false
        if (!saver.isSavedOnDevice(runId)) return false
        mutex.withLock {
            val result = manager.state.value.result?.takeIf { it.runId == runId }
            if (result != null && runId !in started) {
                // 이전 실행에서 저장한 러닝. 결과 화면을 다시 열면 이 값으로 그린다(팀 값은 모름).
                started[runId] = SavedRun(defaultRunTitle(result.startedAtMs, SEOUL), result, savedDistanceM(result), null, null)
            }
        }
        appScope.launch {
            if (saver.awaitPendingWrites()) manager.completeSave(runId)
        }
        return true
    }

    /** 세션 상태가 정해질 때까지 잠깐 기다린다. Firebase가 없거나 시간이 지나면 null. */
    private suspend fun currentSession(): SessionState? {
        val repository = runCatching { session.get() }.getOrElse { e ->
            Log.w(TAG, "세션 없이 저장할 수 없음", e)
            return null
        }
        return withTimeoutOrNull(SESSION_WAIT_MS) { repository.state.first { it != SessionState.Loading } }
    }

    private companion object {
        const val TAG = "TDR/run"
        const val SESSION_WAIT_MS = 5_000L

        /** `feature/record/data/RunSavePlan.kt`의 `savedDistanceM()`과 같은 규칙(다른 feature라 import하지 않는다). */
        fun savedDistanceM(result: RunResult): Long = result.distanceM.roundToLong()
    }
}
