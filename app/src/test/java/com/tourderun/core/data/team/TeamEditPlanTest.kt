package com.tourderun.core.data.team

import com.tourderun.core.data.firestore.WriteMode
import org.junit.Assert.assertEquals
import org.junit.Test

class TeamEditPlanTest {

    @Test fun `팀 문서의 이름·아이콘·색 세 필드만 바꾼다`() {
        val plan = teamEditPlan("t1", "새이름", icon = 3, color = 5)
        assertEquals(listOf("teams/t1"), plan.map { it.path })
        assertEquals(WriteMode.UPDATE, plan.single().mode)
        assertEquals(mapOf("name" to "새이름", "icon" to 3, "color" to 5), plan.single().fields)
    }
}
