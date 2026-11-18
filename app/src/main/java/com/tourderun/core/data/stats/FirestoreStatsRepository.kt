package com.tourderun.core.data.stats

import android.util.Log
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.mapReady
import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.TeamFields
import com.tourderun.core.data.firestore.TeamStatFields
import com.tourderun.core.data.firestore.snapshotFlow
import com.tourderun.core.data.firestore.toTeamStatDto
import com.tourderun.domain.stats.lastWeekId
import com.tourderun.domain.stats.weekId
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import com.tourderun.domain.stats.topPercent as calcTopPercent

/**
 * 팀 주간 통계의 Firestore 구현 (#12). 규칙은 [StatsRepository] 참고.
 *
 * - 이번 주 `teamStats/{teamId}_{weekId(nowMs)}`, 지난주 `teamStats/{teamId}_{lastWeekId(nowMs)}` 두 문서 리스너를 합친다.
 * - 상위 %: count 집계 두 개(서버 전용). `teamStats` 쿼리는 `periodId ==` + `distanceM >`만 쓴다
 *   (인덱스가 periodId↑, distanceM↑이고 `season` 조건은 넣지 않는다). 전체 팀 수는 `teams`의 `season == 1`.
 */
class FirestoreStatsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : StatsRepository {

    override fun observeWeekly(teamId: String, nowMs: Long): Flow<Loaded<WeeklyStats>> {
        val thisWeek = weekDoc(teamId, weekId(nowMs)).snapshotFlow(TAG).map { it.mapReady { doc -> doc.toTeamStatDto() } }
        val lastWeek = weekDoc(teamId, lastWeekId(nowMs)).snapshotFlow(TAG).map { it.mapReady { doc -> doc.toTeamStatDto() } }
        return combine(thisWeek, lastWeek) { now, last ->
            // 두 문서 중 하나라도 첫 값 전에 실패하면 화면에 실패를 알린다.
            when (now) {
                is Loaded.Failed -> now
                is Loaded.Ready -> when (last) {
                    is Loaded.Failed -> last
                    is Loaded.Ready -> Loaded.Ready(
                        value = WeeklyStats(
                            thisWeekM = now.value.distanceM,
                            thisWeekRuns = now.value.runCount,
                            lastWeekM = last.value.distanceM,
                            lastWeekRuns = last.value.runCount,
                            memberM = now.value.memberM,
                        ),
                        isFromCache = now.isFromCache || last.isFromCache,
                    )
                }
            }
        }.distinctUntilChanged()
    }

    override suspend fun topPercent(teamId: String, nowMs: Long): TopPercent? {
        val periodId = weekId(nowMs)
        return try {
            // count 집계는 서버에서만 되므로 우리 거리도 서버 값으로 맞춘다. 오프라인이면 예외 → null.
            val ourM = weekDoc(teamId, periodId).get(Source.SERVER).await().toTeamStatDto().distanceM
            if (ourM <= 0) return null
            coroutineScope {
                val moreCount = async {
                    firestore.collection(Collections.TEAM_STATS)
                        .whereEqualTo(TeamStatFields.PERIOD_ID, periodId)
                        .whereGreaterThan(TeamStatFields.DISTANCE_M, ourM)
                        .serverCount()
                }
                val totalTeams = async {
                    firestore.collection(Collections.TEAMS)
                        .whereEqualTo(TeamFields.SEASON, SEASON)
                        .serverCount()
                }
                val total = totalTeams.await()
                if (total <= 0) {
                    null
                } else {
                    val more = moreCount.await()
                    TopPercent(percent = calcTopPercent(more, total), moreCount = more, totalTeams = total)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "상위 % 집계 실패(오프라인이면 숨김)", e)
            null
        }
    }

    private fun weekDoc(teamId: String, periodId: String): DocumentReference =
        firestore.collection(Collections.TEAM_STATS).document(TeamStatFields.docId(teamId, periodId))

    private suspend fun Query.serverCount(): Long = count().get(AggregateSource.SERVER).await().count

    private companion object {
        const val TAG = "TDR/stats"

        /** 상위 %의 전체 팀 수는 시즌 1 팀 전부(STAT-02). */
        const val SEASON = 1
    }
}
