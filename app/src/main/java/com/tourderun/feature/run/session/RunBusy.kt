package com.tourderun.feature.run.session

/**
 * RUN-02 러닝 때문에 계정·팀 변경을 막아야 하는지.
 * 메모리 상태가 진행 중이거나, 기기에 진행 중 러닝·저장 대기 러닝이 남아 있으면 막는다
 * (앱을 다시 켜 메모리는 IDLE이어도 기기에 남은 러닝은 복구·저장 대상이다).
 */
internal fun isRunBusy(phase: RunPhase, hasStoredSession: Boolean, pendingSaveCount: Int): Boolean =
    phase != RunPhase.IDLE || hasStoredSession || pendingSaveCount > 0
