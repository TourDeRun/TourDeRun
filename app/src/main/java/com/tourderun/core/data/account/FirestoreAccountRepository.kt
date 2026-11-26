package com.tourderun.core.data.account

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.RunFields
import com.tourderun.core.data.run.RunActivity
import com.tourderun.core.data.firestore.toDataError
import com.tourderun.core.data.DataError
import com.tourderun.core.data.team.TeamRepository
import com.tourderun.core.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** [AccountRepository]의 Firebase 구현. */
@Singleton
class FirestoreAccountRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val teamRepository: TeamRepository,
    private val runActivity: RunActivity,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope,
) : AccountRepository {

    private val failures = MutableSharedFlow<DataError>(extraBufferCapacity = 1)
    override val withdrawFailures: Flow<DataError> = failures.asSharedFlow()

    /**
     * 앱 범위에서 끝까지 진행한다. 2단계에서 팀을 나가면 세션이 NoTeam이 되어 화면이 S03으로 옮겨질 수 있는데,
     * 그때 화면(ViewModel)이 사라져도 탈퇴가 중간에 멈추지 않게 하려는 것이다.
     */
    override suspend fun withdraw(uid: String, teamId: String?, idToken: String): Result<Unit> =
        appScope.async { runWithdraw(uid, teamId, idToken) }.await()

    private suspend fun runWithdraw(uid: String, teamId: String?, idToken: String): Result<Unit> = try {
        withTimeout(TIMEOUT_MS) {
            val user = checkNotNull(auth.currentUser) { "로그인하지 않았다" }
            check(user.uid == uid) { "다른 계정으로 탈퇴하려 한다" }

            // 1. 재인증
            user.reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).await()

            // 2. 팀 나가기 (실패하면 여기서 멈춘다: 팀에 남은 채로 계정만 사라지면 안 된다)
            if (teamId != null) teamRepository.leaveTeam(uid, teamId).getOrThrow()

            // 3. 내 러닝 기록. 목록 조회는 반드시 uid 조건(규칙은 필터가 아니다). 배치는 500개까지라 나눠서 지운다
            val runs = firestore.collection(Collections.RUNS)
                .whereEqualTo(RunFields.UID, uid)
                .get(Source.SERVER).await()
                .documents
            runs.chunked(BATCH_LIMIT).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }

            // 4. 내 users 문서
            firestore.collection(Collections.USERS).document(uid).delete().await()

            // 5. 계정. 이게 끝나면 세션이 SignedOut → S01
            user.delete().await()
        }
        clearCredentialState()
        // 같은 기기에서 다음 계정에 이전 계정 러닝의 복구 안내가 뜨지 않게(RUN-02)
        runActivity.clearDeviceData()
        Result.success(Unit)
    } catch (e: TimeoutCancellationException) {
        failures.tryEmit(e.toDataError())
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "회원 탈퇴 실패", e)
        failures.tryEmit(e.toDataError())
        Result.failure(e)
    }

    /** 다음 로그인 때 계정을 다시 고르게 한다(AUTH-04와 같음). 실패해도 탈퇴는 끝났다. */
    private suspend fun clearCredentialState() {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "계정 선택 기록 지우기 실패")
        }
    }

    private companion object {
        const val TAG = "TDR/auth"
        const val TIMEOUT_MS = 30_000L
        const val BATCH_LIMIT = 450
    }
}
