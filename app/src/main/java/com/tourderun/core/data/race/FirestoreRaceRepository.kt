package com.tourderun.core.data.race

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.mapReady
import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.TeamFields
import com.tourderun.core.data.firestore.snapshotFlow
import com.tourderun.core.data.firestore.toRaceTeam
import com.tourderun.core.data.firestore.toTeamDto
import com.tourderun.domain.model.StageRanking
import com.tourderun.domain.model.TeamStanding
import com.tourderun.domain.race.FallbackQuery
import com.tourderun.domain.race.StageRange
import com.tourderun.domain.race.StandingOrder
import com.tourderun.domain.race.fallbackQuery
import com.tourderun.domain.race.rankInStage
import com.tourderun.domain.race.stageRange
import com.tourderun.domain.route.routeFor
import dagger.Lazy
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * 같은 스테이지 경쟁의 Firestore 구현 (#11). 규칙은 [RaceRepository] 참고.
 *
 * - 같은 스테이지 리스너: `season ==`, `distanceM` 범위([stageRange]), `distanceM` 내림차순 → `reachedAt` 오름차순.
 *   이 함수는 스테이지가 정해진 뒤 한 번만 리스너를 붙인다. 스테이지가 바뀌면 호출하는 쪽이 다시 부른다.
 * - 대체 팀(RACE-02): 스테이지 안에 앞(뒤) 팀이 없을 때만([fallbackQuery]) `limit(1)` 리스너를 붙인다.
 *   조회 조건(내 거리, 1위·꼴찌 여부)이 바뀔 때만 다시 연결한다.
 * - 필요한 복합 인덱스(teams): (season↑, distanceM↓, reachedAt↑), (season↑, distanceM↑), (season↑, distanceM↓)
 *
 * - 오류: 같은 스테이지 리스너가 첫 값 전에 실패하면 [Loaded.Failed]. 대체 팀 리스너 실패는 대체 팀 없음(null)으로 둔다.
 *
 * 같은 쿼리를 두 번 수집해도 Firestore SDK가 리스너를 하나로 합치므로 서버 연결은 하나다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreRaceRepository @Inject constructor(
    // google-services.json이 없으면 만들 수 없어서 리스너·조회를 시작할 때 처음 꺼낸다.
    private val firestoreProvider: Lazy<FirebaseFirestore>,
) : RaceRepository {

    private val firestore: FirebaseFirestore get() = firestoreProvider.get()

    override fun observeRace(season: Int, myTeam: TeamStanding): Flow<Loaded<RaceSummary>> {
        val range = stageRange(myTeam.distanceM, routeFor(season))
        val stage = stageTeams(season, range).map { loaded -> loaded.mapReady { it.toStageState(myTeam) } }
        val others = stage
            .map { (it as? Loaded.Ready)?.value?.fallback ?: FallbackQuery.NONE }
            .distinctUntilChanged()
            .flatMapLatest { query ->
                combine(nearestAhead(season, query.aboveM), nearestBehind(season, query.belowM)) { ahead, behind ->
                    OtherStageTeams(query, ahead, behind)
                }
            }
        return combine(stage, others) { loaded, o ->
            when (loaded) {
                // 같은 스테이지 목록을 못 받으면 화면에 실패를 알린다(대체 팀 실패는 대체 팀 없음으로 둔다).
                is Loaded.Failed -> loaded
                is Loaded.Ready -> {
                    val s = loaded.value
                    // 두 리스너 값이 엇갈려 도착하면(조건이 막 바뀐 순간) 옛 조건의 대체 팀은 쓰지 않는다.
                    val current = o.takeIf { it.query == s.fallback }
                    Loaded.Ready(
                        value = RaceSummary(
                            stageIndex = range.stageIndex,
                            stageTeams = s.teams,
                            ranking = s.ranking,
                            aheadOtherStage = current?.ahead?.value,
                            behindOtherStage = current?.behind?.value,
                        ),
                        isFromCache = loaded.isFromCache ||
                            current?.ahead?.isFromCache == true ||
                            current?.behind?.isFromCache == true,
                    )
                }
            }
        }.distinctUntilChanged()
    }

    private fun stageTeams(season: Int, range: StageRange): Flow<Loaded<List<RaceTeam>>> {
        var query = teamsOf(season).whereGreaterThanOrEqualTo(TeamFields.DISTANCE_M, range.startM)
        range.endExclusiveM?.let { query = query.whereLessThan(TeamFields.DISTANCE_M, it) }
        return query
            .orderBy(TeamFields.DISTANCE_M, Query.Direction.DESCENDING)
            // teams 문서는 생성 때부터 reachedAt이 있어야 한다. 이 orderBy는 필드가 없는 문서를 결과에서 뺀다.
            .orderBy(TeamFields.REACHED_AT, Query.Direction.ASCENDING)
            .snapshotFlow(TAG)
            .map { loaded ->
                // 대기 쓰기의 추정 시각·teamId 동점까지 rankInStage와 같은 순서로 맞춘다.
                loaded.mapReady { snapshot -> snapshot.toRaceTeams().sortedWith(compareBy(StandingOrder) { it.standing }) }
            }
            .distinctUntilChanged()
    }

    /** 내 거리보다 먼 팀 중 가장 가까운 팀. [aboveM]이 null이면 조회하지 않는다. */
    private fun nearestAhead(season: Int, aboveM: Long?): Flow<Loaded.Ready<RaceTeam?>?> =
        if (aboveM == null) {
            flowOf(null)
        } else {
            nearest(
                teamsOf(season)
                    .whereGreaterThan(TeamFields.DISTANCE_M, aboveM)
                    .orderBy(TeamFields.DISTANCE_M, Query.Direction.ASCENDING),
            )
        }

    /** 내 거리보다 짧은 팀 중 가장 가까운 팀. [belowM]이 null이면 조회하지 않는다. */
    private fun nearestBehind(season: Int, belowM: Long?): Flow<Loaded.Ready<RaceTeam?>?> =
        if (belowM == null) {
            flowOf(null)
        } else {
            nearest(
                teamsOf(season)
                    .whereLessThan(TeamFields.DISTANCE_M, belowM)
                    .orderBy(TeamFields.DISTANCE_M, Query.Direction.DESCENDING),
            )
        }

    /** 리스너가 첫 값 전에 실패하면 null(대체 팀 없음)을 낸다. combine이 멈추지 않고, 같은 스테이지 순위는 그대로 보인다. */
    private fun nearest(query: Query): Flow<Loaded.Ready<RaceTeam?>?> =
        query.limit(1)
            .snapshotFlow(TAG)
            .map { loaded ->
                when (loaded) {
                    is Loaded.Ready -> Loaded.Ready(loaded.value.toRaceTeams().firstOrNull(), loaded.isFromCache)
                    is Loaded.Failed -> null
                }
            }
            .distinctUntilChanged()

    private fun teamsOf(season: Int): Query =
        firestore.collection(Collections.TEAMS).whereEqualTo(TeamFields.SEASON, season)

    private fun QuerySnapshot.toRaceTeams(): List<RaceTeam> = documents.mapNotNull { it.toTeamDto()?.toRaceTeam() }

    private fun List<RaceTeam>.toStageState(myTeam: TeamStanding): StageState {
        val ranking = rankInStage(map { it.standing }, myTeam.teamId)
        val myDistanceM = firstOrNull { it.standing.teamId == myTeam.teamId }?.standing?.distanceM
        return StageState(
            teams = this,
            ranking = ranking,
            fallback = if (myDistanceM == null) FallbackQuery.NONE else fallbackQuery(ranking, myDistanceM),
        )
    }

    private data class StageState(
        val teams: List<RaceTeam>,
        val ranking: StageRanking?,
        val fallback: FallbackQuery,
    )

    private data class OtherStageTeams(
        val query: FallbackQuery,
        val ahead: Loaded.Ready<RaceTeam?>?,
        val behind: Loaded.Ready<RaceTeam?>?,
    )

    private companion object {
        const val TAG = "TDR/race"
    }
}
