package com.tourderun.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.feature.auth.ui.LoginScreen
import com.tourderun.feature.auth.ui.NicknameScreen
import com.tourderun.feature.auth.ui.TermsItem
import com.tourderun.feature.auth.ui.TermsScreen
import com.tourderun.feature.auth.ui.TermsUiState
import com.tourderun.feature.team.ui.CreateTeamScreen
import com.tourderun.feature.team.ui.InviteCodeScreen
import com.tourderun.feature.team.ui.JoinTeamScreen
import com.tourderun.feature.team.ui.TeamStartScreen

/** 앱을 열었을 때의 시작 화면. 닉네임이 없으면 약관(S01-1)부터 다시 한다(AUTH-02, D-08). */
internal fun EntryStage.startRoute(): Any = when (this) {
    EntryStage.SIGNED_OUT -> LoginRoute
    // 약관 동의 시각은 닉네임과 같이 저장되므로(D-08), 닉네임 전에 앱을 껐으면 약관부터 다시 한다.
    EntryStage.NEEDS_NICKNAME -> TermsRoute
    EntryStage.NEEDS_TEAM -> TeamStartRoute
    EntryStage.READY -> HomeRoute
}

/**
 * 세션 단계가 바뀌었을 때 진입 화면에서 다음 화면으로 옮긴다. 이미 맞는 화면에 있으면 그대로 둔다.
 * 로그아웃(SIGNED_OUT)은 어느 화면에서든 로그인으로, 팀이 없어지면(NEEDS_TEAM: 내보내짐 TEAM-06·나가기 TEAM-07)
 * 어느 화면에서든 팀 시작(S03)으로 보낸다.
 */
internal fun NavController.followEntryStage(stage: EntryStage) {
    val destination = currentDestination ?: return
    val onLogin = destination.hasRoute<LoginRoute>()
    val inSignUp = destination.hasRoute<TermsRoute>() || destination.hasRoute<NicknameRoute>()
    when (stage) {
        EntryStage.SIGNED_OUT -> if (!onLogin) resetTo(LoginRoute)
        EntryStage.NEEDS_NICKNAME -> if (onLogin) resetTo(TermsRoute)
        EntryStage.NEEDS_TEAM -> if (!destination.hasRoute<TeamStartRoute>()) resetTo(TeamStartRoute)
        // 팀 시작 화면(S03)에서는 직접 옮긴다: 만들기는 홈 위에 S06, 참여는 홈 (TEAM-01·03)
        EntryStage.READY -> if (onLogin || inSignUp) resetTo(HomeRoute)
    }
}

/** 세션 첫 값을 기다리는 동안 보이는 화면 (앱 시작 직후, 보통 1초 안). */
@Composable
internal fun EntryLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(TdrTheme.colors.bgSurface),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TdrTheme.colors.brand)
    }
}

/** 백스택을 전부 비우고 [route]만 남긴다. 로그인·로그아웃·팀 가입·탈퇴 뒤에 뒤로 가기로 이전 화면에 못 돌아가게 한다. */
internal fun NavController.resetTo(route: Any) {
    navigate(route) { popUpTo(graph.id) { inclusive = true } }
}

/**
 * 최초 진입 화면 S01~S06 (#52). S01·S02는 ViewModel이 세션에 쓰고, 다음 화면 이동은 [followEntryStage]가 한다.
 * 팀 만들기(S04)·초대 코드(S06)는 Firestore에 연결돼 있고, 팀 참여(S05)는 TEAM-03에서 붙인다.
 */
internal fun NavGraphBuilder.entryGraph(
    navController: NavController,
    onSignOut: () -> Unit,
) {
    composable<LoginRoute> {
        LoginScreen()
    }

    composable<TermsRoute> {
        var agreed by remember { mutableStateOf(emptySet<TermsItem>()) }
        TermsScreen(
            uiState = TermsUiState(agreed),
            onBack = onSignOut,
            onToggleAll = { agreed = if (agreed.size == TermsItem.entries.size) emptySet() else TermsItem.entries.toSet() },
            onToggle = { agreed = if (it in agreed) agreed - it else agreed + it },
            onViewTerms = {}, // TODO(AUTH-01): 약관 전문 보기
            onContinue = { navController.navigate(NicknameRoute) },
        )
    }

    composable<NicknameRoute> {
        // 앱을 다시 켜서 약관부터 시작한 경우에도 뒤로 가기는 약관으로 돌아간다.
        NicknameScreen(onBack = { navController.popBackStack() })
    }

    composable<TeamStartRoute> {
        var dialog by rememberSaveable { mutableStateOf(TeamDialog.NONE) }
        TeamStartScreen(onCreateTeam = { dialog = TeamDialog.CREATE }, onJoinTeam = { dialog = TeamDialog.JOIN })

        when (dialog) {
            TeamDialog.NONE -> Unit
            TeamDialog.CREATE -> CreateTeamScreen(
                onDismiss = { dialog = TeamDialog.NONE },
                onCreated = {
                    // S06에서 뒤로 가도 팀 시작이 아니라 홈이 나오게, 홈을 먼저 깔고 그 위에 S06을 올린다.
                    navController.resetTo(HomeRoute)
                    navController.navigate(InviteCodeRoute)
                },
            )
            TeamDialog.JOIN -> JoinTeamScreen(
                onDismiss = { dialog = TeamDialog.NONE },
                onJoined = { navController.resetTo(HomeRoute) },
            )
        }
    }

    composable<InviteCodeRoute> {
        InviteCodeScreen(
            onBack = { navController.popBackStack() },
            onDepart = { navController.popBackStack() },
        )
    }
}

private enum class TeamDialog { NONE, CREATE, JOIN }

