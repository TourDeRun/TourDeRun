package com.tourderun.core.data.record

import com.tourderun.core.data.session.SessionState
import com.tourderun.core.data.session.SessionUser
import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.model.TeamStanding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 저장에 넘길 팀: 세션에 팀이 없거나 세션의 팀과 다르면 null. 세션에 팀이 있을 때의 null은 저장하지 않는다(RunSaveCoordinator). */
class SaveTeamOfTest {

    private val user = SessionUser(uid = "u1", nickname = "러너")
    private val myTeam = MyTeam(
        season = 1,
        standing = TeamStanding(teamId = "fake-my-team", name = "뛰뛰크루", distanceM = 66_800, reachedAtMs = 0L),
    )

    @Test fun `팀 없음(NoTeam) → null`() {
        assertNull(saveTeamOf(SessionState.NoTeam(user), myTeam))
    }

    @Test fun `팀 있음(Ready)이고 같은 팀 → MyTeamSource 값 그대로`() {
        assertEquals(myTeam, saveTeamOf(SessionState.Ready(user, teamId = "fake-my-team"), myTeam))
    }

    @Test fun `세션의 팀과 MyTeamSource 팀이 다름 → null(없는 팀 문서로 저장하지 않음)`() {
        assertNull(saveTeamOf(SessionState.Ready(user, teamId = "t1"), myTeam))
    }

    @Test fun `팀 있음인데 MyTeamSource 값이 없음 → null`() {
        assertNull(saveTeamOf(SessionState.Ready(user, teamId = "t1"), null))
    }

    @Test fun `로그인 전·닉네임 전 → null`() {
        assertNull(saveTeamOf(SessionState.SignedOut, myTeam))
        assertNull(saveTeamOf(SessionState.NeedsNickname("u1"), myTeam))
    }
}
