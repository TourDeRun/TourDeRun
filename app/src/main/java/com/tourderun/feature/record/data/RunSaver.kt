package com.tourderun.feature.record.data

import com.tourderun.core.data.team.MyTeam
import com.tourderun.domain.model.RunResult
import com.tourderun.domain.record.SaveOutcome

/**
 * 러닝 종료 결과 저장 (REC-01). 배치 한 번으로 `runs`·`teams`·내 `members`·이번 주 `teamStats`·내 `users`를 쓴다.
 * 문서별 필드는 [runSavePlan] 참고.
 */
interface RunSaver {
    /** 새 러닝 ID. 러닝을 시작할 때 기기에서 미리 만든다(`collection("runs").document().id`). */
    fun newRunId(): String

    /**
     * 커밋을 **시작만** 하고 바로 돌아온다(오프라인 대기 쓰기라 `await()`하지 않는다).
     * 실패는 배치를 만드는 단계의 오류만 [Result]로 돌려준다. 서버 반영 실패는 로그로만 남는다.
     * 거리를 반올림해 0m면 저장하지 않고 실패를 돌려준다(페이스를 계산할 수 없음, M-05).
     *
     * @param team 저장하는 순간의 내 팀(`MyTeamSource`의 값). `standing.distanceM`이 저장 전 팀 거리(캐시)다.
     *   null이면 팀 없이 `runs`·`users`만 쓴다(과도기)
     * @param title 사용자가 비워 두면 `defaultRunTitle()` 값을 넘긴다
     * @return S10 결과 화면 값. 서버를 기다리지 않고 기기에서 계산한다(`before` = 캐시된 팀 거리, `after = before + 이번 거리`).
     *   팀이 없으면 null
     */
    fun save(result: RunResult, uid: String, team: MyTeam?, title: String): Result<SaveOutcome?>
}
