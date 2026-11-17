package com.tourderun.core.data.journey

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.batchOf
import com.tourderun.core.data.firestore.seenArrivals
import com.tourderun.core.data.firestore.snapshotFlow
import com.tourderun.core.data.session.MyUidSource
import com.tourderun.core.data.team.MyTeam
import com.tourderun.core.data.team.MyTeamSource
import com.tourderun.domain.journey.PendingArrivals
import com.tourderun.domain.journey.pendingArrivals
import com.tourderun.domain.route.routeFor
import java.util.concurrent.atomic.AtomicReference
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
 * - [observePending]: 내 팀([MyTeamSource]의 팀 ID·**캐시된 팀 거리**·시즌)과 내 `teams/{teamId}/members/{uid}`의
 *   `seenArrivals`를 합쳐 `pendingArrivals()`로 계산한다. members 리스너는 uid·팀 ID가 바뀔 때만 다시 연결한다
 *   (팀 거리만 바뀌면 그대로). 리스너 오류·문서 없음·uid 없음·팀 없음이면 null(연출을 띄우지 않는다).
 * - [markSeen]: [arrivalSeenPlan]을 배치로 commit. 오프라인 대기 쓰기라 `await()`하지 않는다. 서버 거부는 로그만.
 *   쓰면 기기 캐시가 바로 바뀌어 members 리스너가 새 `seenArrivals`를 내고, [observePending]은 null이 된다.
 */
@Singleton
class FirestoreArrivalRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val myTeamSource: MyTeamSource,
    private val uidSource: MyUidSource,
) : ArrivalRepository {

    /** [markSeen]이 쓸 대상. [observePending]이 마지막으로 계산한 uid·팀·본 도착 수. */
    private val target = AtomicReference<SeenTarget?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePending(): Flow<PendingArrivals?> = channelFlow {
        // uid·내 팀을 한 번만 모아 members 리스너 대상과 계산에 같이 쓴다.
        val context = combine(uidSource.observeUid(), myTeamSource.observeMyTeam(), ::Pair).stateIn(this)
        val seen = context
            .map { (uid, team) -> memberKeyOf(uid, team) }
            .distinctUntilChanged()
            .flatMapLatest { key -> if (key == null) flowOf(null) else observeSeen(key) }
        combine(context, seen) { (uid, team), seenOf ->
            target.set(seenTargetOf(uid, team, seenOf))
            pendingFor(uid, team, seenOf)
        }.distinctUntilChanged().collect { send(it) }
    }

    override fun markSeen(pending: PendingArrivals, newCardCityIds: List<String>): Result<Unit> {
        val to = target.get()
            ?: return Result.failure(IllegalStateException("확인할 대상이 없다(로그인·팀·members 문서 확인 전)"))
        // 이미 더 많이 봤으면 쓰지 않는다(seenArrivals를 줄이면 연출이 다시 뜬다).
        if (pending.current <= to.seenArrivals) return Result.success(Unit)
        if (newCardCityIds.isNotEmpty()) Log.w(TAG, "지역 카드 쓰기는 아직 없음(#21), 카드 ${newCardCityIds.size}장 무시")
        return try {
            // TODO(EXT-01): #21에서 newCardCityIds를 넘겨 seenArrivals와 같은 배치로 카드를 쓴다.
            val plan = arrivalSeenPlan(to.uid, to.teamId, to.teamName, pending, newCardCityIds = emptyList())
            firestore.batchOf(plan).commit()
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

/** [FirestoreArrivalRepository.markSeen]이 쓸 대상. */
internal data class SeenTarget(val uid: String, val teamId: String, val teamName: String, val seenArrivals: Int)

/** uid나 팀이 없으면 null(members 리스너를 붙이지 않는다). */
internal fun memberKeyOf(uid: String?, team: MyTeam?): MemberKey? =
    if (uid == null || team == null) null else MemberKey(uid, team.standing.teamId)

/**
 * 지금 uid·팀과 같은 자리의 [seen]일 때만 쓴다. 팀이 바뀌는 중에 이전 팀 문서 값이 섞이면 null.
 * uid·팀·[seen] 중 하나라도 없으면 null.
 */
internal fun seenTargetOf(uid: String?, team: MyTeam?, seen: SeenOf?): SeenTarget? {
    val key = memberKeyOf(uid, team) ?: return null
    if (team == null || seen == null || seen.key != key) return null
    return SeenTarget(key.uid, key.teamId, team.standing.name, seen.seenArrivals)
}

/** 못 본 도착. 대상이 없거나([seenTargetOf]가 null) 새 도착이 없으면 null. */
internal fun pendingFor(uid: String?, team: MyTeam?, seen: SeenOf?): PendingArrivals? {
    val to = seenTargetOf(uid, team, seen) ?: return null
    if (team == null) return null
    return pendingArrivals(to.seenArrivals, team.standing.distanceM, routeFor(team.season))
}
