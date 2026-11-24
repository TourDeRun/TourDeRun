package com.tourderun.core.data.team

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.firestore.Collections
import com.tourderun.domain.team.nextLeader
import com.tourderun.domain.team.MemberJoin
import com.tourderun.core.data.firestore.TeamStatFields
import com.tourderun.core.data.firestore.TeamFields
import com.tourderun.core.data.firestore.InviteCodeFields
import com.tourderun.core.data.firestore.UserFields
import com.tourderun.core.data.firestore.applyWrites
import com.tourderun.core.data.firestore.MemberFields
import com.tourderun.core.data.firestore.TeamDto
import com.tourderun.core.data.firestore.TeamMemberDto
import com.tourderun.core.data.firestore.toTeamMemberDto
import com.tourderun.core.data.firestore.batchOf
import com.tourderun.core.data.firestore.snapshotFlow
import com.tourderun.core.data.firestore.toTeamDto
import com.tourderun.core.data.mapReady
import com.tourderun.domain.journey.arrivals
import com.tourderun.domain.route.routeFor
import com.tourderun.domain.team.generateInviteCode
import com.tourderun.domain.team.joinRejection
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 팀의 Firestore 구현 (TEAM-01~). 규칙은 [TeamRepository] 참고.
 *
 * - 팀 만들기는 초대 코드 중복을 서버에서 확인해야 해서 온라인 필수. 시간 제한은 **그 확인 단계에만** 건다.
 *   오프라인·느린 네트워크면 여기서 실패하고 배치는 시작하지 않는다.
 * - 배치는 시작하면 성공으로 본다. 시작하는 순간 기기 캐시에 반영되고(세션이 Ready, S06은 캐시로 코드 표시)
 *   연결되면 서버에 올라간다. 커밋이 늦다고 실패를 띄우면 사용자가 다시 만들어 배치가 두 번 대기하기 때문이다
 *   (오프라인 대기 쓰기 규칙, REC-01과 같다).
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

    override fun observeInviteCode(teamId: String, uid: String): Flow<Loaded<String?>> =
        firestore.collection(Collections.TEAMS).document(teamId)
            .collection(Collections.MEMBERS).document(uid)
            .snapshotFlow(TAG)
            .map { loaded -> loaded.mapReady { it.getString(MemberFields.INVITE_CODE) } }
            .distinctUntilChanged()

    override fun observeMembers(teamId: String): Flow<Loaded<List<TeamMemberDto>>> =
        firestore.collection(Collections.TEAMS).document(teamId)
            .collection(Collections.MEMBERS)
            .snapshotFlow(TAG)
            .map { loaded -> loaded.mapReady { snapshot -> snapshot.documents.mapNotNull { it.toTeamMemberDto() } } }
            .distinctUntilChanged()

    override suspend fun createTeam(
        uid: String,
        nickname: String,
        name: String,
        icon: Int,
        color: Int,
    ): Result<CreatedTeam> = try {
        // 1. 초대 코드 서버 확인. 시간 초과면 배치를 시작하지 않고 끝난다
        val code = withTimeout(CODE_CHECK_TIMEOUT_MS) { newUniqueCode() }
        val teamId = firestore.collection(Collections.TEAMS).document().id

        // 2. 배치 시작. 이 순간 기기 캐시에 반영된다
        val commit = firestore.batchOf(teamCreatePlan(uid, nickname, teamId, name.trim(), icon, color, code)).commit()
            // 3초가 지난 뒤 늦게 오는 실패(규칙 거절 등)는 화면에 알릴 수 없어 로그로만 남긴다 (#91 리뷰)
            .addOnFailureListener { e -> Log.w(TAG, "팀 만들기 배치 실패", e) }

        // 3. 규칙 거절처럼 바로 오는 실패만 잡으려고 잠깐 기다린다. 시간이 넘으면 null(예외 아님) → 성공으로 본다
        withTimeoutOrNull(COMMIT_WAIT_MS) { commit.await() }
        Result.success(CreatedTeam(teamId, code))
    } catch (e: TimeoutCancellationException) {
        // 1번 단계에서 우리가 건 시간 제한 = 연결 문제. CancellationException의 하위라 먼저 잡는다
        Result.failure(e)
    } catch (e: CancellationException) {
        // 화면을 나가 viewModelScope가 취소된 것. 삼키지 않고 위로 보낸다
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun findTeamByCode(code: String): Result<TeamDto?> = try {
        val team = withTimeout(CODE_CHECK_TIMEOUT_MS) {
            val teamId = firestore.collection(Collections.INVITE_CODES).document(code)
                .get(Source.SERVER).await()
                .getString(InviteCodeFields.TEAM_ID)
            teamId?.let {
                firestore.collection(Collections.TEAMS).document(it).get(Source.SERVER).await().toTeamDto()
            }
        }
        Result.success(team)
    } catch (e: TimeoutCancellationException) {
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun joinTeam(uid: String, nickname: String, code: String): Result<JoinOutcome> = try {
        val outcome = withTimeout(CODE_CHECK_TIMEOUT_MS) {
            firestore.runTransaction { tx ->
                // 1. 읽기를 먼저 모두 끝낸다 (트랜잭션은 쓰기 뒤에 읽을 수 없다)
                val teamId = tx.get(firestore.collection(Collections.INVITE_CODES).document(code))
                    .getString(InviteCodeFields.TEAM_ID)
                    ?: return@runTransaction JoinOutcome.NotFound
                val team = tx.get(firestore.collection(Collections.TEAMS).document(teamId)).toTeamDto()
                    ?: return@runTransaction JoinOutcome.NotFound
                val myTeamId = tx.get(firestore.collection(Collections.USERS).document(uid))
                    .getString(UserFields.TEAM_ID)

                // 2. 참여할 수 있는지 (이미 팀 있음 → 가득 참 순서)
                joinRejection(myTeamId, team.memberCount)?.let { return@runTransaction JoinOutcome.Rejected(it) }

                // 3. 쓰기. 가입 전에 지나온 도시는 클리어 연출 대상이 아니라 seenArrivals를 지금 도착 수로 둔다
                val plan = teamJoinPlan(
                    uid = uid,
                    nickname = nickname,
                    teamId = teamId,
                    inviteCode = code,
                    newMemberCount = team.memberCount + 1,
                    seenArrivals = arrivals(team.distanceM, routeFor(NEW_TEAM_SEASON)),
                )
                tx.applyWrites(firestore, plan)
                JoinOutcome.Joined(teamId)
            }.await()
        }
        Result.success(outcome)
    } catch (e: TimeoutCancellationException) {
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun leaveTeam(uid: String, teamId: String): Result<Unit> = try {
        withTimeout(CODE_CHECK_TIMEOUT_MS) {
            val teamRef = firestore.collection(Collections.TEAMS).document(teamId)
            val membersRef = teamRef.collection(Collections.MEMBERS)
            // 트랜잭션은 목록 조회를 못 해서, 팀원 목록과 주간 통계 ID는 서버에서 먼저 읽는다
            val members = membersRef.get(Source.SERVER).await().documents.mapNotNull { it.toTeamMemberDto() }
            val statIds = firestore.collection(Collections.TEAM_STATS)
                .whereEqualTo(TeamStatFields.TEAM_ID, teamId)
                .get(Source.SERVER).await()
                .documents.map { it.id }

            firestore.runTransaction { tx ->
                val team = tx.get(teamRef)
                val me = tx.get(membersRef.document(uid))
                check(team.exists() && me.exists()) { "이미 팀에 없다" }
                val memberCount = (team.getLong(TeamFields.MEMBER_COUNT) ?: 1L).toInt()
                val isLeader = me.getString(MemberFields.ROLE) == MemberFields.ROLE_LEADER

                val kind = when {
                    memberCount <= 1 -> LeaveKind.Last(me.getString(MemberFields.INVITE_CODE), statIds)
                    isLeader -> {
                        val successor = nextLeader(members.map { MemberJoin(it.uid, it.joinedAtMs) }, uid)
                        // 목록을 읽은 뒤 그 사람이 나갔을 수 있으니 트랜잭션 안에서 다시 확인한다
                        checkNotNull(successor?.takeIf { tx.get(membersRef.document(it)).exists() }) { "다음 팀장을 찾지 못했다" }
                            .let { LeaveKind.Leader(memberCount - 1, it) }
                    }
                    else -> LeaveKind.Member(memberCount - 1)
                }
                tx.applyWrites(firestore, teamLeavePlan(uid, teamId, kind))
            }.await()
        }
        Result.success(Unit)
    } catch (e: TimeoutCancellationException) {
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
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
        const val CODE_CHECK_TIMEOUT_MS = 10_000L
        const val COMMIT_WAIT_MS = 3_000L
        const val MAX_CODE_TRIES = 5
    }
}
