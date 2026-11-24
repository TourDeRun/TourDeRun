package com.tourderun.core.data.run

import kotlinx.coroutines.flow.Flow

/**
 * 다른 영역이 보는 러닝 활동 (RUN-02). 구현은 `RunSessionManager`(최한솔).
 * 로그아웃·탈퇴·팀 나가기는 [isBusy]가 true면 막는다.
 */
interface RunActivity {
    /**
     * 러닝이 진행 중이거나(카운트다운·측정·일시정지·종료 후 저장 전) 기기에 서버 확인을 기다리는 러닝이 있으면 true.
     * 이때 팀·계정이 바뀌면 그 러닝이 팀 없이 저장되거나 서버에서 거부된다.
     */
    val isBusy: Flow<Boolean>

    /** AUTH-05 탈퇴 뒤 기기의 러닝 데이터(위치점·진행 중 러닝·저장 대기 목록)를 전부 지운다. */
    suspend fun clearDeviceData()
}
