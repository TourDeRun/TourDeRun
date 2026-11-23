package com.tourderun.core.data.session

import com.tourderun.core.data.firestore.WriteMode
import org.junit.Assert.assertEquals
import org.junit.Test

class NicknameUpdatePlanTest {

    @Test fun `팀이 있으면 users와 내 members 두 곳을 바꾼다`() {
        val plan = nicknameUpdatePlan(uid = "u1", teamId = "t1", nickname = "민찬")
        assertEquals(listOf("users/u1", "teams/t1/members/u1"), plan.map { it.path })
        plan.forEach {
            assertEquals(WriteMode.UPDATE, it.mode)
            assertEquals(mapOf("nickname" to "민찬"), it.fields)
        }
    }

    @Test fun `팀이 없으면 users만 바꾼다`() {
        val plan = nicknameUpdatePlan(uid = "u1", teamId = null, nickname = "민찬")
        assertEquals(listOf("users/u1"), plan.map { it.path })
    }
}
