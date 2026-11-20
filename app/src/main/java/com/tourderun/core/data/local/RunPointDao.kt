package com.tourderun.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RunPointDao {
    @Insert suspend fun insert(point: RunPointEntity): Long

    @Query("SELECT * FROM run_points WHERE runId = :runId ORDER BY id ASC")
    suspend fun pointsForRun(runId: String): List<RunPointEntity>

    /** 거리에 채택된 위치점만 기록 순서대로 조회한다. */
    @Query("SELECT * FROM run_points WHERE runId = :runId AND accepted = 1 ORDER BY id ASC")
    suspend fun acceptedPointsForRun(runId: String): List<RunPointEntity>

    /** 관측값이 바뀔 때마다 전체 경로를 방출한다. 지도 소비자는 표시 주기를 조절한다. */
    @Query("SELECT * FROM run_points WHERE runId = :runId ORDER BY id ASC")
    fun observePointsForRun(runId: String): Flow<List<RunPointEntity>>

    /** 서버 저장 후 정리할 때는 서버 반영 성공을 확인한 뒤 호출한다. */
    @Query("DELETE FROM run_points WHERE runId = :runId")
    suspend fun deleteForRun(runId: String)

    /** [runIds]에 없는 러닝의 위치점을 지운다. 진행 중·저장 대기 어디에도 없는 러닝의 점 정리용(#99). 빈 목록이면 [deleteAll]. */
    @Query("DELETE FROM run_points WHERE runId NOT IN (:runIds)")
    suspend fun deleteExcept(runIds: List<String>)

    /** 모든 위치점을 지운다. 진행 중·저장 대기 러닝이 하나도 없을 때 정리용(#99). */
    @Query("DELETE FROM run_points")
    suspend fun deleteAll()
}
