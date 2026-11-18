package com.tourderun.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.FirebaseApp
import com.tourderun.R
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.SessionState
import com.tourderun.core.data.team.MyTeam
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.domain.model.GpsPoint
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.record.defaultRunTitle
import com.tourderun.domain.stats.SEOUL
import com.tourderun.feature.record.data.RunSaver
import com.tourderun.feature.record.data.saveTeamOf
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * REC-01 저장 확인용. 디버그 빌드에만 있고 런처·앱 시작 경로에 연결되지 않는다.
 * uid·팀 여부는 [SessionRepository]에서 읽는다(FirebaseAuth를 직접 부르지 않는다).
 * - 팀이 없으면(`NoTeam`) `team = null`로 `runs`·`users`만 쓴다.
 * - 팀이 있으면 `MyTeamSource` 값으로 문서 5개를 쓴다([saveTeamOf]의 TODO 참고).
 * - 로그인 전·닉네임 전(`users/{uid}` 문서 없음)이면 저장을 막는다. users 문서가 없으면 배치 전체가 거부된다.
 * google-services.json이 없으면 Firebase를 건드리지 않고 안내만 보여준다.
 */
@AndroidEntryPoint
class DebugSaveRunActivity : ComponentActivity() {

    // Firestore·FirebaseAuth는 FirebaseApp이 있을 때만 만들어야 하므로 Lazy로 받는다.
    @Inject lateinit var runSaver: dagger.Lazy<RunSaver>
    @Inject lateinit var sessionRepository: dagger.Lazy<SessionRepository>
    @Inject lateinit var myTeamSource: MyTeamSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firebaseReady = FirebaseApp.getApps(this).isNotEmpty()
        val sessionFlow = if (firebaseReady) sessionRepository.get().state else MutableStateFlow(SessionState.SignedOut)

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    val session by sessionFlow.collectAsStateWithLifecycle()
                    val myTeam by myTeamSource.observeMyTeam().collectAsState(initial = null)
                    val team = saveTeamOf(session, myTeam)
                    val uid = (session as? SessionState.NoTeam)?.user?.uid
                        ?: (session as? SessionState.Ready)?.user?.uid
                    var status by rememberSaveable { mutableStateOf<String?>(null) }
                    val idle = stringResource(R.string.debug_save_run_idle)
                    val idleTeam = stringResource(R.string.debug_save_run_idle_team, team?.standing?.teamId.orEmpty())
                    val noFirebase = stringResource(R.string.debug_save_run_no_firebase)
                    val notSignedIn = stringResource(R.string.debug_save_run_not_signed_in)
                    val sessionLoading = stringResource(R.string.debug_save_run_session_loading)
                    val needsNickname = stringResource(R.string.debug_save_run_needs_nickname)
                    val started = stringResource(R.string.debug_save_run_started)
                    val failed = stringResource(R.string.debug_save_run_failed)
                    Column(
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            status ?: when {
                                !firebaseReady -> noFirebase
                                session is SessionState.Loading -> sessionLoading
                                session is SessionState.SignedOut -> notSignedIn
                                session is SessionState.NeedsNickname -> needsNickname
                                team != null -> idleTeam
                                else -> idle
                            },
                        )
                        Button(
                            enabled = uid != null,
                            onClick = {
                                val signedInUid = uid ?: return@Button
                                status = saveFakeRun(signedInUid, team).fold(
                                    onSuccess = { runId -> started.format(runId) },
                                    onFailure = { e -> failed.format(e.message) },
                                )
                            },
                        ) { Text(stringResource(R.string.debug_save_run_button)) }
                    }
                }
            }
        }
    }

    /** 30분 동안 5,000.4m를 달린 가짜 러닝. 점은 6,000개라 5,000개로 솎는 경로도 함께 확인한다. */
    private fun saveFakeRun(uid: String, team: MyTeam?): Result<String> {
        val saver = runSaver.get()
        val endedAtMs = System.currentTimeMillis()
        val startedAtMs = endedAtMs - FAKE_MOVING_SEC * 1_000
        val points = List(FAKE_POINT_COUNT) { i ->
            GpsPoint(
                lat = FAKE_START_LAT,
                lng = FAKE_START_LNG + i * FAKE_STEP_LNG,
                timeMs = startedAtMs + i * FAKE_MOVING_SEC * 1_000 / FAKE_POINT_COUNT,
                accuracyM = 5f,
            )
        }
        val result = RunResult(
            runId = saver.newRunId(),
            startedAtMs = startedAtMs,
            endedAtMs = endedAtMs,
            movingSec = FAKE_MOVING_SEC,
            distanceM = 5_000.4,
            acceptedPoints = points,
        )
        val title = defaultRunTitle(startedAtMs, SEOUL)
        return saver.save(result, uid = uid, team = team, title = title).map { result.runId }
    }

    private companion object {
        const val FAKE_MOVING_SEC = 1_800L
        const val FAKE_POINT_COUNT = 6_000
        // 한강공원 근처에서 동쪽으로 약 5km
        const val FAKE_START_LAT = 37.5283
        const val FAKE_START_LNG = 126.9326
        const val FAKE_STEP_LNG = 0.0000094
    }
}
