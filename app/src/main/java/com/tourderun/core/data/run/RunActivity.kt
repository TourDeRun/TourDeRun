package com.tourderun.core.data.run

import kotlinx.coroutines.flow.Flow

/** RUN-02 계정·팀 변경을 막는 이유. 이유마다 사용자가 할 일이 달라서 안내 문구도 다르다. */
enum class RunBusyReason {
    /** 카운트다운·측정·일시정지·종료 직후(저장 전), 또는 앱 재시작 뒤 기기에 진행 중 러닝이 남음 → 러닝을 마친다. */
    ACTIVE,

    /** 저장을 시작했고 서버 확인을 기다리는 중(AWAITING) → 연결되면 저절로 끝난다. */
    SAVING,

    /** 서버가 저장을 거절한 기록이 남음(REJECTED) → 다시 저장하거나 버린다. */
    SAVE_REJECTED,
}

/**
 * 다른 영역이 보는 러닝 활동 (RUN-02). 구현은 `RunSessionManager`(최한솔).
 * 로그아웃·탈퇴·팀 나가기는 [busyReason]이 null이 아니면 막고 그 이유에 맞는 안내를 보여 준다.
 */
interface RunActivity {
    /**
     * 지금 계정·팀 변경을 막아야 하는 이유. 막지 않아도 되면 null.
     * 이때 팀·계정이 바뀌면 그 러닝이 팀 없이 저장되거나, 다음 계정에 이전 계정 러닝의 복구 안내가 뜬다.
     */
    val busyReason: Flow<RunBusyReason?>

    /** AUTH-05 탈퇴 뒤 기기의 러닝 데이터(위치점·진행 중 러닝·저장 대기 목록)를 전부 지운다. */
    suspend fun clearDeviceData()
}
