package com.tourderun.core.data.journey

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.batchOf
import com.tourderun.core.data.firestore.seenArrivals
import com.tourderun.core.data.firestore.snapshotFlow
import com.tourderun.core.data.team.MyTeam
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.domain.journey.pendingArrivals
import com.tourderun.domain.route.routeFor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 못 본 도착의 Firestore 구현 (JRN-05). 규칙은 [ArrivalRepository] 참고.
 *
 * - [observePending]: 받은 uid, 내 팀([MyTeamSource]의 팀 ID·**캐시된 팀 거리**·시즌), 내 `teams/{teamId}/members/{uid}`의
 *   `seenArrivals`를 합쳐 `pendingArrivals()`로 계산한다. members 리스너는 팀 ID가 바뀔 때만 다시 연결한다
 *   (팀 거리만 바뀌면 그대로). 리스너 오류·문서 없음·uid 없음·팀 없음이면 null(연출을 띄우지 않는다).
 * - [markSeen]: 받은 [PendingSeen]과 새 카드로 [arrivalSeenPlan]을 배치 commit(`seenArrivals`와 `users.cards`를 같은 배치에). 오프라인 대기 쓰기라 `await()`하지 않는다.
 *   서버 거부는 로그만. 쓰면 기기 캐시가 바로 바뀌어 members 리스너가 새 `seenArrivals`를 내고, [observePending]은 null이 된다.
 */
@Singleton
class FirestoreArrivalRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val myTeamSource: MyTeamSource,
) : ArrivalRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePending(uid: String?): Flow<PendingSeen?> {
        if (uid == null) return flowOf(null)
        return channelFlow {
            // 내 팀을 한 번만 모아 members 리스너 대상과 계산에 같이 쓴다.
            val uidAndTeam = myTeamSource.observeMyTeam().map { team -> uid to team }.stateIn(this)
            val seen = uidAndTeam
                .map { (myUid, team) -> memberKeyOf(myUid, team) }
                .distinctUntilChanged()
                .flatMapLatest { key -> if (key == null) flowOf(null) else observeSeen(key) }
            combine(uidAndTeam, seen) { (myUid, team), seenOf -> pendingFor(myUid, team, seenOf) }
                .distinctUntilChanged()
                .collect { send(it) }
        }
    }

    override fun markSeen(seen: PendingSeen, newCardCityIds: List<String>): Result<Unit> {
        return try {
            firestore.batchOf(arrivalSeenPlan(seen, newCardCityIds)).commit()
                .addOnFailureListener { Log.w(TAG, "도착 확인 쓰기 거부", it) }
            Result.success(Unit)
        } catch (e: RuntimeException) {
            Log.w(TAG, "도착 확인 쓰기 시작 실패", e)
            Result.failure(e)
        }
    }

    private fun observeSeen(key: MemberKey): Flow<SeenOf?> =
        memberDoc(key).snapshotFlow(TAG).map { loaded ->
            (loaded as? Loaded.Ready)?.value?.seenArrivals()?.let { SeenOf(key, it) }
        }

    private fun memberDoc(key: MemberKey): DocumentReference =
        firestore.collection(Collections.TEAMS).document(key.teamId)
            .collection(Collections.MEMBERS).document(key.uid)

    private companion object {
        const val TAG = "TDR/journey"
    }
}

/** 내 members 문서 자리: `teams/{teamId}/members/{uid}`. */
internal data class MemberKey(val uid: String, val teamId: String)

/** members 리스너가 받은 본 도착 수와 그 문서 자리. */
internal data class SeenOf(val key: MemberKey, val seenArrivals: Int)

/** uid나 팀이 없으면 null(members 리스너를 붙이지 않는다). */
internal fun memberKeyOf(uid: String?, team: MyTeam?): MemberKey? =
    if (uid == null || team == null) null else MemberKey(uid, team.standing.teamId)

/**
 * 못 본 도착과 확인을 쓸 자리. 지금 uid·팀과 같은 자리의 [seen]일 때만 계산한다:
 * 팀이 바뀌는 중에 이전 팀 문서 값이 섞이면 null. uid·팀·[seen] 중 하나라도 없거나 새 도착이 없으면 null.
 */
internal fun pendingFor(uid: String?, team: MyTeam?, seen: SeenOf?): PendingSeen? {
    val key = memberKeyOf(uid, team) ?: return null
    if (team == null || seen == null || seen.key != key) return null
    val pending = pendingArrivals(seen.seenArrivals, team.standing.distanceM, routeFor(team.season)) ?: return null
    return PendingSeen(key.uid, key.teamId, team.standing.name, pending, team.season)
}
