package com.tourderun.feature.record.data

import com.tourderun.core.data.firestore.Collections
import com.tourderun.core.data.firestore.MemberFields
import com.tourderun.core.data.firestore.RunFields
import com.tourderun.core.data.firestore.TeamFields
import com.tourderun.core.data.firestore.TeamStatFields
import com.tourderun.core.data.firestore.UserFields
import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.model.LatLngValue
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.record.thinEvenly
import com.tourderun.domain.run.averagePaceSecPerKm
import com.tourderun.domain.stats.weekId
import kotlin.math.roundToLong

/** 배치 안 문서 하나를 쓰는 방식. */
enum class WriteMode {
    /** 새 문서 생성(`set`). */
    SET,

    /** 있는 문서의 필드만 바꾼다(`update`). 문서가 없으면 배치 전체가 실패한다. */
    UPDATE,

    /** 없으면 만들고 있으면 합친다(`set(merge)`). 중첩 Map은 안쪽 키만 합쳐진다. */
    SET_MERGE,
}

/**
 * Firebase 타입을 쓰지 않는 필드 값. `FirestoreRunSaver`가 Firestore 값으로 바꾼다.
 * 그래서 배치 내용을 Firestore 없이 테스트할 수 있다.
 */
sealed interface FieldOp {
    /** `FieldValue.increment(by)` */
    data class Increment(val by: Long) : FieldOp

    /** `FieldValue.serverTimestamp()` */
    data object ServerTime : FieldOp

    /** `Timestamp` (epoch ms) */
    data class Time(val epochMs: Long) : FieldOp

    /** `List<GeoPoint>` */
    data class Points(val points: List<LatLngValue>) : FieldOp
}

/**
 * 배치에 들어갈 문서 하나.
 *
 * @property path `runs/{runId}`처럼 컬렉션부터 시작하는 문서 경로
 * @property fields 키는 최상위 필드 이름(점 표기 없음). 값은 일반 값·[FieldOp]·중첩 Map
 */
data class DocWrite(
    val path: String,
    val mode: WriteMode,
    val fields: Map<String, Any?>,
)

/** 저장할 거리(정수 m). 측정 중 Double을 저장 직전에 한 번만 반올림한다. 5개 문서가 모두 이 값을 쓴다. */
fun savedDistanceM(result: RunResult): Long = result.distanceM.roundToLong()

/**
 * REC-01 배치 한 번에 쓸 문서 목록(순서대로).
 *
 * 팀 있음: ① `runs` 생성 ② `teams` ③ 내 `members` ④ 이번 주 `teamStats` ⑤ 내 `users`.
 * 팀 없음: ①과 ⑤만. 보안 규칙은 거리 증가를 같은 배치의 새 러닝과 `lastRunId`로 대조하므로 증가 쓰기에는 항상 `lastRunId`를 함께 쓴다.
 * 주간 문서는 러닝 **시작 시각**의 주(`weekId`)라서 자정·주 경계를 넘긴 러닝도 시작한 주에 들어간다.
 *
 * TODO(REC-01): 팀 없이 저장하는 경우는 팀이 있어야 러닝할 수 있게 되면(TeamRepository 연결 후) 막는다.
 *
 * @param distanceM [savedDistanceM] 값. 0보다 커야 한다
 * @param team 저장하는 순간의 내 팀. 없으면 null
 */
fun runSavePlan(
    result: RunResult,
    distanceM: Long,
    uid: String,
    team: MyTeam?,
    title: String,
): List<DocWrite> {
    require(distanceM > 0) { "저장 거리는 0보다 커야 한다: $distanceM" }
    val runId = result.runId
    val periodId = weekId(result.startedAtMs)

    val run = DocWrite(
        path = "${Collections.RUNS}/$runId",
        mode = WriteMode.SET,
        fields = mapOf(
            RunFields.UID to uid,
            RunFields.TEAM_ID to team?.standing?.teamId,
            RunFields.STARTED_AT to FieldOp.Time(result.startedAtMs),
            RunFields.ENDED_AT to FieldOp.Time(result.endedAtMs),
            RunFields.MOVING_SEC to result.movingSec,
            RunFields.DISTANCE_M to distanceM,
            RunFields.PACE_SEC_PER_KM to savedPaceSecPerKm(result.movingSec, distanceM),
            RunFields.PERIOD_ID to periodId,
            RunFields.TITLE to title,
            RunFields.MEMO to null,
            RunFields.PATH to FieldOp.Points(thinEvenly(result.acceptedPoints).map { LatLngValue(it.lat, it.lng) }),
        ),
    )
    val user = DocWrite(
        path = "${Collections.USERS}/$uid",
        mode = WriteMode.UPDATE,
        fields = mapOf(
            UserFields.TOTAL_M to FieldOp.Increment(distanceM),
            UserFields.RUN_COUNT to FieldOp.Increment(1),
        ),
    )
    if (team == null) return listOf(run, user)
    val teamId = team.standing.teamId

    val teamDoc = DocWrite(
        path = "${Collections.TEAMS}/$teamId",
        mode = WriteMode.UPDATE,
        fields = mapOf(
            TeamFields.DISTANCE_M to FieldOp.Increment(distanceM),
            TeamFields.REACHED_AT to FieldOp.ServerTime,
            TeamFields.LAST_RUN_ID to runId,
        ),
    )
    val member = DocWrite(
        path = "${Collections.TEAMS}/$teamId/${Collections.MEMBERS}/$uid",
        mode = WriteMode.UPDATE,
        fields = mapOf(
            MemberFields.CONTRIBUTION_M to FieldOp.Increment(distanceM),
            MemberFields.LAST_RUN_ID to runId,
        ),
    )
    val weekly = DocWrite(
        path = "${Collections.TEAM_STATS}/${TeamStatFields.docId(teamId, periodId)}",
        mode = WriteMode.SET_MERGE,
        fields = mapOf(
            TeamStatFields.DISTANCE_M to FieldOp.Increment(distanceM),
            TeamStatFields.RUN_COUNT to FieldOp.Increment(1),
            // set(merge)에서 "memberM.uid" 키는 점이 들어간 필드 이름 그대로 저장된다. 중첩 Map으로 써야 memberM 안에 합쳐진다.
            TeamStatFields.MEMBER_M to mapOf(uid to FieldOp.Increment(distanceM)),
            TeamStatFields.LAST_RUN_ID to runId,
            TeamStatFields.TEAM_ID to teamId,
            TeamStatFields.SEASON to team.season,
            TeamStatFields.PERIOD_ID to periodId,
        ),
    )
    return listOf(run, teamDoc, member, weekly, user)
}

/**
 * 저장할 평균 페이스(초/km). 화면과 같은 [averagePaceSecPerKm]을 쓰고, 거리는 다른 문서와 같은 저장 거리(반올림한 정수 m)를 쓴다.
 * 거리 0이면 페이스가 없으므로(null) 예외를 던지고, [FirestoreRunSaver]가 실패 결과로 돌려준다.
 */
private fun savedPaceSecPerKm(movingSec: Long, distanceM: Long): Long =
    requireNotNull(averagePaceSecPerKm(movingSec, distanceM.toDouble())) { "거리 0m는 페이스를 계산할 수 없다" }.toLong()
