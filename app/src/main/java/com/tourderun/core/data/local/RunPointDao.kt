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

    @Query("SELECT * FROM run_points WHERE runId = :runId ORDER BY id ASC")
    fun observePointsForRun(runId: String): Flow<List<RunPointEntity>>

    @Query("DELETE FROM run_points WHERE runId = :runId")
    suspend fun deleteForRun(runId: String)
}
