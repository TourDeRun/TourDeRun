package com.tourderun.core.ui.model

import com.tourderun.core.data.firestore.TeamMemberDto
import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.domain.team.contributionShares
import com.tourderun.domain.team.weeklyMvps

/**
 * 팀원 목록 화면 값 (TEAM-04·EXT-03). 홈(S07) 팀원 카드와 S14 팀 정보가 같이 쓴다.
 *
 * @property members 기여 거리 내림차순, 비율 합(이전 팀원 포함)은 100
 * @property formerM 나간·내보내진 팀원이 남긴 거리(M-02). 0이면 "이전 팀원" 줄을 숨긴다
 */
data class TeamMembersUi(
    val members: List<MemberUi>,
    val formerM: Long,
    val formerPercent: Int,
)

/**
 * 팀 거리 + 팀원 문서 + 이번 주 거리 → 팀원 목록.
 *
 * - 순서·비율: `contributionShares`(기여 거리 내림차순, 최대 나머지 방식으로 합 100%)
 * - MVP: `weeklyMvps`(1인 팀·모두 0km면 없음, 나간 팀원은 대상 아님)
 * - 아바타 색: 가입 순서대로 `TeamColor` 순서를 돌려 쓴다
 */
fun teamMembersUi(teamDistanceM: Long, members: List<TeamMemberDto>, weeklyMemberM: Map<String, Long>): TeamMembersUi {
    val shares = contributionShares(teamDistanceM, members.associate { it.uid to it.contributionM })
    val byUid = members.associateBy { it.uid }
    val colorOf = members.sortedWith(compareBy<TeamMemberDto> { it.joinedAtMs }.thenBy { it.uid })
        .mapIndexed { i, m -> m.uid to TeamColor.entries[i % TeamColor.entries.size] }
        .toMap()
    val mvps = weeklyMvps(weeklyMemberM, byUid.keys)
    val former = shares.firstOrNull { it.uid == null }
    return TeamMembersUi(
        members = shares.mapNotNull { share ->
            val member = share.uid?.let { byUid[it] } ?: return@mapNotNull null
            MemberUi(
                uid = member.uid,
                nickname = member.nickname,
                color = colorOf[member.uid] ?: TeamColor.BLUE,
                contributionM = share.distanceM,
                isLeader = member.isLeader,
                isMvp = member.uid in mvps,
                percent = share.percent,
            )
        },
        formerM = former?.distanceM ?: 0L,
        formerPercent = former?.percent ?: 0,
    )
}
