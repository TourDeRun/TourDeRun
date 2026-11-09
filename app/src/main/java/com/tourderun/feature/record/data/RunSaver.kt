package com.tourderun.feature.record.data

import com.tourderun.domain.model.RunResult

/**
 * 러닝 종료 결과 저장 (REC-01).
 * 1단계(#4): `runs/{runId}` 문서만. 팀·멤버·주간 통계·사용자 반영은 #10에서 같은 배치에 추가한다.
 */
interface RunSaver {
    /** 새 러닝 ID. 러닝을 시작할 때 기기에서 미리 만든다(`collection("runs").document().id`). */
    fun newRunId(): String

    /**
     * 커밋을 **시작만** 하고 바로 돌아온다(오프라인 대기 쓰기라 `await()`하지 않는다).
     * 실패는 배치를 만드는 단계의 오류만 [Result]로 돌려준다. 서버 반영 실패는 로그로만 남는다.
     * 거리를 반올림해 0m면 저장하지 않고 실패를 돌려준다(페이스를 계산할 수 없음, M-05).
     *
     * @param teamId 팀이 없으면 null(문서에 null을 명시해서 쓴다)
     * @param title 사용자가 비워 두면 `defaultRunTitle()` 값을 넘긴다
     */
    fun save(result: RunResult, uid: String, teamId: String?, title: String): Result<Unit>
}
