package com.tourderun.core.data.record

import com.tourderun.domain.record.RunRecord
import com.tourderun.domain.record.RunTextError
import kotlinx.coroutines.flow.Flow

/**
 * 내 러닝 기록 조회·수정·삭제 (REC-02·03). 화면: S11 내 기록, S12 기록 상세(전병국).
 *
 * - 실패는 예외로 던지지 않는다. Flow는 오류가 나면 마지막 값을 유지한 채 끝나고, 쓰기는 [Result]로 돌려준다.
 * - 쓰기는 오프라인 대기 쓰기라 `await()`하지 않는다. 커밋을 시작하면 바로 성공을 돌려준다.
 */
interface RunRepository {
    /**
     * 내 기록 목록, `startedAt` 최신순으로 최대 [limit]개.
     * 화면은 [PAGE_SIZE]부터 시작해 끝까지 내리면 [PAGE_SIZE]씩 늘려 다시 구독한다.
     * 받은 개수가 [limit]보다 적으면 더 없는 것이다. 목록 항목의 `path`는 항상 빈 리스트다.
     */
    fun observeRuns(uid: String, limit: Long): Flow<List<RunRecord>>

    /** 기록 한 건(`path` 포함). 없거나 지워지면 null. */
    fun observeRun(runId: String): Flow<RunRecord?>

    /**
     * 제목·메모 수정. `checkRunText()`에 걸리면 [InvalidRunTextException]으로 실패하고 아무것도 쓰지 않는다.
     * 통과하면 앞뒤 공백을 지운 값으로 쓴다. 빈 메모는 Firestore에 `""`로 쓰고, 읽을 때는 null이다.
     */
    fun updateTitleMemo(runId: String, title: String, memo: String?): Result<Unit>

    /** `runs` 문서만 지운다. 팀 거리·주간 통계·기여는 빼지 않는다(M-01). */
    fun delete(runId: String): Result<Unit>

    companion object {
        /** 목록 한 번에 늘리는 개수 (REC-02). */
        const val PAGE_SIZE = 20L
    }
}

/** 제목·메모 검사 실패. [errors]로 칸마다 오류 문구를 고른다. */
class InvalidRunTextException(val errors: Set<RunTextError>) :
    IllegalArgumentException("제목·메모 검사 실패: $errors")
