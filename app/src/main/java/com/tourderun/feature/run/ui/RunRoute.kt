package com.tourderun.feature.run.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tourderun.R
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrButtonStyle
import com.tourderun.core.designsystem.component.TdrCard
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.core.ui.Format
import com.tourderun.core.ui.model.stageProgressUi
import com.tourderun.domain.route.routeFor
import com.tourderun.feature.run.service.RunTrackingService
import kotlin.math.roundToLong

/**
 * S09 러닝 진입점. 시작 전(권한·시작) → 카운트다운 → 측정 중 → 종료 후 선택지를 한 화면에서 바꿔 그린다.
 *
 * @param onOpenResult 저장할 러닝이 종료됐을 때. 결과는 `RunSessionManager.state.value.result`에 있다(S10·REC-01이 읽는다)
 * @param onClose 카운트다운 취소·버리기 후 이전 화면으로
 */
@Composable
fun RunRoute(
    onOpenResult: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RunViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var askPermission by rememberSaveable { mutableStateOf(false) }
    var startFailed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                // 이 화면이 보이는 동안에만 서비스를 시작한다(Android 12+ 백그라운드 시작 제한).
                RunEvent.StartService -> if (!RunTrackingService.start(context)) {
                    // 시작 요청이 거절되면 onStartCommand까지 가지 않으므로 여기서 러닝을 되돌린다.
                    viewModel.onServiceStartFailed()
                    when (serviceStartFailureNotice(context.hasRunPermissions())) {
                        ServiceStartFailureNotice.Permission -> askPermission = true
                        ServiceStartFailureNotice.Retry -> startFailed = true
                    }
                }
                RunEvent.RequestResume -> if (context.hasRunPermissions()) viewModel.resume() else askPermission = true
                RunEvent.OpenResult -> onOpenResult()
                RunEvent.Close -> onClose()
            }
        }
    }
    RunContent(
        uiState = uiState,
        onStart = viewModel::start,
        onCancelCountdown = viewModel::cancelCountdown,
        onPauseToggle = viewModel::togglePause,
        onStop = viewModel::stop,
        onContinue = viewModel::continueRun,
        onDiscard = viewModel::discard,
        onOpenResult = viewModel::openResult,
        onClose = onClose,
        modifier = modifier,
    )
    if (askPermission) {
        RunPermissionDialog(
            onReady = {
                askPermission = false
                viewModel.resume()
            },
            onDismiss = { askPermission = false },
        )
    }
    if (startFailed) RunStartFailedDialog(onDismiss = { startFailed = false })
}

/** RUN-04 측정 서비스를 시작하지 못했을 때 보일 안내. */
enum class ServiceStartFailureNotice {
    /** 위치·알림 권한이 없다. 권한 안내를 띄운다. */
    Permission,

    /** 권한은 있는데 시스템이 시작을 거절했다(백그라운드 시작 제한 등). 다시 시도하라고 알린다. */
    Retry,
}

fun serviceStartFailureNotice(hasRunPermissions: Boolean): ServiceStartFailureNotice =
    if (hasRunPermissions) ServiceStartFailureNotice.Retry else ServiceStartFailureNotice.Permission

/** 권한은 있는데 측정 서비스가 시작되지 않았을 때. 러닝은 일시정지(카운트다운이면 취소) 상태라 다시 누르면 된다. S09와 복구 안내가 같이 쓴다. */
@Composable
internal fun RunStartFailedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(stringResource(R.string.run_start_failed), style = TdrTheme.typography.body, color = TdrTheme.colors.ink) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_confirm), color = TdrTheme.colors.ink, style = TdrTheme.typography.subtitle)
            }
        },
        containerColor = TdrTheme.colors.bgSurface,
        shape = TdrShapes.lg,
    )
}

/**
 * 다시 시작할 때 위치·알림 권한이 꺼져 있으면 띄운다. 권한을 받기 전까지 러닝은 버튼을 누르기 전 상태
 * (일시정지 또는 종료 결과) 그대로다. S09와 복구 안내(RunRecoveryHost)가 같이 쓴다.
 */
@Composable
internal fun RunPermissionDialog(onReady: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_permission_resume_title), style = TdrTheme.typography.title) },
        text = { RunPermissionGate(onReady = onReady, continueLabel = stringResource(R.string.run_resume)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = TdrTheme.colors.ink, style = TdrTheme.typography.subtitle)
            }
        },
        containerColor = TdrTheme.colors.bgSurface,
        shape = TdrShapes.lg,
    )
}

@Composable
fun RunContent(
    uiState: RunScreenUiState,
    onStart: () -> Unit,
    onCancelCountdown: () -> Unit,
    onPauseToggle: () -> Unit,
    onStop: () -> Unit,
    onContinue: () -> Unit,
    onDiscard: () -> Unit,
    onOpenResult: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is RunScreenUiState.Running -> RunningScreen(uiState.running, onPauseToggle, onStop, modifier)
        else -> TdrTheme(darkTheme = true) {
            RunFrame(modifier) {
                when (uiState) {
                    RunScreenUiState.Loading -> Unit
                    RunScreenUiState.NoTeam -> NoTeamContent(onClose)
                    is RunScreenUiState.Ready -> ReadyContent(uiState, onStart, onClose)
                    is RunScreenUiState.Countdown -> CountdownContent(uiState.sec, onCancelCountdown)
                    is RunScreenUiState.Finished -> FinishedContent(uiState, onContinue, onDiscard, onOpenResult)
                    is RunScreenUiState.Running -> Unit
                }
            }
        }
    }
}

@Composable
private fun RunFrame(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .background(TdrTheme.colors.bgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = TdrSpacing.screenHorizontal, vertical = TdrSpacing.s6),
        verticalArrangement = Arrangement.spacedBy(TdrSpacing.s4),
        content = content,
    )
}

@Composable
private fun ColumnScope.NoTeamContent(onClose: () -> Unit) {
    Text(stringResource(R.string.run_no_team_title), style = TdrTheme.typography.titleLg)
    Text(stringResource(R.string.run_no_team_body), style = TdrTheme.typography.body, color = TdrTheme.colors.inkMuted)
    Spacer(Modifier.weight(1f))
    TdrButton(stringResource(R.string.run_close), onClose, style = TdrButtonStyle.Outline)
}

@Composable
private fun ColumnScope.ReadyContent(uiState: RunScreenUiState.Ready, onStart: () -> Unit, onClose: () -> Unit) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    val stage = uiState.stage
    Text(stringResource(R.string.run_ready_title), style = type.titleLg)
    TdrCard(verticalArrangement = Arrangement.spacedBy(TdrSpacing.s1)) {
        Text(
            stringResource(R.string.run_ready_stage, stage.stageNumber, stage.fromCity, stage.toCity),
            style = type.subtitle,
        )
        Text(
            if (stage.finished) {
                stringResource(R.string.stage_finished, stage.toCity)
            } else {
                stringResource(R.string.run_to_next, stage.toCity, Format.kmUp(stage.toNextM, decimals = 2))
            },
            style = type.caption,
            color = colors.inkMuted,
        )
    }
    RunPermissionGate(
        onReady = onStart,
        continueLabel = stringResource(R.string.run_start),
        modifier = Modifier.fillMaxWidth(),
    )
    BatteryOptimizationTip(Modifier.fillMaxWidth())
    Spacer(Modifier.weight(1f))
    TdrButton(stringResource(R.string.run_close), onClose, style = TdrButtonStyle.Outline)
}

@Composable
private fun ColumnScope.CountdownContent(sec: Int, onCancel: () -> Unit) {
    val type = TdrTheme.typography
    Spacer(Modifier.weight(1f))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            sec.coerceAtLeast(1).toString(),
            style = type.runHero,
            color = TdrTheme.colors.brandBright,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    Text(
        stringResource(R.string.run_countdown_gps),
        style = type.body,
        color = TdrTheme.colors.inkMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.weight(1f))
    TdrButton(stringResource(R.string.run_countdown_cancel), onCancel, style = TdrButtonStyle.Outline)
}

@Composable
private fun ColumnScope.FinishedContent(
    uiState: RunScreenUiState.Finished,
    onContinue: () -> Unit,
    onDiscard: () -> Unit,
    onOpenResult: () -> Unit,
) {
    val type = TdrTheme.typography
    val km = Format.km(uiState.distanceM.roundToLong(), decimals = 2)
    val time = Format.duration(uiState.movingSec)
    if (uiState.tooShort) {
        Text(stringResource(R.string.run_too_short_title), style = type.titleLg)
        Text(stringResource(R.string.run_too_short_body, km, time), style = type.body, color = TdrTheme.colors.inkMuted)
        Spacer(Modifier.weight(1f))
        TdrButton(stringResource(R.string.run_too_short_continue), onContinue)
        TdrButton(stringResource(R.string.run_too_short_discard), onDiscard, style = TdrButtonStyle.OutlineDanger)
    } else {
        // 종료 후 결과 화면을 닫았거나 앱을 다시 연 경우. 저장(REC-01)은 결과 화면에서 한다.
        Text(stringResource(R.string.run_finished_title), style = type.titleLg)
        Text(stringResource(R.string.run_finished_body, km, time), style = type.body, color = TdrTheme.colors.inkMuted)
        Spacer(Modifier.weight(1f))
        TdrButton(stringResource(R.string.run_finished_open), onOpenResult)
        TdrButton(stringResource(R.string.run_too_short_discard), onDiscard, style = TdrButtonStyle.OutlineDanger)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RunReadyPreview() {
    RunContent(RunScreenUiState.Ready(stageProgressUi(66_800, routeFor(1))), {}, {}, {}, {}, {}, {}, {}, {})
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RunCountdownPreview() {
    RunContent(RunScreenUiState.Countdown(3), {}, {}, {}, {}, {}, {}, {}, {})
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RunTooShortPreview() {
    RunContent(RunScreenUiState.Finished(42.0, 35, tooShort = true), {}, {}, {}, {}, {}, {}, {}, {})
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RunNoTeamPreview() {
    RunContent(RunScreenUiState.NoTeam, {}, {}, {}, {}, {}, {}, {}, {})
}
