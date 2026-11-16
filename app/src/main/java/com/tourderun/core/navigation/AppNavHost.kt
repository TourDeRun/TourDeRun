package com.tourderun.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.tourderun.core.designsystem.component.TdrTab
import com.tourderun.core.designsystem.component.TdrTabBar
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.feature.collection.ui.CardDetailScreen
import com.tourderun.feature.collection.ui.CollectionScreen
import com.tourderun.feature.home.ui.HomeScreen
import com.tourderun.feature.journey.ui.RoadViewScreen
import com.tourderun.feature.profile.ui.ProfileScreen
import com.tourderun.feature.race.ui.StageRankScreen
import com.tourderun.feature.record.ui.RecordDetailScreen
import com.tourderun.feature.record.ui.RecordsScreen
import com.tourderun.feature.run.ui.RunResultScreen
import com.tourderun.feature.run.ui.RunningScreen
import com.tourderun.feature.stat.ui.WeeklyStatsScreen
import com.tourderun.feature.team.ui.TeamEditScreen
import com.tourderun.feature.team.ui.TeamScreen

/**
 * 앱 화면 연결. 탭 화면(홈·로드뷰·프로필)에서만 하단 탭을 보이고, 나머지는 화면 전체를 쓴다.
 *
 * ViewModel이 없는 화면은 [TempUiStates]의 고정 상태로 띄운다. 화면 주인은 ViewModel을 붙일 때
 * 해당 `composable { }` 안의 상태와 빈 콜백(`{}`)을 자기 것으로 바꾼다.
 *
 * 시작 화면은 [EntryStage]로 정한다. 지금은 로그인·팀 상태가 목([MockSession])이라 기기에만 저장된다.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination?.tab()
    val back: () -> Unit = { navController.popBackStack() }
    // TODO(AUTH-01): SessionRepository·TeamRepository가 생기면 목 대신 그 값으로 EntryStage를 계산한다.
    val context = LocalContext.current
    val session = remember { MockSession(context.applicationContext) }
    // 시작 화면은 앱을 열 때 한 번만 정한다. 그 뒤 이동은 각 화면이 resetTo()로 한다.
    val startRoute = remember { session.stage.startRoute() }

    Column(modifier.fillMaxSize().background(TdrTheme.colors.bgCanvas)) {
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            entryGraph(navController, session)

            composable<HomeRoute> {
                HomeScreen(
                    onTeamClick = { navController.navigate(TeamRoute) },
                    onStageRankClick = { navController.navigate(StageRankRoute) },
                    onWeeklyStatsClick = { navController.navigate(WeeklyStatsRoute) },
                )
            }
            composable<RoadViewRoute> {
                RoadViewScreen()
            }
            composable<ProfileRoute> {
                // TODO(AUTH-03): 닉네임 수정. 로그아웃·탈퇴는 목이라 확인 없이 바로 되고 화면 이동만 한다.
                ProfileScreen(
                    uiState = TempUiStates.profile.copy(
                        nickname = session.nickname ?: TempUiStates.profile.nickname,
                        teamName = session.team?.name,
                    ),
                    onEditNickname = {},
                    onTeamClick = { navController.navigate(TeamRoute) },
                    onRecordsClick = { navController.navigate(RecordsRoute) },
                    onRunClick = { navController.navigate(RecordDetailRoute(it.runId)) },
                    onCollectionClick = { navController.navigate(CollectionRoute) },
                    onLogout = {
                        session.signOut()
                        navController.resetTo(LoginRoute)
                    },
                    onWithdraw = {
                        session.withdraw()
                        navController.resetTo(LoginRoute)
                    },
                )
            }

            composable<RunningRoute> {
                // TODO(RUN-02): 일시정지·종료는 RunSessionManager가 처리하고, 종료 결과로 결과 화면을 연다.
                RunningScreen(
                    uiState = TempUiStates.running,
                    onPauseToggle = {},
                    onStopLongPress = {
                        navController.navigate(RunResultRoute) { popUpTo<RunningRoute> { inclusive = true } }
                    },
                )
            }
            composable<RunResultRoute> {
                RunResultScreen(
                    uiState = TempUiStates.runResult,
                    onConfirm = { navController.popBackStack(HomeRoute, inclusive = false) },
                )
            }

            composable<RecordsRoute> {
                RecordsScreen(
                    onBack = back,
                    onRunClick = { navController.navigate(RecordDetailRoute(it.runId)) },
                    onStartRun = { navController.selectTab(TdrTab.Run) },
                )
            }
            composable<RecordDetailRoute> {
                // 삭제로 닫힐 때 사용자가 뒤로 가기를 같이 눌러도 이 화면까지만 닫히게 경로를 지정한다.
                RecordDetailScreen(onBack = { navController.popBackStack<RecordDetailRoute>(inclusive = true) })
            }

            composable<TeamRoute> {
                // TODO(TEAM-02): 초대 코드 공유 시트, TODO(TEAM-06): 내보내기. 팀 나가기는 목이라 화면 이동만 한다.
                TeamScreen(
                    uiState = TempUiStates.team,
                    onBack = back,
                    onEdit = { navController.navigate(TeamEditRoute) },
                    onKick = {},
                    onShareInvite = {},
                    onLeave = {
                        session.leaveTeam()
                        navController.resetTo(TeamStartRoute)
                    },
                )
            }
            composable<TeamEditRoute> {
                // TODO(TEAM-05): 이름·색·아이콘 입력과 저장
                TeamEditScreen(
                    uiState = TempUiStates.teamEdit,
                    onBack = back,
                    onSave = back,
                    onNameChange = {},
                    onColorSelect = {},
                    onIconSelect = {},
                )
            }

            composable<StageRankRoute> { StageRankScreen(onBack = back) }
            composable<WeeklyStatsRoute> { WeeklyStatsScreen(onBack = back) }

            composable<CollectionRoute> {
                CollectionScreen(
                    uiState = TempUiStates.collection,
                    onBack = back,
                    onCardClick = { navController.navigate(CardDetailRoute(it.cityId)) },
                )
            }
            composable<CardDetailRoute> { entry ->
                CardDetailScreen(
                    uiState = TempUiStates.cardDetail(entry.toRoute<CardDetailRoute>().cityId),
                    onBack = back,
                )
            }
        }

        if (currentTab != null) {
            TdrTabBar(selected = currentTab, onSelect = { navController.selectTab(it) })
        }
    }
}

/** 이 화면이 탭 화면이면 그 탭, 아니면 null(하단 탭을 숨긴다). */
private fun NavDestination.tab(): TdrTab? = when {
    hasRoute<HomeRoute>() -> TdrTab.Home
    hasRoute<RoadViewRoute>() -> TdrTab.Road
    hasRoute<ProfileRoute>() -> TdrTab.Profile
    else -> null
}

/** 탭 전환. 탭을 오가도 스크롤 위치 같은 화면 상태가 남고, 뒤로 가기는 항상 홈을 거쳐 앱을 나간다. */
private fun NavController.selectTab(tab: TdrTab) {
    val route: Any = when (tab) {
        TdrTab.Home -> HomeRoute
        TdrTab.Road -> RoadViewRoute
        TdrTab.Profile -> ProfileRoute
        TdrTab.Run -> {
            // 러닝은 탭이 아니라 화면 전체를 덮는 화면이라 그냥 위에 쌓는다.
            // TODO(RUN-01): 팀 확인·권한 요청·카운트다운은 러닝 담당이 이 앞에 붙인다.
            navigate(RunningRoute) { launchSingleTop = true }
            return
        }
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
