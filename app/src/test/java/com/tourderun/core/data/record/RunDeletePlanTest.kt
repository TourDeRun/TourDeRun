package com.tourderun.core.data.record

import com.tourderun.core.data.firestore.FieldOp
import com.tourderun.core.data.firestore.WriteMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RunDeletePlanTest {

    @Test fun `기록 문서를 지우고 내 누적 거리와 횟수를 그만큼 뺀다`() {
        val plan = runDeletePlan(runId = "r1", uid = "u1", distanceM = 5_230)

        assertEquals(listOf("runs/r1", "users/u1"), plan.map { it.path })
        assertEquals(listOf(WriteMode.DELETE, WriteMode.UPDATE), plan.map { it.mode })
        assertEquals(
            mapOf("totalM" to FieldOp.Increment(-5_230), "runCount" to FieldOp.Increment(-1)),
            plan[1].fields,
        )
    }

    @Test fun `팀 문서는 건드리지 않는다 - 팀 거리는 줄지 않는다`() {
        val plan = runDeletePlan(runId = "r1", uid = "u1", distanceM = 5_230)

        assertEquals(emptyList<String>(), plan.map { it.path }.filter { it.startsWith("teams") || it.startsWith("teamStats") })
    }

    @Test fun `거리가 0 이하면 계획을 만들지 않는다`() {
        assertThrows(IllegalArgumentException::class.java) { runDeletePlan("r1", "u1", distanceM = 0) }
    }
}
