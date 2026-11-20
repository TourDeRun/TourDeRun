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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.tourderun.core.data.local.PendingSaveState
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrButtonStyle
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSize
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format
import com.tourderun.domain.run.isTooShortToSave
import com.tourderun.feature.run.data.RunSaveCoordinator
import com.tourderun.feature.run.service.RunTrackingService
import com.tourderun.feature.run.session.RecoveredRun
import com.tourderun.feature.run.session.RunPhase
import com.tourderun.feature.run.session.RunSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** RUN-07 복구 안내가 끝난 뒤 갈 곳. */
enum class RecoveryOutcome {
    /** 이어서 측정 중. 서비스를 시작하고 러닝 화면으로 간다. */
    Resumed,
    Discarded,

    /** 종료 상태로 만들었다. 결과 화면(S10)으로 가서 저장한다(REC-06). */
    OpenResult,
}

/** 복구 안내에서 고를 수 있는 것. */
enum class RecoveryChoice { Resume, Save, Discard }

/**
 * 복구 안내에 보일 선택지. 종료된 러닝(저장 전에 앱이 꺼졌거나 서버가 저장을 거부함)은 이어할 수 없고,
 * 기준 미달(M-05)이면 저장할 수 없다. 버리기는 항상 있다.
 */
fun recoveryChoices(run: RecoveredRun): List<RecoveryChoice> = buildList {
    if (!run.finished) add(RecoveryChoice.Resume)
    if (!isTooShortToSave(run.distanceM, run.movingSec)) add(RecoveryChoice.Save)
    add(RecoveryChoice.Discard)
}

/**
 * RUN-07 강제 종료 후 복구와 저장 거부 안내(#99). [RunRecoveryViewModel]이 viewModelScope로 쓰고, 테스트는 직접 만든다.
 *
 * - 앱을 열 때: 어디에도 속하지 않은 위치점을 정리하고, 저장 대기 러닝은 [RunSaveCoordinator.settleSaved]로 서버 반영을 기다린다.
 *   진행 중인 러닝이 남아 있으면 안내를 띄운다(저장을 시작한 직후 꺼진 종료 러닝은 안내 없이 저장 대기로 넘긴다).
 * - 그 뒤로 계속: 서버가 저장을 거부한 러닝이 있고 측정 중인 러닝이 없으면(IDLE), 원래 runId로 종료 상태에 올려 안내를 띄운다.
 *   측정 중이면 그 러닝을 끝낼 때까지 거부된 러닝은 저장 대기 목록에 그대로 둔다.
 */
class RunRecoveryController(
    private val manager: RunSessionManager,
    private val clock: Clock,
    private val saveCoordinator: RunSaveCoordinator,
    private val scope: CoroutineScope,
) {
    private val mutableRecovered = MutableStateFlow<RecoveredRun?>(null)
    val recovered: StateFlow<RecoveredRun?> = mutableRecovered.asStateFlow()

    private val outcomes = Channel<RecoveryOutcome>(Channel.BUFFERED)
    val outcomeFlow: Flow<RecoveryOutcome> = outcomes.receiveAsFlow()

    fun start() {
        scope.launch {
            recoverOnLaunch()
            showRejectedWhenIdle()
        }
    }

    private suspend fun recoverOnLaunch() {
        manager.pruneOrphanPoints()
        manager.pendingSaves.first()
            .filter { it.state == PendingSaveState.AWAITING }
            .forEach { saveCoordinator.settleSaved(it.runId) }
        if (manager.pendingRecovery() == null) return
        val run = manager.recover() ?: return
        // 저장을 시작한 직후(저장 대기로 옮기기 전) 앱이 꺼진 러닝은 안내 없이 저장 대기로 넘긴다(#23).
        if (run.finished && saveCoordinator.settleIfSavedOnDevice(run.runId)) return
        mutableRecovered.value = run
    }

    /** 거부된 러닝이 있고 측정 중이 아니면 안내를 띄운다. 끝나지 않는다(scope가 끝날 때까지). */
    private suspend fun showRejectedWhenIdle() {
        combine(manager.state, manager.pendingSaves) { state, pending ->
            state.phase == RunPhase.IDLE && pending.any { it.state == PendingSaveState.REJECTED }
        }
            .distinctUntilChanged()
            .collect { ready ->
                if (ready && mutableRecovered.value == null) manager.recoverRejected()?.let { mutableRecovered.value = it }
            }
    }

    fun resume() = act { _ ->
        if (manager.resume(clock.nowMs())) outcomes.send(RecoveryOutcome.Resumed)
    }

    /** 권한 안내에서 허용한 뒤 재개. 복구 안내는 이미 닫혀 있다. */
    fun resumeAfterPermission() {
        scope.launch {
            if (manager.resume(clock.nowMs())) outcomes.send(RecoveryOutcome.Resumed)
        }
    }

    /** 위치 권한이 없어 권한 안내로 넘어갈 때. 복구 안내만 닫고 러닝은 일시정지 그대로 둔다. */
    fun closeForPermission() {
        mutableRecovered.value = null
    }

    /** 이어하기 뒤 시스템이 서비스 시작을 거절했을 때. 다시 일시정지로 되돌린다. */
    fun onServiceStartFailed() {
        scope.launch { manager.stopForServiceFailure(clock.nowMs()) }
    }

    /**
     * "지금까지 저장"(REC-06). 일시정지 상태면 앱이 꺼진 시각([RecoveredRun.endMs])으로 종료하고 결과 화면으로 간다.
     * 이미 종료된 러닝(저장 전에 꺼졌거나 서버가 거부함)은 바로 결과 화면으로 간다. 결과 화면은 원래 runId로 저장한다.
     */
    fun save() = act { run ->
        if (run.finished || manager.finish(run.endMs) != null) outcomes.send(RecoveryOutcome.OpenResult)
    }

    fun discard() = act { run ->
        if (manager.discard(run.runId)) outcomes.send(RecoveryOutcome.Discarded)
    }

    private fun act(block: suspend (RecoveredRun) -> Unit) {
        val run = mutableRecovered.value ?: return
        mutableRecovered.value = null
        scope.launch { block(run) }
    }
}

/** [RunRecoveryController]를 화면 수명에 붙인다. */
@HiltViewModel
class RunRecoveryViewModel @Inject constructor(
    manager: RunSessionManager,
    clock: Clock,
    saveCoordinator: RunSaveCoordinator,
) : ViewModel() {
    private val controller = RunRecoveryController(manager, clock, saveCoordinator, viewModelScope).also { it.start() }

    val recovered: StateFlow<RecoveredRun?> = controller.recovered
    val outcomeFlow: Flow<RecoveryOutcome> = controller.outcomeFlow

    fun resume() = controller.resume()
    fun resumeAfterPermission() = controller.resumeAfterPermission()
    fun closeForPermission() = controller.closeForPermission()
    fun onServiceStartFailed() = controller.onServiceStartFailed()
    fun save() = controller.save()
    fun discard() = controller.discard()
}

/**
 * 앱 최상단(내비게이션 위)에 둔다. 남은 러닝이 없으면 아무것도 그리지 않는다.
 *
 * 이어하기를 눌렀는데 위치 권한이 없으면 복구 안내 대신 권한 안내를 띄운다. 그동안 러닝은 일시정지 그대로이고,
 * 권한을 받으면 재개한다. 권한 안내를 닫으면 일시정지로 남아 러닝 탭에서 다시 시작할 수 있다.
 *
 * @param onOpenRun 재개한 뒤 러닝 화면(S09)으로
 * @param onOpenResult "지금까지 저장"을 고른 뒤 결과 화면(S10)으로
 */
@Composable
fun RunRecoveryHost(
    onOpenRun: () -> Unit,
    onOpenResult: () -> Unit,
    viewModel: RunRecoveryViewModel = hiltViewModel(),
) {
    val recovered by viewModel.recovered.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var askPermission by rememberSaveable { mutableStateOf(false) }
    var startFailed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.outcomeFlow.collect { outcome ->
            when (outcome) {
                RecoveryOutcome.Resumed -> if (RunTrackingService.start(context)) {
                    onOpenRun()
                } else {
                    // 시작 요청이 거절되면 onStartCommand까지 가지 않으므로 여기서 일시정지로 되돌린다.
                    viewModel.onServiceStartFailed()
                    when (serviceStartFailureNotice(context.hasRunPermissions())) {
                        ServiceStartFailureNotice.Permission -> askPermission = true
                        ServiceStartFailureNotice.Retry -> startFailed = true
                    }
                }
                RecoveryOutcome.OpenResult -> onOpenResult()
                RecoveryOutcome.Discarded -> Unit
            }
        }
    }
    recovered?.let { run ->
        RunRecoveryDialog(
            run = run,
            // 그사이 위치 권한이 꺼졌으면 서비스가 시작되지 않으므로, 측정을 재개하지 않고 러닝 화면에서 권한을 안내한다.
            onResume = {
                if (context.hasRunPermissions()) {
                    viewModel.resume()
                } else {
                    viewModel.closeForPermission()
                    askPermission = true
                }
            },
            onSave = viewModel::save,
            onDiscard = viewModel::discard,
        )
    }
    if (askPermission) {
        RunPermissionDialog(
            onReady = {
                askPermission = false
                viewModel.resumeAfterPermission()
            },
            onDismiss = { askPermission = false },
        )
    }
    if (startFailed) RunStartFailedDialog(onDismiss = { startFailed = false })
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
    AlertDialog(
        // 하나를 골라야 다음 러닝을 시작할 수 있어 바깥을 눌러도 닫지 않는다.
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = {
            val title = if (run.saveRejected) R.string.run_recovery_rejected_title else R.string.run_recovery_title
            Text(stringResource(title), style = type.title)
        },
        text = {
            Text(
                stringResource(
                    if (run.saveRejected) R.string.run_recovery_rejected_body else R.string.run_recovery_body,
                    Format.km(run.distanceM.roundToLong(), decimals = 2),
                    Format.duration(run.movingSec),
                ),
                style = type.body,
                color = colors.inkMuted,
            )
        },
        confirmButton = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TdrSpacing.s2),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val choices = recoveryChoices(run)
                if (RecoveryChoice.Resume in choices) {
                    TdrButton(stringResource(R.string.run_recovery_resume), onResume, height = TdrSize.buttonMd)
                }
                if (RecoveryChoice.Save in choices) {
                    TdrButton(
                        stringResource(if (run.saveRejected) R.string.run_recovery_save_again else R.string.run_recovery_save),
                        onSave,
                        style = if (RecoveryChoice.Resume in choices) TdrButtonStyle.Outline else TdrButtonStyle.Primary,
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
