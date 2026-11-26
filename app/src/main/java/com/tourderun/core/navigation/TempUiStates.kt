package com.tourderun.core.navigation

import com.tourderun.core.data.record.FakeRunRepository
import com.tourderun.core.ui.preview.PreviewSamples
import com.tourderun.feature.profile.ui.CollectionSummaryUi
import com.tourderun.feature.profile.ui.ProfileUiState
import com.tourderun.feature.record.ui.toRowUi

/**
 * ViewModel이 아직 없는 화면을 [AppNavHost]에 띄우기 위한 임시 고정 상태. 값은 와이어프레임([PreviewSamples])과 같다.
 *
 * 화면 주인이 ViewModel을 붙이면 그 화면의 항목을 지운다. 전부 지워지면 이 파일도 지운다.
 */
internal object TempUiStates {
    // TODO(AUTH-03): 프로필 ViewModel로 바꾼다.
    val profile = ProfileUiState(
        nickname = "민지",
        teamName = PreviewSamples.team.name,
        isLeader = true,
        totalDistanceM = 86_400,
        runCount = 21,
        // 누르면 S12가 열리므로 가짜 Repository에 실제로 있는 기록이어야 한다.
        latestRun = FakeRunRepository.SAMPLE_RUNS.first().toRowUi(),
        collection = CollectionSummaryUi(3, 8, "천안"),
    )

    // TODO(TEAM-04): 팀 ViewModel로 바꾼다.
    // TODO(TEAM-05): 팀 수정 ViewModel로 바꾼다.
}
