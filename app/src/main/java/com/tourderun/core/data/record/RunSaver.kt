package com.tourderun.core.data.record

import com.tourderun.core.data.session.SessionState
import com.tourderun.core.data.session.teamId
import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.record.SaveOutcome

/**
 * 러닝 종료 결과 저장 (REC-01). 배치 한 번으로 `runs`·`teams`·내 `members`·이번 주 `teamStats`·내 `users`를 쓴다.
 * 문서별 필드는 `feature/record/data/RunSavePlan.kt`의 `runSavePlan()` 참고.
 */
interface RunSaver {
    /** 새 러닝 ID. 러닝을 시작할 때 기기에서 미리 만든다(`collection("runs").document().id`). */
    fun newRunId(): String

    /**
     * 커밋을 **시작만** 하고 바로 돌아온다(오프라인 대기 쓰기라 `await()`하지 않는다).
     * 실패는 배치를 만드는 단계의 오류만 [Result]로 돌려준다. 서버 반영 실패는 [onServerRejected]로 알린다.
     * 거리를 반올림해 0m면 저장하지 않고 실패를 돌려준다(페이스를 계산할 수 없음, M-05).
     *
     * @param team 저장하는 순간의 내 팀(`MyTeamSource`의 값). `standing.distanceM`이 저장 전 팀 거리(캐시)다.
     *   null이면 팀 없이 `runs`·`users`만 쓴다(팀이 없는 사용자)
     * @param title 사용자가 비워 두면 `defaultRunTitle()` 값을 넘긴다
     * @param onServerConfirmed 서버가 배치를 받아들였을 때 한 번 부른다(#23, 기기에 남긴 위치점·진행 상태 정리용).
     *   오프라인이면 온라인이 되어 반영된 뒤에 불리고, 그 전에 앱이 꺼지면 불리지 않는다. 서버가 거부하면 부르지 않는다
     * @param onServerRejected 서버가 배치를 받아들이지 않았을 때(보안 규칙 거부 등). 기기 캐시의 쓰기는 되돌려진다.
     *   [onServerConfirmed]와 둘 중 하나만 한 번 불린다. 오프라인은 실패가 아니라 대기라 부르지 않는다
     * @return S10 결과 화면 값. 서버를 기다리지 않고 기기에서 계산한다(`before` = 캐시된 팀 거리, `after = before + 이번 거리`).
     *   팀이 없으면 null
     */
    fun save(
        result: RunResult,
        uid: String,
        team: MyTeam?,
        title: String,
        onServerConfirmed: () -> Unit = {},
        onServerRejected: () -> Unit = {},
    ): Result<SaveOutcome?>

    /**
     * `runs/{runId}`가 기기 캐시에 있으면 true. 저장 배치를 시작한 뒤(서버 반영 전이어도) 앱이 꺼진 러닝을 찾을 때 쓴다.
     * 캐시에 없거나 읽기에 실패하면 false.
     */
    suspend fun isSavedOnDevice(runId: String): Boolean

    /**
     * 기기에 대기 중인 쓰기가 모두 서버에 반영될 때까지 기다린다(`waitForPendingWrites`). 오프라인이면 온라인이 될 때까지 기다린다.
     *
     * @return 반영을 확인했으면 true, 실패했으면 false
     */
    suspend fun awaitPendingWrites(): Boolean
}

/**
 * 저장에 넘길 팀. 세션에 팀이 없으면(`NoTeam` 등) null이라 `runs`·`users` 두 문서만 쓴다.
 * 팀이 있고 [myTeam](`MyTeamSource`의 값, 세션 teamId로 읽은 `teams` 문서)이 세션의 팀과 같은 팀이면 [myTeam]을 쓴다.
 * 둘이 다르면(팀을 막 바꿔 팀 문서를 아직 못 읽은 순간 등) 다른 팀 문서에 거리를 더하게 되므로 null로 둔다.
 * 이때 `runs.teamId`(null)가 `users.teamId`와 달라 보안 규칙이 배치를 거부한다(거부된 러닝은 복구 안내로 남는다).
 */
fun saveTeamOf(session: SessionState, myTeam: MyTeam?): MyTeam? {
    val teamId = session.teamId ?: return null
    return myTeam?.takeIf { it.standing.teamId == teamId }
}
