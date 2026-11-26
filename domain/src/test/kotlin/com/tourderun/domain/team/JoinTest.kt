package com.tourderun.domain.team

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JoinTest {

    @Test fun `팀이 없고 1명인 팀 → 참여 가능`() {
        assertNull(joinRejection(myTeamId = null, memberCount = 1))
    }

    @Test fun `팀이 없고 3명인 팀 → 4번째로 참여 가능`() {
        assertNull(joinRejection(myTeamId = null, memberCount = 3))
    }

    @Test fun `4명인 팀 → 가득 참`() {
        assertEquals(JoinRejection.FULL, joinRejection(myTeamId = null, memberCount = 4))
    }

    @Test fun `이미 팀이 있음 → 거절`() {
        assertEquals(JoinRejection.ALREADY_IN_TEAM, joinRejection(myTeamId = "t2", memberCount = 1))
    }

    @Test fun `이미 팀이 있고 상대 팀도 가득 참 → 팀 있음을 먼저 알림`() {
        assertEquals(JoinRejection.ALREADY_IN_TEAM, joinRejection(myTeamId = "t2", memberCount = 4))
    }

    @Test fun `최대 인원은 4명`() {
        assertEquals(4, TEAM_MAX_MEMBERS)
    }
}
