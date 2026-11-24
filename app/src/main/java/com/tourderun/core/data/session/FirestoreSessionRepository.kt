package com.tourderun.core.data.session

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.mapReady
import com.tourderun.core.data.firestore.batchOf
import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.UserFields
import com.tourderun.core.data.firestore.snapshotFlow
import com.tourderun.core.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** [SessionRepository]의 진짜 구현. 로그인 상태 → 내 users 문서를 실시간으로 따라간다 (AUTH-01). */
@OptIn(ExperimentalCoroutinesApi::class)   // flatMapLatest가 "실험적" 표시라 붙인다
@Singleton
class FirestoreSessionRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope,
) : SessionRepository {

    // ── 1. 로그인 상태를 Flow로 ───────────────────────────────
    /** 로그인하면 uid, 로그아웃하면 null을 낸다. */
    private fun authUidFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    // ── 2. users 문서 스냅샷 → SessionState ──────────────────
    private fun toSessionState(uid: String, snapshot: DocumentSnapshot): SessionState {

        if (!snapshot.exists()) {
            return if (snapshot.metadata.isFromCache) SessionState.Loading
            else SessionState.NeedsNickname(uid)
        }

        val nickname = snapshot.getString(UserFields.NICKNAME)
            ?: return SessionState.NeedsNickname(uid)

        val user = SessionUser(uid = uid, nickname = nickname)
        val teamId = snapshot.getString(UserFields.TEAM_ID)

        return if (teamId == null) SessionState.NoTeam(user)
        else SessionState.Ready(user, teamId)
    }

    // ── 3. 둘을 이어서 앱 전체에 공개 ────────────────────────
    override val state: StateFlow<SessionState> = authUidFlow()
        .flatMapLatest { uid ->
            if (uid == null) {
                flowOf(SessionState.SignedOut)
            } else {
                firestore.collection(Collections.USERS).document(uid)
                    .snapshotFlow(TAG)
                    .map { loaded ->
                        when (loaded) {
                            is Loaded.Ready -> toSessionState(uid, loaded.value)
                            // 첫 값 전에 리스너가 실패함(권한 거절 등). 로그는 snapshotFlow가 남긴다.
                            // TODO(AUTH-01): 로딩 화면에 다시 시도 버튼을 달고 여기서 재구독한다.
                            is Loaded.Failed -> SessionState.Loading
                        }
                    }
            }
        }
        .stateIn(appScope, SharingStarted.Eagerly, SessionState.Loading)

    // ── 4. S01 로그인 ────────────────────────────────────────
    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        // 화면을 나가 취소된 것. 실패로 바꾸지 않고 위로 보낸다
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── 5. S02 users 문서 만들기 ─────────────────────────────
    override suspend fun createUser(nickname: String): Result<Unit> = try {
        val uid = requireNotNull(auth.currentUser?.uid) { "로그인하지 않은 상태에서 createUser를 불렀다" }
        val ref = firestore.collection(Collections.USERS).document(uid)
        val data = mapOf(
            UserFields.NICKNAME to nickname,
            UserFields.CREATED_AT to FieldValue.serverTimestamp(),
            UserFields.TERMS_AGREED_AT to FieldValue.serverTimestamp(),
            UserFields.TEAM_ID to null,
            UserFields.TOTAL_M to 0L,
            UserFields.RUN_COUNT to 0L,
            UserFields.CARDS to emptyMap<String, Any>(),
        )
        withTimeout(CREATE_TIMEOUT_MS) {
            firestore.runTransaction { tx ->
                // 이미 문서가 있으면(다른 기기에서 먼저 만듦) 덮어쓰지 않는다
                if (!tx.get(ref).exists()) tx.set(ref, data)
            }.await()
        }
        Result.success(Unit)
    } catch (e: TimeoutCancellationException) {
        // 우리가 건 10초 제한 = 연결 문제. CancellationException의 하위라 먼저 잡는다
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── 7. AUTH-03 닉네임 바꾸기 ─────────────────────────────
    override fun updateNickname(uid: String, teamId: String?, nickname: String): Result<Unit> = try {
        firestore.batchOf(nicknameUpdatePlan(uid, teamId, nickname)).commit()
            .addOnFailureListener { e -> Log.w(TAG, "닉네임 바꾸기 실패", e) }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── 8. S13 누적 거리·러닝 횟수 ───────────────────────────
    override fun observeMyStats(uid: String): Flow<Loaded<MyStats>> =
        firestore.collection(Collections.USERS).document(uid)
            .snapshotFlow(TAG)
            .map { loaded ->
                loaded.mapReady {
                    MyStats(
                        totalM = it.getLong(UserFields.TOTAL_M) ?: 0L,
                        runCount = it.getLong(UserFields.RUN_COUNT) ?: 0L,
                    )
                }
            }
            .distinctUntilChanged()

    // ── 6. 로그아웃 ──────────────────────────────────────────
    override fun signOut() {
        auth.signOut()   // state는 리스너가 알아서 SignedOut으로 바꾼다
        // AUTH-04: 기기에 남은 "마지막으로 고른 구글 계정"도 지워서, 다음 로그인 때 계정을 다시 고르게 한다.
        // 화면을 기다리게 하지 않으려고 앱 범위 코루틴에서 따로 한다. 실패해도 로그아웃은 이미 끝났다.
        appScope.launch {
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: ClearCredentialException) {
                Log.w(TAG, "계정 선택 기록 지우기 실패: ${e.type}")
            }
        }
    }

    private companion object {
        const val TAG = "TDR/auth"
        const val CREATE_TIMEOUT_MS = 10_000L
    }
}