package com.tourderun.core.ui.model

import androidx.compose.runtime.Immutable
import com.tourderun.core.designsystem.component.TeamAppearance
import com.tourderun.core.designsystem.component.TeamColor

/**
 * 여러 화면이 같이 쓰는 화면용 값. ViewModel이 :domain 계산 결과(Route, TeamPosition)로 만든다.
 * 거리는 정수 m, 시간은 초.
 */

/** 팀 요약 (홈 머리 카드, 팀 화면, 로드뷰의 우리 팀). */
@Immutable
data class TeamUi(
    val name: String,
    val appearance: TeamAppearance,
    val season: Int,
    val memberCount: Int,
    val maxMembers: Int = 4,
    val distanceM: Long,
)

/** 다른 팀 (순위·로드뷰). 다른 팀은 이름·이미지·인원·거리만 보인다. */
@Immutable
data class RivalTeamUi(
    val name: String,
    val appearance: TeamAppearance,
    val memberCount: Int,
    val distanceM: Long,
)

/** 팀원 한 명과 기여 거리. */
@Immutable
data class MemberUi(
    val uid: String,
    val nickname: String,
    val color: TeamColor,
    val contributionM: Long,
    val isLeader: Boolean = false,
    val isMvp: Boolean = false,
)

/**
 * 지금 달리는 스테이지와 팀 위치.
 *
 * @property stageNumber 1부터 (TeamPosition.stageIndex + 1)
 * @property startKm 스테이지 시작 누적 거리(출발 도시 기준)
 * @property endKm 스테이지 끝 누적 거리
 * @property teamDistanceM 팀 누적 거리
 */
@Immutable
data class StageProgressUi(
    val stageNumber: Int,
    val stageCount: Int,
    val fromCity: String,
    val toCity: String,
    val startKm: Int,
    val endKm: Int,
    val teamDistanceM: Long,
) {
    val lengthKm: Int get() = endKm - startKm
    val doneInStageM: Long get() = (teamDistanceM - startKm * 1_000L).coerceIn(0L, lengthKm * 1_000L)
    val toNextM: Long get() = (endKm * 1_000L - teamDistanceM).coerceAtLeast(0L)
    val progress: Float get() = if (lengthKm == 0) 1f else doneInStageM / (lengthKm * 1_000f)

    /** 같은 스테이지에서 [distanceM] 위치의 진행률. */
    fun progressOf(distanceM: Long): Float =
        if (lengthKm == 0) 1f else ((distanceM - startKm * 1_000L) / (lengthKm * 1_000f)).coerceIn(0f, 1f)
}

/** 내 기록 한 줄 (목록·프로필 최근 기록). */
@Immutable
data class RunRowUi(
    val runId: String,
    val month: Int,
    val day: Int,
    val title: String,
    val movingSec: Long,
    val paceSecPerKm: Long,
    val distanceM: Long,
)
