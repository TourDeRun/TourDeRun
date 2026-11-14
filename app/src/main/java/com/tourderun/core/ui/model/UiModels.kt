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
 * 지금 달리는 스테이지와 팀 위치 (JRN-02).
 *
 * 직접 만들지 말고 [stageProgressUi]로 만든다. 진행률·남은 거리는 `locate()` 결과를 그대로 담고,
 * 화면에서 다시 계산하지 않는다(스테이지 경계 "시작 이상, 끝 미만"이 화면마다 달라지지 않게).
 *
 * @property stageNumber 1부터 (TeamPosition.stageIndex + 1). 완주면 마지막 스테이지
 * @property startKm 스테이지 시작 누적 거리(출발 도시 기준)
 * @property endKm 스테이지 끝 누적 거리
 * @property teamDistanceM 팀 누적 거리. 완주 후에도 계속 늘어난다
 * @property doneInStageM 스테이지 안에서 달린 거리. 완주면 스테이지 길이
 * @property toNextM 다음 도시까지 남은 거리. 완주면 0. 표시는 `Format.kmUp`(올림)
 * @property progress 스테이지 진행률 0~1. 완주면 1
 * @property arrivals 도착한 도시 수 0~[stageCount] (출발 도시 제외)
 * @property finished 마지막 도시 도착(완주). 위치는 마지막 도시에 멈춘다
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
    val doneInStageM: Long,
    val toNextM: Long,
    val progress: Float,
    val arrivals: Int,
    val finished: Boolean,
) {
    val lengthKm: Int get() = endKm - startKm

    /** 같은 스테이지에서 [distanceM] 위치의 진행률(다른 팀 마커, 러닝 전 위치). 스테이지 밖이면 0 또는 1. */
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
