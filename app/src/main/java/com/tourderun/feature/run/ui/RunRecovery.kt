package com.tourderun.feature.run.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tourderun.R
import com.tourderun.core.data.Clock
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrButtonStyle
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSize
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format
import com.tourderun.domain.run.isTooShortToSave
import com.tourderun.feature.run.service.RunTrackingService
import com.tourderun.feature.run.session.RecoveredRun
import com.tourderun.feature.run.session.RunSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToLong
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** RUN-07 복구 안내가 끝난 뒤 갈 곳. */
enum class RecoveryOutcome { Resumed, Saved, Discarded }

/**
 * RUN-07 강제 종료 후 복구. 앱을 열 때 기기에 끝나지 않은 러닝이 남아 있고 측정 중이 아니면 안내를 띄운다.
 * 이어하기·지금까지 저장·버리기를 [RunSessionManager]에 넘긴다.
 */
@HiltViewModel
class RunRecoveryViewModel @Inject constructor(
    private val manager: RunSessionManager,
    private val clock: Clock,
) : ViewModel() {
    private val mutableRecovered = MutableStateFlow<RecoveredRun?>(null)
    val recovered: StateFlow<RecoveredRun?> = mutableRecovered.asStateFlow()

    private val outcomes = Channel<RecoveryOutcome>(Channel.BUFFERED)
    val outcomeFlow: Flow<RecoveryOutcome> = outcomes.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (manager.pendingRecovery() != null) mutableRecovered.value = manager.recover()
        }
    }

    fun resume() = act { _ ->
        if (manager.resume(clock.nowMs())) outcomes.send(RecoveryOutcome.Resumed)
    }

    fun saveSoFar() = act { run ->
        if (run.finished || manager.finish(run.endMs) != null) outcomes.send(RecoveryOutcome.Saved)
    }

    fun discard() = act { run ->
        if (manager.discard(run.runId)) outcomes.send(RecoveryOutcome.Discarded)
    }

    private fun act(block: suspend (RecoveredRun) -> Unit) {
        val run = mutableRecovered.value ?: return
        mutableRecovered.value = null
        viewModelScope.launch { block(run) }
    }
}

/**
 * 앱 최상단(내비게이션 위)에 둔다. 남은 러닝이 없으면 아무것도 그리지 않는다.
 *
 * @param onResumed 이어하기: 러닝 화면(S09)으로
 * @param onSaved 지금까지 저장: 결과 화면(S10)으로. 결과는 `RunSessionManager.state.value.result`
 */
@Composable
fun RunRecoveryHost(
    onResumed: () -> Unit,
    onSaved: () -> Unit,
    viewModel: RunRecoveryViewModel = hiltViewModel(),
) {
    val recovered by viewModel.recovered.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.outcomeFlow.collect { outcome ->
            when (outcome) {
                RecoveryOutcome.Resumed -> {
                    RunTrackingService.start(context)
                    onResumed()
                }
                RecoveryOutcome.Saved -> onSaved()
                RecoveryOutcome.Discarded -> Unit
            }
        }
    }
    recovered?.let { RunRecoveryDialog(it, viewModel::resume, viewModel::saveSoFar, viewModel::discard) }
}

@Composable
fun RunRecoveryDialog(
    run: RecoveredRun,
    onResume: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val tooShort = isTooShortToSave(run.distanceM, run.movingSec)
    AlertDialog(
        // 셋 중 하나를 골라야 다음 러닝을 시작할 수 있어 바깥을 눌러도 닫지 않는다.
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.run_recovery_title), style = type.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TdrSpacing.s2)) {
                Text(
                    stringResource(
                        R.string.run_recovery_body,
                        Format.km(run.distanceM.roundToLong(), decimals = 2),
                        Format.duration(run.movingSec),
                    ),
                    style = type.body,
                    color = colors.inkMuted,
                )
                if (tooShort) {
                    Text(stringResource(R.string.run_recovery_too_short), style = type.caption, color = colors.inkMuted)
                }
            }
        },
        confirmButton = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TdrSpacing.s2),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!run.finished) {
                    TdrButton(stringResource(R.string.run_recovery_resume), onResume, height = TdrSize.buttonMd)
                }
                if (!tooShort) {
                    TdrButton(
                        stringResource(R.string.run_recovery_save),
                        onSave,
                        style = if (run.finished) TdrButtonStyle.Primary else TdrButtonStyle.Outline,
                        height = TdrSize.buttonMd,
                    )
                }
                TextButton(onClick = onDiscard) {
                    Text(stringResource(R.string.run_recovery_discard), color = colors.danger, style = type.subtitle)
                }
            }
        },
        containerColor = colors.bgSurface,
        shape = TdrShapes.lg,
    )
}
