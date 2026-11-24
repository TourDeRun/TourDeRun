package com.tourderun.feature.collection.ui

import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.card.OwnedCard
import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.model.TeamStanding
import com.tourderun.domain.route.routeFor
import java.time.ZonedDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** S17·S18: 가진 카드 + 팀 거리 → 도감·카드 상세 상태 (EXT-01). */
class CollectionMapperTest {

    private val route = routeFor(1)
    private val seoul = ZoneId.of("Asia/Seoul")

    /** 한국 시간 [month]월 [day]일 0시 30분(UTC로는 전날). */
    private fun at(month: Int, day: Int) = ZonedDateTime.of(2026, month, day, 0, 30, 0, 0, seoul).toInstant().toEpochMilli()

    private val owned = listOf(
        OwnedCard("seoul", at(10, 28), "뛰뛰크루"),
        OwnedCard("anyang", at(10, 31), "뛰뛰크루"),
    )

    private fun statuses(state: CollectionUiState) = state.cards.associate { it.cityId to it.status }

    @Test fun `가진 카드는 받은 날짜를 한국 시간으로 보인다`() {
        val state = collectionUiState(route, owned, teamDistanceM = 66_800)

        assertEquals(RegionCardStatus.Collected("10월 28일"), statuses(state)["seoul"])
        assertEquals(RegionCardStatus.Collected("10월 31일"), statuses(state)["anyang"])
        assertEquals(2, state.collected)
        assertEquals(8, state.cards.size)
    }

    @Test fun `팀이 다음에 도착할 도시는 남은 거리와 함께 다음 카드`() {
        val state = collectionUiState(route, owned, teamDistanceM = 66_800)

        assertEquals(RegionCardStatus.Next(85_000 - 66_800), statuses(state)["cheonan"])
        assertEquals(RegionCardStatus.Locked(4, 125), statuses(state)["sejong"])
    }

    @Test fun `가입 전에 팀이 지난 도시는 잠김`() {
        val state = collectionUiState(route, owned, teamDistanceM = 66_800)

        assertEquals(RegionCardStatus.Locked(2, 48), statuses(state)["osan"])
    }

    @Test fun `19000m는 안양 도착이라 다음 카드는 오산`() {
        val state = collectionUiState(route, owned, teamDistanceM = 19_000)

        assertEquals(RegionCardStatus.Next(48_000 - 19_000), statuses(state)["osan"])
    }

    @Test fun `18999m면 다음 카드는 안양(아직 없으면)`() {
        val state = collectionUiState(route, owned.take(1), teamDistanceM = 18_999)

        assertEquals(RegionCardStatus.Next(1), statuses(state)["anyang"])
    }

    @Test fun `완주했으면 다음 카드가 없다`() {
        val state = collectionUiState(route, owned, teamDistanceM = 350_000)

        assertTrue(state.cards.none { it.status is RegionCardStatus.Next })
        assertEquals(RegionCardStatus.Locked(7, 350), statuses(state)["busan"])
    }

    @Test fun `팀이 없으면 다음 카드가 없다`() {
        val state = collectionUiState(route, owned, teamDistanceM = null)

        assertTrue(state.cards.none { it.status is RegionCardStatus.Next })
    }

    @Test fun `다음 도시 카드를 이미 가졌으면 받은 카드로 보인다`() {
        val state = collectionUiState(route, owned + OwnedCard("cheonan", at(11, 2), "예전팀"), teamDistanceM = 66_800)

        assertEquals(RegionCardStatus.Collected("11월 2일"), statuses(state)["cheonan"])
    }

    @Test fun `받은 시각을 모르면 날짜 없이 받은 카드`() {
        val state = collectionUiState(route, listOf(OwnedCard("seoul", 0, "")), teamDistanceM = 0)

        assertEquals(RegionCardStatus.Collected(null), statuses(state)["seoul"])
    }

    @Test fun `서울 카드가 없으면 출발 도시 잠김`() {
        val state = collectionUiState(route, emptyList(), teamDistanceM = 0)

        assertEquals(RegionCardStatus.Locked(0, 0), statuses(state)["seoul"])
        assertEquals(RegionCardStatus.Next(19_000), statuses(state)["anyang"])
    }

    @Test fun `카드 상세 - 가진 카드는 받은 날짜와 팀 이름`() {
        val detail = requireNotNull(cardDetailUiState(route, "anyang", owned))

        assertEquals("안양", detail.name)
        assertEquals(1, detail.stageNumber)
        assertEquals(19, detail.distanceFromStartKm)
        assertEquals("10월 31일", detail.acquiredDateText)
        assertEquals("뛰뛰크루", detail.teamName)
    }

    @Test fun `카드 상세 - 출발 도시는 스테이지 없이 0km`() {
        val detail = requireNotNull(cardDetailUiState(route, "seoul", owned))

        assertNull(detail.stageNumber)
        assertEquals(0, detail.distanceFromStartKm)
    }

    @Test fun `카드 상세 - 아직 없는 카드는 받은 줄을 숨긴다`() {
        val detail = requireNotNull(cardDetailUiState(route, "daegu", owned))

        assertEquals(6, detail.stageNumber)
        assertEquals(262, detail.distanceFromStartKm)
        assertNull(detail.acquiredDateText)
    }

    @Test fun `카드 상세 - 경로에 없는 도시는 null`() {
        assertNull(cardDetailUiState(route, "jeju", owned))
    }

    @Test fun `카드를 못 읽으면 도감·상세 모두 실패`() {
        val failed = Loaded.Failed(DataError.PermissionDenied)

        assertEquals(CollectionLoad.Failed(DataError.PermissionDenied), collectionLoad(failed, team = null))
        assertEquals(CardDetailLoad.Failed(DataError.PermissionDenied), cardDetailLoad("seoul", failed, team = null))
    }

    @Test fun `내 팀 거리로 다음 카드를 계산한다`() {
        val team = MyTeam(season = 1, standing = TeamStanding("t", "뛰뛰크루", 66_800, reachedAtMs = 0L))

        val load = collectionLoad(Loaded.Ready(owned, isFromCache = true), team) as CollectionLoad.Content

        assertEquals(RegionCardStatus.Next(18_200), load.ui.cards.first { it.cityId == "cheonan" }.status)
    }

    @Test fun `경로에 없는 도시 상세는 닫힌다`() {
        assertEquals(CardDetailLoad.NotFound, cardDetailLoad("jeju", Loaded.Ready(owned, isFromCache = false), team = null))
    }
}
