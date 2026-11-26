package com.tourderun.feature.run.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tourderun.core.designsystem.theme.TdrTheme

/**
 * S10 러닝 결과를 [RunResultViewModel]에 연결한다. 저장이 끝나기 전(아주 잠깐)에는 빈 배경만 그린다.
 *
 * @param onDone 확인을 눌렀거나, 저장할 러닝이 없거나 저장을 시작하지 못했을 때. 홈으로 간다
 */
@Composable
fun RunResultHost(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RunResultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                RunResultEvent.Close -> onDone()
            }
        }
    }
    val state = uiState
    if (state == null) {
        Box(modifier.fillMaxSize().background(TdrTheme.colors.bgCanvas))
    } else {
        RunResultScreen(uiState = state, onConfirm = onDone, modifier = modifier)
    }
}
