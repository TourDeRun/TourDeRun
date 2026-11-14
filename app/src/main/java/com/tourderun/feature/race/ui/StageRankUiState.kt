package com.tourderun.feature.race.ui

import com.tourderun.core.ui.Format
import com.tourderun.core.ui.model.StageProgressUi

/** 순위 한 줄. 다른 팀은 이름·이미지(icon·color 번호)·인원·거리만 보인다(JRN-04). */
data class RankRowUi(
    val rank: Int,
    val name: String,
    /** 팀 아이콘 번호 0~7(M-06, `TeamIcon` 선언 순서) */
    val iconIndex: Int,
    /** 팀 색 번호 0~7(M-06, `TeamColor` 선언 순서) */
    val colorIndex: Int,
    val memberCount: Int,
    val distanceM: Long,
    /** 바로 앞 팀과의 거리 차(0 이상). 1위면 null */
    val gapToAheadM: Long?,
    val isOurTeam: Boolean,
) {
    /** 줄에 보이는 팀 거리(소수 한 자리 내림, `Format.teamKm`). 84_960 → "84.9" */
    val distanceKm: String get() = Format.teamKm(distanceM)
}

/** S15 스테이지 순위 상태 (RACE-01·02·03). */
sealed interface StageRankUiState {
    data object Loading : StageRankUiState

    /** 팀이 없음. 홈에서 들어올 수 없지만 팀을 나간 직후 등을 위해 둔다. */
    data object NoTeam : StageRankUiState

    /**
     * @property stage 헤더(스테이지 번호·도시·구간). 우리 팀 거리로 `stageProgressUi()`가 만든 값이라 홈·로드뷰와 같다
     * @property rows 같은 스테이지 팀, 순위순(같은 거리면 먼저 도달한 팀이 앞)
     * @property isOffline 기기 캐시 값이면 true(오프라인 배지)
     */
    data class Content(
        val stage: StageProgressUi,
        val ourRank: Int,
        val rows: List<RankRowUi>,
        val isOffline: Boolean,
    ) : StageRankUiState {
        val totalTeams: Int get() = rows.size

        /** 스테이지에 우리 팀뿐 */
        val isAlone: Boolean get() = rows.size == 1
    }
}
