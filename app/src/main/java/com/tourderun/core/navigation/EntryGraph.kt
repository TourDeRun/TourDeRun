package com.tourderun.core.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tourderun.R
import com.tourderun.core.designsystem.component.TeamAppearance
import com.tourderun.core.ui.preview.PreviewSamples
import com.tourderun.domain.route.routeFor
import com.tourderun.feature.auth.ui.LoginScreen
import com.tourderun.feature.auth.ui.LoginUiState
import com.tourderun.feature.auth.ui.NicknameScreen
import com.tourderun.feature.auth.ui.NicknameUiState
import com.tourderun.feature.auth.ui.TermsItem
import com.tourderun.feature.auth.ui.TermsScreen
import com.tourderun.feature.auth.ui.TermsUiState
import com.tourderun.feature.team.ui.CreateTeamDialog
import com.tourderun.feature.team.ui.CreateTeamUiState
import com.tourderun.feature.team.ui.InviteCodeScreen
import com.tourderun.feature.team.ui.InviteCodeUiState
import com.tourderun.feature.team.ui.JoinTeamDialog
import com.tourderun.feature.team.ui.JoinTeamPreviewUi
import com.tourderun.feature.team.ui.JoinTeamUiState
import com.tourderun.feature.team.ui.TeamStartScreen

/** 앱을 열었을 때의 시작 화면. 닉네임이 없으면 S02부터 다시 한다(AUTH-02). */
internal fun EntryStage.startRoute(): Any = when (this) {
    EntryStage.SIGNED_OUT -> LoginRoute
    EntryStage.NEEDS_NICKNAME -> NicknameRoute
    EntryStage.NEEDS_TEAM -> TeamStartRoute
    EntryStage.READY -> HomeRoute
}

/** 백스택을 전부 비우고 [route]만 남긴다. 로그인·로그아웃·팀 가입·탈퇴 뒤에 뒤로 가기로 이전 화면에 못 돌아가게 한다. */
internal fun NavController.resetTo(route: Any) {
    navigate(route) { popUpTo(graph.id) { inclusive = true } }
}

/**
 * 최초 진입 화면 S01~S06 (#52). 입력값은 화면 안에서만 들고, 결과만 [session](목)에 남긴다.
 *
 * TODO(AUTH-01): 화면 주인(권민찬)이 ViewModel을 붙이면 각 `composable { }` 안의 상태와 [session] 호출을 바꾼다.
 */
internal fun NavGraphBuilder.entryGraph(navController: NavController, session: MockSession) {
    composable<LoginRoute> {
        LoginScreen(
            uiState = LoginUiState(),
            onGoogleSignIn = {
                session.signIn()
                // 처음 온 사용자(닉네임 없음)만 약관을 거친다.
                navController.resetTo(
                    if (session.stage == EntryStage.NEEDS_NICKNAME) TermsRoute else session.stage.startRoute(),
                )
            },
        )
    }

    composable<TermsRoute> {
        var agreed by remember { mutableStateOf(emptySet<TermsItem>()) }
        TermsScreen(
            uiState = TermsUiState(agreed),
            onBack = {
                session.signOut()
                navController.resetTo(LoginRoute)
            },
            onToggleAll = { agreed = if (agreed.size == TermsItem.entries.size) emptySet() else TermsItem.entries.toSet() },
            onToggle = { agreed = if (it in agreed) agreed - it else agreed + it },
            onViewTerms = {}, // TODO(AUTH-01): 약관 전문 보기
            onContinue = { navController.navigate(NicknameRoute) },
        )
    }

    composable<NicknameRoute> {
        var state by remember { mutableStateOf(NicknameUiState()) }
        NicknameScreen(
            uiState = state,
            // 앱을 다시 켜서 S02부터 시작한 경우에는 돌아갈 화면이 없어 아무 일도 없다.
            onBack = { navController.popBackStack() },
            onNicknameChange = { state = state.copy(nickname = it) },
            onSubmit = {
                if (state.isValid) {
                    session.saveNickname(state.nickname)
                    navController.resetTo(TeamStartRoute)
                }
            },
        )
    }

    composable<TeamStartRoute> {
        var dialog by rememberSaveable { mutableStateOf(TeamDialog.NONE) }
        TeamStartScreen(onCreateTeam = { dialog = TeamDialog.CREATE }, onJoinTeam = { dialog = TeamDialog.JOIN })

        when (dialog) {
            TeamDialog.NONE -> Unit
            TeamDialog.CREATE -> {
                var state by remember { mutableStateOf(CreateTeamUiState()) }
                CreateTeamDialog(
                    uiState = state,
                    onDismiss = { dialog = TeamDialog.NONE },
                    onNameChange = { state = state.copy(name = it) },
                    onColorSelect = { state = state.copy(color = it) },
                    onIconSelect = { state = state.copy(icon = it) },
                    onSubmit = {
                        if (state.isValid) {
                            session.joinTeam(MockTeam(state.name.trim(), TeamAppearance(state.color, state.icon)))
                            // S06에서 뒤로 가도 팀 시작이 아니라 홈이 나오게, 홈을 먼저 깔고 그 위에 S06을 올린다.
                            navController.resetTo(HomeRoute)
                            navController.navigate(InviteCodeRoute)
                        }
                    },
                )
            }
            TeamDialog.JOIN -> {
                var code by remember { mutableStateOf("") }
                val found = code == MockSession.INVITE_CODE
                val joinTeam = PreviewSamples.team
                JoinTeamDialog(
                    uiState = JoinTeamUiState(
                        code = code,
                        preview = if (found) {
                            JoinTeamPreviewUi(
                                name = joinTeam.name,
                                appearance = joinTeam.appearance,
                                memberCount = 3,
                                stageNumber = PreviewSamples.stage.stageNumber,
                                fromCity = PreviewSamples.stage.fromCity,
                                toCity = PreviewSamples.stage.toCity,
                            )
                        } else {
                            null
                        },
                        errorMessage = if (code.length == JoinTeamUiState.CODE_LENGTH && !found) {
                            stringResource(R.string.join_error_not_found)
                        } else {
                            null
                        },
                    ),
                    onDismiss = { dialog = TeamDialog.NONE },
                    onCodeChange = { code = it.uppercase() },
                    onJoin = {
                        if (found) {
                            session.joinTeam(MockTeam(joinTeam.name, joinTeam.appearance))
                            navController.resetTo(HomeRoute)
                        }
                    },
                )
            }
        }
    }

    composable<InviteCodeRoute> {
        val team = session.team
        val route = routeFor(SEASON)
        InviteCodeScreen(
            uiState = InviteCodeUiState(
                teamName = team?.name.orEmpty(),
                appearance = team?.appearance ?: TeamAppearance(),
                code = MockSession.INVITE_CODE,
                memberCount = 1,
                season = SEASON,
                routeFrom = route.cities.first().nameKo,
                routeTo = route.cities.last().nameKo,
                routeKm = (route.totalM / 1_000).toInt(),
            ),
            onBack = { navController.popBackStack() },
            onCopyCode = {}, // TODO(TEAM-02): 클립보드 복사
            onShareCode = {}, // TODO(TEAM-02): 공유 시트
            onDepart = { navController.popBackStack() },
        )
    }
}

private enum class TeamDialog { NONE, CREATE, JOIN }

private const val SEASON = 1
