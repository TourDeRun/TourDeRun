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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.tourderun.R
import com.tourderun.domain.model.GpsPoint
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.record.defaultRunTitle
import com.tourderun.domain.stats.SEOUL
import com.tourderun.feature.record.data.RunSaver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * REC-01 저장 확인용. 디버그 빌드에만 있고 런처·앱 시작 경로에 연결되지 않는다.
 * 팀 없이(`team = null`) 저장하므로 `runs`·`users`만 쓴다. `users/{uid}` 문서가 없으면 배치 전체가 거부된다.
 * google-services.json이 없으면 Firebase를 건드리지 않고 안내만 보여준다.
 * 로그인하지 않았으면 저장을 막는다(개발용 보안 규칙도 request.auth가 필요). uid는 화면을 열 때 읽는다.
 */
@AndroidEntryPoint
class DebugSaveRunActivity : ComponentActivity() {

    // Firestore는 FirebaseApp이 있을 때만 만들어야 하므로 Lazy로 받는다.
    @Inject lateinit var runSaver: dagger.Lazy<RunSaver>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firebaseReady = FirebaseApp.getApps(this).isNotEmpty()
        // FirebaseAuth도 FirebaseApp이 있을 때만 건드린다.
        val uid = if (firebaseReady) FirebaseAuth.getInstance().currentUser?.uid else null

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    var status by rememberSaveable { mutableStateOf<String?>(null) }
                    val idle = stringResource(R.string.debug_save_run_idle)
                    val noFirebase = stringResource(R.string.debug_save_run_no_firebase)
                    val notSignedIn = stringResource(R.string.debug_save_run_not_signed_in)
                    val started = stringResource(R.string.debug_save_run_started)
                    val failed = stringResource(R.string.debug_save_run_failed)
                    Column(
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            status ?: when {
                                !firebaseReady -> noFirebase
                                uid == null -> notSignedIn
                                else -> idle
                            },
                        )
                        Button(
                            enabled = uid != null,
                            onClick = {
                                val signedInUid = uid ?: return@Button
                                status = saveFakeRun(signedInUid).fold(
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
    private fun saveFakeRun(uid: String): Result<String> {
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
        return saver.save(result, uid = uid, team = null, title = title).map { result.runId }
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
