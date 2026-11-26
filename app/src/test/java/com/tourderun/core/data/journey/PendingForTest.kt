package com.tourderun.core.data.journey

import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.model.TeamStanding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** FirestoreArrivalRepository의 계산 부분: uid·내 팀·members 값 → 못 본 도착과 확인을 쓸 자리. */
class PendingForTest {

    private fun team(distanceM: Long, teamId: String = "team-1") =
        MyTeam(season = 1, standing = TeamStanding(teamId = teamId, name = "뛰뛰크루", distanceM = distanceM, reachedAtMs = 0L))

    private fun seen(n: Int, uid: String = "uid-1", teamId: String = "team-1") = SeenOf(MemberKey(uid, teamId), n)

    @Test fun `본 것 1, 85000m → 오산·천안, current 3`() {
        val pending = pendingFor("uid-1", team(85_000), seen(1))

        assertEquals(listOf("osan", "cheonan"), pending?.pending?.newCities?.map { it.id })
        assertEquals(3, pending?.pending?.current)
    }

    @Test fun `seenArrivals 필드 없음(0), 19000m → 안양`() {
        assertEquals(listOf("anyang"), pendingFor("uid-1", team(19_000), seen(0))?.pending?.newCities?.map { it.id })
    }

    @Test fun `새 도착 없음 → null`() {
        assertNull(pendingFor("uid-1", team(66_800), seen(2)))
    }

    @Test fun `uid null → null, 리스너 대상도 없음`() {
        assertNull(pendingFor(null, team(85_000), seen(1)))
        assertNull(memberKeyOf(null, team(85_000)))
    }

    @Test fun `팀 없음 → null, 리스너 대상도 없음`() {
        assertNull(pendingFor("uid-1", null, seen(1)))
        assertNull(memberKeyOf("uid-1", null))
    }

    @Test fun `members 오류·문서 없음(seen null) → null`() {
        assertNull(pendingFor("uid-1", team(85_000), null))
    }

    @Test fun `팀이 바뀌는 중 이전 팀 문서 값 → null`() {
        assertNull(pendingFor("uid-1", team(85_000, teamId = "team-2"), seen(1, teamId = "team-1")))
    }

    @Test fun `리스너 대상은 uid와 팀 ID`() {
        assertEquals(MemberKey("uid-1", "team-1"), memberKeyOf("uid-1", team(85_000)))
    }

    @Test fun `확인을 쓸 자리에 uid·팀 ID·팀 이름`() {
        val seen = requireNotNull(pendingFor("uid-1", team(85_000), seen(1)))
        assertEquals(Triple("uid-1", "team-1", "뛰뛰크루"), Triple(seen.uid, seen.teamId, seen.teamName))
        assertEquals(1, seen.pending.seen)
    }

    @Test fun `다른 uid의 문서 값 → null`() {
        assertNull(pendingFor("uid-2", team(85_000), seen(1, uid = "uid-1")))
    }
}
