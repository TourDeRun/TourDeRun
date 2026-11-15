package com.tourderun.feature.stat.ui

/** 이번 주 기간. 화면 문구 "2026-W46 · 11월 9일(월) ~ 15일(일)". */
data class WeekPeriodUi(
    val weekId: String,
    val startMonth: Int,
    val startDay: Int,
    val endMonth: Int,
    val endDay: Int,
)

/** 지난주 대비 이번 주 팀 거리. */
sealed interface WeeklyChangeUi {
    /** [diffM] > 0: "+6.1km", < 0: "−2.0km" */
    data class Diff(val diffM: Long) : WeeklyChangeUi

    /** 지난주와 같은 거리 */
    data object Same : WeeklyChangeUi

    /** 지난주 0km(기록 없음) */
    data object NoLastWeek : WeeklyChangeUi
}

/** 상위 % 카드. */
sealed interface WeeklyRankUi {
    /** "전체 [totalTeams]팀 중 상위 [percent]%", "이번 주 우리보다 많이 달린 팀 [moreCount]개" */
    data class Shown(val percent: Int, val totalTeams: Long, val moreCount: Long) : WeeklyRankUi

    /** 이번 주 0km: % 대신 "이번 주 첫 러닝을 시작해 보세요" */
    data object FirstRun : WeeklyRankUi

    /** 오프라인이거나 아직 조회 전·조회 실패: 카드를 그리지 않는다 */
    data object Hidden : WeeklyRankUi
}

/** S16 주간 통계 상태 (STAT-01·02). */
sealed interface WeeklyStatsUiState {
    data object Loading : WeeklyStatsUiState

    /** 팀이 없음. 홈에서 들어올 수 없지만 팀을 나간 직후 등을 위해 둔다. */
    data object NoTeam : WeeklyStatsUiState

    /**
     * @property isOffline 기기 캐시 값이면 true(오프라인 배지, 상위 % 숨김)
     */
    data class Content(
        val period: WeekPeriodUi,
        val distanceM: Long,
        val runCount: Long,
        val lastWeekDistanceM: Long,
        val lastWeekRunCount: Long,
        val change: WeeklyChangeUi,
        val rank: WeeklyRankUi,
        val isOffline: Boolean,
    ) : WeeklyStatsUiState
}

/** S16 사용자 동작. */
sealed interface WeeklyStatsAction {
    data object Back : WeeklyStatsAction
}
