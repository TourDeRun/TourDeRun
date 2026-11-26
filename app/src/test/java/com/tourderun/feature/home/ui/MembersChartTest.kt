package com.tourderun.feature.home.ui

import com.tourderun.core.designsystem.component.TeamColor
import com.tourderun.core.ui.model.MemberUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** S07 팀원 기여 카드 값 (TEAM-04·EXT-03). */
class MembersChartTest {

    @Test fun `1인 팀 84960m → 팀 합계와 기여 거리가 둘 다 84점9`() {
        val members = listOf(MemberUi("u1", "민지", TeamColor.BLUE, 84_960, isLeader = true, percent = 100))

        val chart = membersChart(members, teamDistanceM = 84_960)

        assertEquals("84.9", chart.totalKm)
        assertEquals(listOf("84.9"), chart.bars.map { it.km })
    }

    @Test fun `여러 명이면 팀원 순서대로 각자 내림 → 합계보다 큰 기여 거리가 없다`() {
        val members = listOf(
            MemberUi("u2", "서준", TeamColor.NAVY, 43_970, percent = 52),
            MemberUi("u1", "민지", TeamColor.BLUE, 40_990, percent = 48),
        )

        val chart = membersChart(members, teamDistanceM = 84_960)

        assertEquals("84.9", chart.totalKm)
        assertEquals(listOf("43.9", "40.9"), chart.bars.map { it.km })
    }

    @Test fun `퍼센트는 다시 계산하지 않고 MemberUi percent를 쓴다`() {
        // 반올림하면 33·33·33(합 99)이지만 최대 나머지 방식 값 34·33·33을 그대로
        val members = listOf(
            MemberUi("a", "가", TeamColor.BLUE, 10_000, percent = 34),
            MemberUi("b", "나", TeamColor.NAVY, 10_000, percent = 33),
            MemberUi("c", "다", TeamColor.TEAL, 10_000, percent = 33),
        )

        val chart = membersChart(members, teamDistanceM = 30_000)

        assertEquals(listOf(34, 33, 33), chart.bars.map { it.percent })
    }

    @Test fun `나간 팀원 몫이 있으면 끝에 이전 팀원 막대, 퍼센트 합은 100`() {
        val members = listOf(
            MemberUi("u1", "민지", TeamColor.BLUE, 30_800, percent = 46),
            MemberUi("u2", "서준", TeamColor.NAVY, 20_000, percent = 30),
        )

        val chart = membersChart(members, teamDistanceM = 66_800)

        val former = chart.bars.last()
        assertNull(former.member)
        assertEquals(16_000L, former.distanceM)
        assertEquals("16.0", former.km)
        assertEquals(24, former.percent)
        assertEquals(100, chart.bars.sumOf { it.percent })
    }

    @Test fun `나간 팀원 몫이 없으면 이전 팀원 막대도 없다`() {
        val members = listOf(MemberUi("u1", "민지", TeamColor.BLUE, 66_800, percent = 100))

        val chart = membersChart(members, teamDistanceM = 66_800)

        assertEquals(1, chart.bars.size)
    }

    @Test fun `팀원이 없으면 합계만`() {
        val chart = membersChart(emptyList(), teamDistanceM = 0)

        assertEquals("0.0", chart.totalKm)
        assertEquals(emptyList<MembersChartBar>(), chart.bars)
    }
}
