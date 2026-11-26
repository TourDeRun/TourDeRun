package com.tourderun.core.navigation

import com.tourderun.core.data.session.SessionState
import com.tourderun.core.data.session.SessionUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntryStageTest {
    @Test fun `로그인 안 했으면 로그인부터`() {
        assertEquals(EntryStage.SIGNED_OUT, entryStage(signedIn = false, hasNickname = false, hasTeam = false))
    }

    @Test fun `로그아웃 상태면 닉네임과 팀이 남아 있어도 로그인부터`() {
        assertEquals(EntryStage.SIGNED_OUT, entryStage(signedIn = false, hasNickname = true, hasTeam = true))
    }

    @Test fun `로그인했지만 닉네임이 없으면 닉네임 설정`() {
        assertEquals(EntryStage.NEEDS_NICKNAME, entryStage(signedIn = true, hasNickname = false, hasTeam = false))
    }

    @Test fun `닉네임이 있고 팀이 없으면 팀 시작`() {
        assertEquals(EntryStage.NEEDS_TEAM, entryStage(signedIn = true, hasNickname = true, hasTeam = false))
    }

    @Test fun `팀까지 있으면 홈`() {
        assertEquals(EntryStage.READY, entryStage(signedIn = true, hasNickname = true, hasTeam = true))
    }

    @Test fun `시작 화면은 단계마다 정해져 있다`() {
        assertEquals(LoginRoute, EntryStage.SIGNED_OUT.startRoute())
        assertEquals(TermsRoute, EntryStage.NEEDS_NICKNAME.startRoute())
        assertEquals(TeamStartRoute, EntryStage.NEEDS_TEAM.startRoute())
        assertEquals(HomeRoute, EntryStage.READY.startRoute())
    }

    // ---- 세션 상태 → 진입 단계 (toEntryStage) ----

    private val user = SessionUser(uid = "u1", nickname = "민찬")

    @Test fun `세션을 불러오는 중이면 아직 모름`() {
        assertNull(SessionState.Loading.toEntryStage())
    }

    @Test fun `로그아웃 상태면 로그인부터`() {
        assertEquals(EntryStage.SIGNED_OUT, SessionState.SignedOut.toEntryStage())
    }

    @Test fun `users 문서가 없으면 닉네임 단계`() {
        assertEquals(EntryStage.NEEDS_NICKNAME, SessionState.NeedsNickname("u1").toEntryStage())
    }

    @Test fun `users에 팀이 없으면 팀 시작`() {
        assertEquals(EntryStage.NEEDS_TEAM, SessionState.NoTeam(user).toEntryStage())
    }

    @Test fun `users에 팀이 있으면 홈`() {
        assertEquals(EntryStage.READY, SessionState.Ready(user, teamId = "t1").toEntryStage())
    }
}
