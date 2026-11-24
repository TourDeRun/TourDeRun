package com.tourderun.feature.run.session

import com.tourderun.core.data.local.PendingSaveState
import com.tourderun.core.data.run.RunBusyReason

/**
 * RUN-02 러닝 때문에 계정·팀 변경을 막아야 하는 이유. 막지 않아도 되면 null.
 *
 * - 메모리 상태가 진행 중이거나(종료 직후 FINISHED 포함) 기기에 진행 중 러닝이 남아 있으면 [RunBusyReason.ACTIVE]
 *   (앱을 다시 켜 메모리는 IDLE이어도 기기에 남은 러닝은 복구 대상이다)
 * - 저장 거절 기록이 있으면 [RunBusyReason.SAVE_REJECTED] (사용자가 다시 저장·버리기를 해야 끝나서 대기보다 먼저 알린다)
 * - 서버 확인 대기만 있으면 [RunBusyReason.SAVING]
 */
internal fun runBusyReason(
    phase: RunPhase,
    hasStoredSession: Boolean,
    pendingStates: List<PendingSaveState>,
): RunBusyReason? = when {
    phase != RunPhase.IDLE || hasStoredSession -> RunBusyReason.ACTIVE
    PendingSaveState.REJECTED in pendingStates -> RunBusyReason.SAVE_REJECTED
    pendingStates.isNotEmpty() -> RunBusyReason.SAVING
    else -> null
}
