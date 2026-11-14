package com.tourderun.core.navigation

import org.junit.Assert.assertEquals
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
        assertEquals(NicknameRoute, EntryStage.NEEDS_NICKNAME.startRoute())
        assertEquals(TeamStartRoute, EntryStage.NEEDS_TEAM.startRoute())
        assertEquals(HomeRoute, EntryStage.READY.startRoute())
    }
}
