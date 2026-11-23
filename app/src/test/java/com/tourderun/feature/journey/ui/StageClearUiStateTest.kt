package com.tourderun.feature.journey.ui

import com.tourderun.domain.journey.pendingArrivals
import com.tourderun.domain.route.routeFor
import org.junit.Assert.assertEquals
import org.junit.Test

class StageClearUiStateTest {
    private val route = routeFor(1)

    private fun stateOf(seenArrivals: Int, teamDistanceM: Long): StageClearUiState =
        stageClearUiState(requireNotNull(pendingArrivals(seenArrivals, teamDistanceM, route)), route)

    @Test fun `안양 도착 → 스테이지 1 클리어, 다음은 스테이지 2 오산 29km`() {
        assertEquals(
            StageClearUiState(
                clearedStage = 1,
                arrivedCity = "안양",
                nextStage = 2,
                nextCity = "오산",
                nextStageKm = 29,
                card = null,
            ),
            stateOf(seenArrivals = 0, teamDistanceM = 19_000),
        )
    }

    @Test fun `오산·천안 두 도시 도착 → 마지막 도시 천안, 스테이지 3 클리어, 다음은 스테이지 4 세종 40km`() {
        assertEquals(
            StageClearUiState(
                clearedStage = 3,
                arrivedCity = "천안",
                nextStage = 4,
                nextCity = "세종",
                nextStageKm = 40,
                card = null,
            ),
            stateOf(seenArrivals = 1, teamDistanceM = 85_000),
        )
    }

    @Test fun `부산 도착 → 스테이지 7 클리어, 다음 스테이지·도시·거리 모두 null`() {
        assertEquals(
            StageClearUiState(
                clearedStage = 7,
                arrivedCity = "부산",
                nextStage = null,
                nextCity = null,
                nextStageKm = null,
                card = null,
            ),
            stateOf(seenArrivals = 6, teamDistanceM = 350_000),
        )
    }

    @Test fun `부산 카드 → 순번 8 of 8, 영문 이름 Busan`() {
        val pending = requireNotNull(pendingArrivals(6, 350_000, route))
        assertEquals(EarnedCardUi("부산", "Busan", index = 8, total = 8), stageClearUiState(pending, route, listOf("busan")).card)
    }

    @Test fun `새 카드 빈 목록·null → 카드 없음`() {
        val pending = requireNotNull(pendingArrivals(0, 19_000, route))
        assertEquals(null, stageClearUiState(pending, route, emptyList()).card)
        assertEquals(null, stageClearUiState(pending, route, null).card)
    }

    @Test fun `서울 카드 → 순번 1, 경로에 없는 도시 → null`() {
        assertEquals(EarnedCardUi("서울", "Seoul", index = 1, total = 8), earnedCardUi("seoul", route))
        assertEquals(null, earnedCardUi("jeju", route))
    }
}
