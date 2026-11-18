package com.tourderun.core.data.journey

import com.tourderun.core.data.firestore.FieldOp
import com.tourderun.core.data.firestore.MemberFields
import com.tourderun.core.data.firestore.UserFields
import com.tourderun.core.data.firestore.WriteMode
import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.journey.pendingArrivals
import com.tourderun.domain.model.TeamStanding
import com.tourderun.domain.route.routeFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArrivalSeenPlanTest {
    /** 본 것 1, 85,000m → 오산·천안, current 3 */
    private val pending = requireNotNull(pendingArrivals(1, 85_000, routeFor(1)))
    private val seen = PendingSeen(uid = "uid-1", teamId = "team-1", teamName = "뛰뛰크루", pending = pending)

    @Test fun `카드가 없으면 members 문서 하나, 필드는 seenArrivals 하나`() {
        val plan = arrivalSeenPlan(seen, emptyList())
        assertEquals(1, plan.size)
        val member = plan.single()
        assertEquals("teams/team-1/members/uid-1", member.path)
        assertEquals(WriteMode.UPDATE, member.mode)
        assertEquals(mapOf(MemberFields.SEEN_ARRIVALS to 3), member.fields)
    }

    @Test fun `카드 2장이면 문서 2개, users에는 cards 두 항목만`() {
        val plan = arrivalSeenPlan(seen, listOf("osan", "cheonan"))
        assertEquals(listOf("teams/team-1/members/uid-1", "users/uid-1"), plan.map { it.path })
        val users = plan[1]
        assertEquals(WriteMode.SET_MERGE, users.mode)
        val card = mapOf(UserFields.CARD_AT to FieldOp.ServerTime, UserFields.CARD_TEAM_NAME to "뛰뛰크루")
        assertEquals(mapOf(UserFields.CARDS to mapOf("osan" to card, "cheonan" to card)), users.fields)
    }

    @Test fun `카드 1장이면 users의 cards에 그 도시 항목 하나만`() {
        val plan = arrivalSeenPlan(seen, listOf("cheonan"))
        assertEquals(2, plan.size)
        val users = plan[1]
        assertEquals("users/uid-1", users.path)
        assertEquals(setOf(UserFields.CARDS), users.fields.keys)
        val card = mapOf(UserFields.CARD_AT to FieldOp.ServerTime, UserFields.CARD_TEAM_NAME to "뛰뛰크루")
        assertEquals(mapOf("cheonan" to card), users.fields[UserFields.CARDS])
    }

    @Test fun `같은 카드 ID가 두 번 와도 한 번만`() {
        val plan = arrivalSeenPlan(seen, listOf("osan", "osan"))
        @Suppress("UNCHECKED_CAST")
        val cards = plan[1].fields[UserFields.CARDS] as Map<String, Any?>
        assertEquals(setOf("osan"), cards.keys)
    }

    @Test fun `PendingSeen의 팀과 지금 팀이 달라도 markSeen은 받은 값의 팀에 쓴다`() {
        // 연출을 띄운 뒤 team-2로 옮겨 가서 지금 팀으로 계산하면 다른 자리(또는 null)가 된다.
        val now = pendingFor("uid-1", team("team-2", 19_000), SeenOf(MemberKey("uid-1", "team-2"), 1))
        assertNull(now)
        // markSeen은 지금 팀을 다시 읽지 않고 받은 PendingSeen(team-1)만으로 쓴다.
        val plan = arrivalSeenPlan(seen, emptyList())
        assertEquals(listOf("teams/team-1/members/uid-1"), plan.map { it.path })
        assertEquals(mapOf(MemberFields.SEEN_ARRIVALS to 3), plan.single().fields)
    }

    @Test fun `Fake는 markSeen 뒤에 null`() = runBlocking {
        val repo = FakeArrivalRepository(ArrivalScenario.TWO_CITIES)
        val before = requireNotNull(repo.observePending("uid-1").first()) { "연출할 도착이 있어야 함" }
        assertEquals("uid-1", before.uid)
        assertEquals(listOf("osan", "cheonan"), before.pending.newCities.map { it.id })
        assertEquals(Result.success(Unit), repo.markSeen(before))
        assertNull(repo.observePending("uid-1").first())
    }

    @Test fun `Fake도 uid null → null`() = runBlocking {
        assertNull(FakeArrivalRepository(ArrivalScenario.TWO_CITIES).observePending(null).first())
    }

    @Test fun `Fake 기본 시나리오는 연출 없음`() = runBlocking {
        assertNull(FakeArrivalRepository().observePending("uid-1").first())
    }

    private fun team(teamId: String, distanceM: Long) =
        MyTeam(season = 1, standing = TeamStanding(teamId = teamId, name = "다른팀", distanceM = distanceM, reachedAtMs = 0L))
}
