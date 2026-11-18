package com.tourderun.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tourderun.domain.model.GpsPoint

/** RUN-06 원본 위치점. segment가 바뀌는 곳은 일시정지 공백이다. */
@Entity(tableName = "run_points", indices = [Index("runId")])
data class RunPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: String,
    val segment: Int,
    val lat: Double,
    val lng: Double,
    val timeMs: Long,
    val accuracyM: Float,
    val accepted: Boolean,
)

fun RunPointEntity.toGpsPoint() = GpsPoint(lat, lng, timeMs, accuracyM)

fun GpsPoint.toRunPointEntity(runId: String, segment: Int, accepted: Boolean) =
    RunPointEntity(runId = runId, segment = segment, lat = lat, lng = lng, timeMs = timeMs, accuracyM = accuracyM, accepted = accepted)
