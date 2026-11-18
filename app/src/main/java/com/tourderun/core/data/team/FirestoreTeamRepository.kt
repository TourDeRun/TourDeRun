package com.tourderun.core.data.team

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.TeamDto
import com.tourderun.core.data.firestore.batchOf
import com.tourderun.core.data.firestore.snapshotFlow
import com.tourderun.core.data.firestore.toTeamDto
import com.tourderun.core.data.mapReady
import com.tourderun.domain.team.generateInviteCode
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * 팀의 Firestore 구현 (TEAM-01~). 규칙은 [TeamRepository] 참고.
 *
 * - 팀 만들기는 서버 확인이 필요한 일(초대 코드 중복 확인, 만든 직후 S06에 코드 표시)이라 온라인 필수.
 *   초대 코드를 서버에서 확인하는 단계가 먼저라서, 오프라인이면 배치를 시작하기 전에 실패한다
 *   (배치가 기기에 대기했다가 나중에 몰래 올라가는 일이 없다).
 */
@Singleton
class FirestoreTeamRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : TeamRepository {

    private val random = Random.Default

    override fun observeTeam(teamId: String): Flow<Loaded<TeamDto?>> =
        firestore.collection(Collections.TEAMS).document(teamId)
            .snapshotFlow(TAG)
            .map { loaded -> loaded.mapReady { it.toTeamDto() } }
            .distinctUntilChanged()

    override suspend fun createTeam(
        uid: String,
        nickname: String,
        name: String,
        icon: Int,
        color: Int,
    ): Result<CreatedTeam> = runCatching {
        withTimeout(TIMEOUT_MS) {
            val code = newUniqueCode()

            val teamId = firestore.collection(Collections.TEAMS).document().id

            firestore.batchOf(teamCreatePlan(uid, nickname, teamId, name.trim(), icon, color, code)).commit().await()

            CreatedTeam(teamId, code)
        }
    }

    /**
     * 아직 아무 팀도 쓰지 않는 초대 코드 (TEAM-02). 서버 값을 직접 확인한다(캐시는 믿지 않는다).
     * 32글자 6자리라 겹칠 일은 거의 없지만, 겹치면 [MAX_CODE_TRIES]번까지 다시 만든다.
     */
    private suspend fun newUniqueCode(): String {
        repeat(MAX_CODE_TRIES) {
            val code = generateInviteCode(random)
            val exists = firestore.collection(Collections.INVITE_CODES).document(code)
                .get(Source.SERVER).await().exists()

            if (!exists) return code
        }
        error("초대 코드를 ${MAX_CODE_TRIES}번 만들었지만 모두 이미 있다")
    }

    private companion object {
        const val TAG = "TDR/team"
        const val TIMEOUT_MS = 10_000L
        const val MAX_CODE_TRIES = 5
    }
}
