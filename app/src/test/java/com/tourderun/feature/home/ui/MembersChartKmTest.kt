package com.tourderun.feature.home.ui

import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.ui.model.MemberUi
import org.junit.Assert.assertEquals
import org.junit.Test

class MembersChartKmTest {
    @Test fun `1인 팀 84960m → 팀 합계와 기여 거리가 둘 다 84점9`() {
        val members = listOf(MemberUi("u1", "민지", TeamColor.BLUE, 84_960, isLeader = true))

        val km = membersChartKm(members, teamDistanceM = 84_960)

        assertEquals("84.9", km.totalKm)
        assertEquals(listOf("84.9"), km.memberKm)
    }

    @Test fun `여러 명이면 팀원 순서대로 각자 내림 → 합계보다 큰 기여 거리가 없다`() {
        val members = listOf(
            MemberUi("u1", "민지", TeamColor.BLUE, 40_990),
            MemberUi("u2", "서준", TeamColor.entries[1], 43_970),
        )

        val km = membersChartKm(members, teamDistanceM = 84_960)

        assertEquals("84.9", km.totalKm)
        assertEquals(listOf("40.9", "43.9"), km.memberKm)
    }

    @Test fun `팀원이 없으면 합계만`() {
        val km = membersChartKm(emptyList(), teamDistanceM = 0)

        assertEquals("0.0", km.totalKm)
        assertEquals(emptyList<String>(), km.memberKm)
    }
}
