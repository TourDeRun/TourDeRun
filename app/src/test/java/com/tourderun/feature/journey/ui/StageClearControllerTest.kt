package com.tourderun.feature.journey.ui

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
    private val controller = StageClearController(session, Lazy { arrivals })
    private val visible = controller.visible.stateIn(scope, SharingStarted.Eagerly, null)

    @After fun tearDown() = scope.cancel()

    @Test fun `uid가 null이 되면 → 다시 구독하지 않고 null`() {
        session.state.value = SessionState.SignedOut
        assertNull(visible.value)
        assertEquals(listOf("u1"), arrivals.observedUids)
    }

    @Test fun `못 본 도착 있음 → 그 도착이 보인다`() {
        assertEquals(anyang("u1"), visible.value)
    }

    @Test fun `confirm → markSeen 1번, 저장소가 같은 값을 내도 null`() {
        controller.confirm(visible.value)
        assertEquals(listOf(anyang("u1")), arrivals.marked)
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
        assertEquals("오산", visible.value?.pending?.lastCity?.nameKo)
    }

    @Test fun `uid 바뀜 → 새 uid로 다시 구독`() {
        session.state.value = SessionState.Ready(SessionUser("u2", "러너2"), "team-1")
        assertEquals(listOf("u1", "u2"), arrivals.observedUids)
        assertEquals(anyang("u2"), visible.value)
    }
}

private fun anyang(uid: String) =
    PendingSeen(uid, "team-1", "뛰뛰크루", requireNotNull(pendingArrivals(0, 19_000, routeFor(1))), season = 1)

private class TestSession(initial: SessionState) : SessionRepository {
    override val state = MutableStateFlow(initial)
    override suspend fun signInWithGoogle(idToken: String) = Result.success(Unit)
    override suspend fun createUser(nickname: String) = Result.success(Unit)
    override fun signOut() = Unit
}

/** 본 수 0, 팀 거리 [distanceM]으로 계산한다. markSeen해도 값을 바꾸지 않는다(연출이 닫히는 건 컨트롤러 몫인지 보려고). */
private class RecordingArrivals : ArrivalRepository {
    val distanceM = MutableStateFlow(19_000L)
    val observedUids = mutableListOf<String>()
    val marked = mutableListOf<PendingSeen>()
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
        marked += seen
        return if (failMark) Result.failure(IllegalStateException("배치 시작 실패")) else Result.success(Unit)
    }
}
