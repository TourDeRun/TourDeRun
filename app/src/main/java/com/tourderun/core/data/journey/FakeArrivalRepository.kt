package com.tourderun.core.data.journey

import com.tourderun.domain.journey.PendingArrivals
import com.tourderun.domain.journey.pendingArrivals
import com.tourderun.domain.route.routeFor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 화면 연결용 가짜 구현. 시나리오의 (본 도착 수, 팀 거리)로 `pendingArrivals`를 그대로 계산해 낸다.
 * [markSeen]을 부르면 그 뒤로는 null을 낸다(앱을 다시 켜면 처음부터).
 *
 * Hilt는 [ArrivalScenario.NONE](연출 없음)으로 만든다. 연출 화면은 Preview·디버그에서 직접 만든다:
 * `FakeArrivalRepository(ArrivalScenario.TWO_CITIES)`.
 */
@Singleton
class FakeArrivalRepository(scenario: ArrivalScenario) : ArrivalRepository {

    @Inject constructor() : this(ArrivalScenario.NONE)

    private val state = MutableStateFlow(pendingArrivals(scenario.seenArrivals, scenario.teamDistanceM, routeFor(SEASON)))

    override fun observePending(): Flow<PendingArrivals?> = state.asStateFlow()

    override fun markSeen(pending: PendingArrivals, newCardCityIds: List<String>): Result<Unit> {
        state.update { current -> if (current == pending) null else current }
        return Result.success(Unit)
    }

    private companion object {
        const val SEASON = 1
    }
}

/** 가짜 도착 상황(시즌 1). */
enum class ArrivalScenario(val seenArrivals: Int, val teamDistanceM: Long) {
    /** 새 도착 없음(66.8km, 안양·오산 모두 봄) */
    NONE(seenArrivals = 2, teamDistanceM = 66_800),

    /** 안양 도착 */
    ONE_CITY(seenArrivals = 0, teamDistanceM = 19_000),

    /** 한 번에 오산·천안 도착(연출은 천안) */
    TWO_CITIES(seenArrivals = 1, teamDistanceM = 85_000),

    /** 부산 도착 = 완주 */
    FINISH(seenArrivals = 6, teamDistanceM = 350_000),
}
