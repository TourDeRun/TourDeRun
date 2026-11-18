package com.tourderun.feature.record.data

import com.tourderun.core.data.session.SessionState
import com.tourderun.core.data.session.SessionUser
import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.model.TeamStanding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 저장에 넘길 팀: 세션에 팀이 없으면 null(문서 2개만 쓰는 경로). */
class SaveTeamOfTest {

    private val user = SessionUser(uid = "u1", nickname = "러너")
    private val myTeam = MyTeam(
        season = 1,
        standing = TeamStanding(teamId = "fake-my-team", name = "뛰뛰크루", distanceM = 66_800, reachedAtMs = 0L),
    )

    @Test fun `팀 없음(NoTeam) → null`() {
        assertNull(saveTeamOf(SessionState.NoTeam(user), myTeam))
    }

    @Test fun `팀 있음(Ready) → MyTeamSource 값 그대로`() {
        assertEquals(myTeam, saveTeamOf(SessionState.Ready(user, teamId = "t1"), myTeam))
    }

    @Test fun `로그인 전·닉네임 전 → null`() {
        assertNull(saveTeamOf(SessionState.SignedOut, myTeam))
        assertNull(saveTeamOf(SessionState.NeedsNickname("u1"), myTeam))
    }
}
