package com.tourderun.feature.journey.ui

import com.tourderun.core.data.session.MyStats
import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.card.CardRepository
import com.tourderun.core.data.card.FakeCardRepository
import com.tourderun.core.data.card.OwnedCard
import com.tourderun.core.data.journey.ArrivalRepository
import com.tourderun.core.data.journey.PendingSeen
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.SessionState
import com.tourderun.core.data.session.SessionUser
import com.tourderun.domain.journey.pendingArrivals
import com.tourderun.domain.route.routeFor
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StageClearControllerTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined + Job())
    private val session = TestSession(SessionState.Ready(SessionUser("u1", "러너"), "team-1"))
    private val arrivals = RecordingArrivals()
    private val cards = FakeCardRepository()
    private val controller = StageClearController(session, Lazy { arrivals }, Lazy { cards })
    private val visible = controller.visible.stateIn(scope, SharingStarted.Eagerly, null)

    /** 다른 카드 저장소로 만든 컨트롤러와 그 연출 값. */
    private fun withCards(repository: CardRepository, distanceM: Long = 19_000): Pair<StageClearController, () -> StageClearShown?> {
        val recording = RecordingArrivals().also { it.distanceM.value = distanceM }
        val c = StageClearController(session, Lazy { recording }, Lazy { repository })
        val v = c.visible.stateIn(scope, SharingStarted.Eagerly, null)
        lastArrivals = recording
        return c to { v.value }
    }
    private var lastArrivals = arrivals

    @After fun tearDown() = scope.cancel()

    @Test fun `uid가 null이 되면 → 다시 구독하지 않고 null`() {
        session.state.value = SessionState.SignedOut
        assertNull(visible.value)
        assertEquals(listOf("u1"), arrivals.observedUids)
    }

    @Test fun `못 본 도착 있음 → 그 도착이 보인다`() {
        assertEquals(anyang("u1"), visible.value?.seen)
    }

    @Test fun `confirm → markSeen 1번, 저장소가 같은 값을 내도 null`() {
        controller.confirm(visible.value)
        assertEquals(listOf(anyang("u1")), arrivals.marked.map { it.first })
        assertNull(visible.value)
    }

    @Test fun `confirm 두 번 → markSeen 1번`() {
        val seen = visible.value
        controller.confirm(seen)
        controller.confirm(seen)
        controller.confirm(visible.value)
        assertEquals(1, arrivals.marked.size)
    }

    @Test fun `markSeen 실패 → 연출은 닫힌다`() {
        arrivals.failMark = true
        val result = controller.confirm(visible.value)
        assertEquals(true, result?.isFailure)
        assertNull(visible.value)
    }

    @Test fun `확인 뒤 새 도착이 오면 다시 보인다`() {
        controller.confirm(visible.value)
        arrivals.distanceM.value = 48_000
        assertEquals("오산", visible.value?.seen?.pending?.lastCity?.nameKo)
    }

    @Test fun `uid 바뀜 → 새 uid로 다시 구독`() {
        session.state.value = SessionState.Ready(SessionUser("u2", "러너2"), "team-1")
        assertEquals(listOf("u1", "u2"), arrivals.observedUids)
        assertEquals(anyang("u2"), visible.value?.seen)
    }

    // ---- EXT-01 지역 카드 ----

    @Test fun `새 도시 1곳 + 가진 카드 없음 → markSeen에 그 도시 ID, 연출에 카드`() {
        val shown = visible.value
        assertEquals(listOf("anyang"), shown?.newCardCityIds)
        controller.confirm(shown)
        assertEquals(listOf("anyang"), arrivals.marked.single().second)
        val ui = stageClearUiState(requireNotNull(shown).seen.pending, routeFor(1), shown.newCardCityIds)
        assertEquals(EarnedCardUi("안양", "Anyang", index = 2, total = 8), ui.card)
    }

    @Test fun `이미 가진 도시 → markSeen에 빈 목록, 카드 null`() {
        val (c, shown) = withCards(FakeCardRepository(listOf(OwnedCard("anyang", 1L, "옛 팀"))))
        val s = requireNotNull(shown())
        assertEquals(emptyList<String>(), s.newCardCityIds)
        assertNull(stageClearUiState(s.seen.pending, routeFor(1), s.newCardCityIds).card)
        c.confirm(s)
        assertEquals(emptyList<String>(), lastArrivals.marked.single().second)
    }

    @Test fun `두 도시 한 번에 → markSeen에 두 ID, 카드는 마지막 도시`() {
        // 본 수 1, 85,000m → 오산·천안
        val (c, shown) = withCards(FakeCardRepository(), distanceM = 85_000)
        val s = requireNotNull(shown())
        assertEquals(listOf("osan", "cheonan"), s.newCardCityIds)
        assertEquals(EarnedCardUi("천안", "Cheonan", index = 4, total = 8, count = 2), stageClearUiState(s.seen.pending, routeFor(1), s.newCardCityIds).card)
        c.confirm(s)
        assertEquals(listOf("osan", "cheonan"), lastArrivals.marked.single().second)
    }

    @Test fun `두 도시 중 마지막 도시만 가짐 → 앞 도시 카드만, 연출 카드도 그 도시`() {
        val (_, shown) = withCards(FakeCardRepository(listOf(OwnedCard("cheonan", 1L, "옛 팀"))), distanceM = 85_000)
        val s = requireNotNull(shown())
        assertEquals(listOf("osan"), s.newCardCityIds)
        assertEquals("오산", stageClearUiState(s.seen.pending, routeFor(1), s.newCardCityIds).card?.cityName)
    }

    @Test fun `가진 카드를 아직 못 읽음 → 연출을 띄우지 않는다`() {
        val (_, shown) = withCards(LoadingCards)
        assertNull(shown())
    }

    @Test fun `가진 카드 읽기 실패 → 카드 없이 연출, 확인은 닫기만 하고 쓰지 않는다`() {
        val (c, shown) = withCards(FakeCardRepository(failure = DataError.Unavailable))
        val s = requireNotNull(shown())
        assertNull(s.newCardCityIds)
        assertNull(stageClearUiState(s.seen.pending, routeFor(1), s.newCardCityIds).card)
        assertNull(c.confirm(s))
        assertEquals(emptyList<Pair<PendingSeen, List<String>>>(), lastArrivals.marked)
        assertNull(shown())
    }

    @Test fun `확인 뒤 카드가 반영돼 가진 카드가 바뀌어도 같은 도착은 다시 안 보인다`() {
        controller.confirm(visible.value)
        cards.owned.value = listOf(OwnedCard("anyang", 1L, "뛰뛰크루"))
        assertNull(visible.value)
        assertEquals(1, arrivals.marked.size)
    }

    @Test fun `못 본 도착이 없으면 가진 카드를 구독하지 않는다`() {
        var subscribed = 0
        val counting = object : CardRepository {
            override fun observeOwned(uid: String?) = cards.observeOwned(uid).also { subscribed++ }
        }
        // 10,000m: 본 수 0, 도착 수 0 → 새 도착 없음
        val (_, shown) = withCards(counting, distanceM = 10_000)
        assertNull(shown())
        assertEquals(0, subscribed)
    }
}

/** 첫 값을 내지 않는 가진 카드(불러오는 중). */
private object LoadingCards : CardRepository {
    override fun observeOwned(uid: String?) = emptyFlow<Loaded<List<OwnedCard>>>()
}

private fun anyang(uid: String) =
    PendingSeen(uid, "team-1", "뛰뛰크루", requireNotNull(pendingArrivals(0, 19_000, routeFor(1))), season = 1)

private class TestSession(initial: SessionState) : SessionRepository {
    override val state = MutableStateFlow(initial)
    override suspend fun signInWithGoogle(idToken: String) = Result.success(Unit)
    override suspend fun createUser(nickname: String) = Result.success(Unit)
    override fun signOut() = Unit
    override fun updateNickname(uid: String, teamId: String?, nickname: String) = Result.success(Unit)
    override fun observeMyStats(uid: String) = emptyFlow<Loaded<MyStats>>()
}

/** 본 수 0, 팀 거리 [distanceM]으로 계산한다. markSeen해도 값을 바꾸지 않는다(연출이 닫히는 건 컨트롤러 몫인지 보려고). */
private class RecordingArrivals : ArrivalRepository {
    val distanceM = MutableStateFlow(19_000L)
    val observedUids = mutableListOf<String>()
    val marked = mutableListOf<Pair<PendingSeen, List<String>>>()
    var failMark = false

    override fun observePending(uid: String?): Flow<PendingSeen?> {
        requireNotNull(uid) { "uid가 없으면 구독하지 않아야 한다" }
        observedUids += uid
        return distanceM.map { m ->
            val seen = if (m >= 48_000) 1 else 0
            pendingArrivals(seen, m, routeFor(1))?.let { PendingSeen(uid, "team-1", "뛰뛰크루", it, season = 1) }
        }
    }

    override fun markSeen(seen: PendingSeen, newCardCityIds: List<String>): Result<Unit> {
        marked += seen to newCardCityIds
        return if (failMark) Result.failure(IllegalStateException("배치 시작 실패")) else Result.success(Unit)
    }
}
