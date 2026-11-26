package com.tourderun.feature.team.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** S05 초대 코드로 참여 모달 (TEAM-03). 상태는 [JoinTeamViewModel], 그리기는 [JoinTeamDialog]. */
@Composable
fun JoinTeamScreen(
    onDismiss: () -> Unit,
    onJoined: () -> Unit,
    viewModel: JoinTeamViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                JoinTeamEvent.Joined -> onJoined()
            }
        }
    }

    JoinTeamDialog(
        uiState = uiState,
        onDismiss = onDismiss,
        onCodeChange = { viewModel.onAction(JoinTeamAction.CodeChanged(it)) },
        onJoin = { viewModel.onAction(JoinTeamAction.Join) },
    )
}
