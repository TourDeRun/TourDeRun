package com.tourderun.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.designsystem.component.TdrTab
import com.tourderun.core.designsystem.component.TdrTabBar
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.feature.collection.ui.CardDetailScreen
import com.tourderun.feature.collection.ui.CollectionScreen
import com.tourderun.feature.home.ui.HomeScreen
import com.tourderun.feature.journey.ui.RoadViewScreen
import com.tourderun.feature.journey.ui.StageClearHost
import com.tourderun.feature.profile.ui.ProfileScreen
import com.tourderun.feature.profile.ui.ProfileViewModel
import com.tourderun.feature.race.ui.StageRankScreen
import com.tourderun.feature.record.ui.RecordDetailScreen
import com.tourderun.feature.record.ui.RecordsScreen
import com.tourderun.feature.run.ui.RunResultHost
import com.tourderun.feature.run.ui.RunRecoveryHost
import com.tourderun.feature.run.ui.RunRoute
import com.tourderun.feature.stat.ui.WeeklyStatsScreen
import com.tourderun.feature.team.ui.TeamEditScreen
import com.tourderun.feature.team.ui.TeamScreen

/**
 * 앱 화면 연결. 탭 화면(홈·로드뷰·프로필)에서만 하단 탭을 보이고, 나머지는 화면 전체를 쓴다.
 *
 * ViewModel이 없는 화면은 [TempUiStates]의 고정 상태로 띄운다. 화면 주인은 ViewModel을 붙일 때
 * 해당 `composable { }` 안의 상태와 빈 콜백(`{}`)을 자기 것으로 바꾼다.
 *
 * 시작 화면은 [EntryStage]로 정한다. 로그인·닉네임·팀 여부는 모두 [SessionRepository](Firebase)의 값이다.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination?.tab()
    val back: () -> Unit = { navController.popBackStack() }
    val entryViewModel: EntryViewModel = hiltViewModel()
    val sessionState by entryViewModel.sessionState.collectAsStateWithLifecycle()
    val stage = sessionState.toEntryStage()

    // 시작 화면은 세션 상태의 첫 값으로 한 번만 정한다. 그 전에는 로딩 화면을 보인다.
    var startRoute by remember { mutableStateOf<Any?>(null) }
    LaunchedEffect(stage) {
        if (startRoute == null && stage != null) startRoute = stage.startRoute()
    }
    val start = startRoute
    if (start == null) {
        EntryLoadingScreen(modifier)
        return
    }
    // 그 뒤로는 세션 상태가 바뀌면 진입 화면 사이를 옮긴다 (로그인 성공, 닉네임 저장, 로그아웃).
    LaunchedEffect(stage) {
        if (stage != null) navController.followEntryStage(stage)
    }

    Column(modifier.fillMaxSize().background(TdrTheme.colors.bgCanvas)) {
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            entryGraph(navController, onSignOut = entryViewModel::signOut)

            composable<HomeRoute> {
                HomeScreen(
                    onTeamClick = { navController.navigate(TeamRoute) },
                    onStageRankClick = { navController.navigate(StageRankRoute) },
                    onWeeklyStatsClick = { navController.navigate(WeeklyStatsRoute) },
                )
                StageClearHost(onViewCard = { navController.navigate(CardDetailRoute(it)) })
            }
            composable<RoadViewRoute> {
                RoadViewScreen()
            }
            composable<ProfileRoute> {
                // TODO(AUTH-03): 닉네임 수정.
                val profileViewModel: ProfileViewModel = hiltViewModel()
                val header by profileViewModel.header.collectAsStateWithLifecycle()
                // AUTH-04: 확인 창이 열렸는지는 화면 회전에도 남게 rememberSaveable로 둔다.
                var showLogoutConfirm by rememberSaveable { mutableStateOf(false) }
                ProfileScreen(
                    uiState = TempUiStates.profile.copy(
                        nickname = header.nickname ?: TempUiStates.profile.nickname,
                        teamName = header.teamName,
                        showLogoutConfirm = showLogoutConfirm,
                    ),
                    onEditNickname = {},
                    onTeamClick = { navController.navigate(TeamRoute) },
                    onRecordsClick = { navController.navigate(RecordsRoute) },
                    onRunClick = { navController.navigate(RecordDetailRoute(it.runId)) },
                    onCollectionClick = { navController.navigate(CollectionRoute) },
                    onLogout = { showLogoutConfirm = true },
                    onLogoutDismiss = { showLogoutConfirm = false },
                    onLogoutConfirm = {
                        showLogoutConfirm = false
                        entryViewModel.signOut()
                    },
                    // TODO(AUTH-05): 실제 탈퇴. 지금은 로그아웃만 한다.
                    onWithdraw = entryViewModel::signOut,
                )
            }

            composable<RunningRoute> {
                // 권한·카운트다운·측정·종료는 RunRoute 안에서 바뀐다. 종료 결과는 RunSessionManager.state.value.result.
                RunRoute(
                    onOpenResult = {
                        navController.navigate(RunResultRoute) { popUpTo<RunningRoute> { inclusive = true } }
                    },
                    onClose = back,
                )
            }
            composable<RunResultRoute> {
                // 종료된 러닝을 저장하고 결과를 보인다(REC-01). 저장할 러닝이 없으면 바로 홈으로 간다.
                RunResultHost(onDone = { navController.popBackStack(HomeRoute, inclusive = false) })
                StageClearHost(onViewCard = { navController.navigate(CardDetailRoute(it)) })
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
                // 초대 코드 공유는 S06을 다시 쓴다. 내보내기(TEAM-06)·나가기(TEAM-07)는 TeamScreen 안 TODO
                TeamScreen(
                    onBack = back,
                    onEdit = { navController.navigate(TeamEditRoute) },
                    onShowInvite = { navController.navigate(InviteCodeRoute) },
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

    // RUN-07: 앱을 열 때 끝나지 않은 러닝이 있으면 어느 화면 위에서든 복구 안내를 먼저 띄운다.
    RunRecoveryHost(
        onOpenRun = { navController.navigate(RunningRoute) { launchSingleTop = true } },
        onOpenResult = { navController.navigate(RunResultRoute) { launchSingleTop = true } },
    )
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
            // 팀 확인·권한 요청·카운트다운은 RunRoute(S09) 안에서 한다.
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
