package com.tourderun.core.navigation

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.tourderun.core.designsystem.component.TeamAppearance
import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.designsystem.component.TeamIcon

/** 목 팀. 이름과 대표 이미지만 있다. */
data class MockTeam(val name: String, val appearance: TeamAppearance)

/**
 * **목(가짜) 로그인·팀 상태.** 최초 진입 흐름(#52)의 화면 이동만 확인하려고 둔다. 서버에 아무것도 보내지 않고
 * 값은 이 기기의 SharedPreferences에만 남는다(앱을 다시 켜도 유지, 앱 데이터를 지우면 처음부터).
 *
 * TODO(AUTH-01): `SessionRepository`·`TeamRepository`(권민찬)가 생기면 이 파일을 지우고
 *  [EntryStage]를 그 값으로 계산한다. 이 클래스의 함수 이름이 실제 Repository에 필요한 동작 목록이다.
 */
@Stable
class MockSession(context: Context) {
    private val prefs = context.getSharedPreferences("mock_session", Context.MODE_PRIVATE)

    var signedIn by mutableStateOf(prefs.getBoolean(KEY_SIGNED_IN, false))
        private set
    var nickname by mutableStateOf(prefs.getString(KEY_NICKNAME, null))
        private set
    var team by mutableStateOf(readTeam())
        private set

    val stage: EntryStage get() = entryStage(signedIn, nickname != null, team != null)

    /** AUTH-01 구글 로그인 자리. 항상 성공한다. */
    fun signIn() {
        signedIn = true
        prefs.edit { putBoolean(KEY_SIGNED_IN, true) }
    }

    /** AUTH-02 닉네임 설정(`users` 문서 생성) 자리. */
    fun saveNickname(value: String) {
        nickname = value
        prefs.edit { putString(KEY_NICKNAME, value) }
    }

    /** TEAM-01 팀 만들기, TEAM-03 초대 코드로 참여 자리. */
    fun joinTeam(value: MockTeam) {
        team = value
        prefs.edit {
            putString(KEY_TEAM_NAME, value.name)
            putInt(KEY_TEAM_COLOR, value.appearance.color.ordinal)
            putInt(KEY_TEAM_ICON, value.appearance.icon.ordinal)
        }
    }

    /** TEAM-07 팀 탈퇴 자리. */
    fun leaveTeam() {
        team = null
        prefs.edit { remove(KEY_TEAM_NAME) }
    }

    /** AUTH-04 로그아웃 자리. 닉네임과 팀은 남아서 다시 로그인하면 바로 홈으로 간다. */
    fun signOut() {
        signedIn = false
        prefs.edit { putBoolean(KEY_SIGNED_IN, false) }
    }

    /** AUTH-05 회원 탈퇴 자리. 전부 지운다. */
    fun withdraw() {
        signedIn = false
        nickname = null
        team = null
        prefs.edit { clear() }
    }

    private fun readTeam(): MockTeam? {
        val name = prefs.getString(KEY_TEAM_NAME, null) ?: return null
        return MockTeam(
            name,
            TeamAppearance(
                TeamColor.entries.getOrNull(prefs.getInt(KEY_TEAM_COLOR, 0)) ?: TeamColor.BLUE,
                TeamIcon.entries.getOrNull(prefs.getInt(KEY_TEAM_ICON, 0)) ?: TeamIcon.RUNNER,
            ),
        )
    }

    companion object {
        /** 목에서 통하는 유일한 초대 코드. 팀 화면(S14) 임시 상태에 보이는 코드와 같다. */
        const val INVITE_CODE = "4K7Q2M"

        private const val KEY_SIGNED_IN = "signedIn"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_TEAM_NAME = "teamName"
        private const val KEY_TEAM_COLOR = "teamColor"
        private const val KEY_TEAM_ICON = "teamIcon"
    }
}
