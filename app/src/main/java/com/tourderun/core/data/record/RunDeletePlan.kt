package com.tourderun.core.data.record

import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.DocWrite
import com.tourderun.core.data.firestore.FieldOp
import com.tourderun.core.data.firestore.UserFields
import com.tourderun.core.data.firestore.WriteMode

/**
 * REC-05 기록 삭제 배치에 쓸 문서 목록(순서대로).
 *
 * ① `runs/{runId}` 삭제 ② 내 `users`의 누적 거리(`totalM`)에서 그 기록 거리만큼, 러닝 횟수(`runCount`)에서 1을 뺀다.
 * 그래서 S11 총계·프로필 숫자가 지운 뒤에도 남은 기록과 맞는다.
 * 팀 거리·기여 거리·주간 통계는 빼지 않는다(M-01: 팀 거리는 한 번 쌓이면 줄지 않는다).
 *
 * @param distanceM 지우는 기록의 저장 거리(`runs.distanceM`). 0보다 커야 한다
 */
fun runDeletePlan(runId: String, uid: String, distanceM: Long): List<DocWrite> {
    require(distanceM > 0) { "지우는 기록의 거리는 0보다 커야 한다: $distanceM" }
    return listOf(
        DocWrite(path = "${Collections.RUNS}/$runId", mode = WriteMode.DELETE, fields = emptyMap()),
        DocWrite(
            path = "${Collections.USERS}/$uid",
            mode = WriteMode.UPDATE,
            fields = mapOf(
                UserFields.TOTAL_M to FieldOp.Increment(-distanceM),
                UserFields.RUN_COUNT to FieldOp.Increment(-1),
            ),
        ),
    )
}
