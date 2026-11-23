package com.tourderun.feature.team.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * S04 팀 만들기 모달 (TEAM-01). 상태는 [CreateTeamViewModel], 그리기는 [CreateTeamDialog].
 * 만들면 [onCreated]로 알린다(부르는 쪽이 홈 위에 S06을 연다).
 */
@Composable
fun CreateTeamScreen(
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreateTeamViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CreateTeamEvent.Created -> onCreated()
            }
        }
    }

    CreateTeamDialog(
        uiState = uiState,
        onDismiss = onDismiss,
        onNameChange = { viewModel.onAction(CreateTeamAction.NameChanged(it)) },
        onColorSelect = { viewModel.onAction(CreateTeamAction.ColorSelected(it)) },
        onIconSelect = { viewModel.onAction(CreateTeamAction.IconSelected(it)) },
        onSubmit = { viewModel.onAction(CreateTeamAction.Submit) },
    )
}
