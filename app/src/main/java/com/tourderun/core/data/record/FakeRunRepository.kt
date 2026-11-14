package com.tourderun.core.data.record

import com.tourderun.domain.journey.haversineM
import com.tourderun.domain.model.LatLngValue
import com.tourderun.domain.record.RunRecord
import com.tourderun.domain.record.RunTextCheck
import com.tourderun.domain.record.checkRunText
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * 가짜 [RunRepository]. Firebase를 쓰지 않아 로그인·`google-services.json` 없이 S11·S12를 붙여 볼 수 있다.
 * 기본은 한강 러닝 5개, `FakeRunRepository(emptyList())`는 빈 목록. uid는 보지 않는다.
 * 수정·삭제는 메모리에만 반영되고 앱을 다시 켜면 처음으로 돌아간다.
 */
@Singleton
class FakeRunRepository(initial: List<RunRecord>) : RunRepository {

    @Inject constructor() : this(SAMPLE_RUNS)

    private val runs = MutableStateFlow(initial)

    override fun observeRuns(uid: String, limit: Long): Flow<List<RunRecord>> =
        runs.map { list ->
            list.sortedByDescending { it.startedAtMs }
                .take(limit.coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                .map { it.copy(path = emptyList()) }
        }

    override fun observeRun(runId: String): Flow<RunRecord?> =
        runs.map { list -> list.find { it.id == runId } }.distinctUntilChanged()

    override fun updateTitleMemo(runId: String, title: String, memo: String?): Result<Unit> =
        when (val check = checkRunText(title, memo)) {
            is RunTextCheck.Invalid -> Result.failure(InvalidRunTextException(check.errors))
            is RunTextCheck.Valid -> {
                runs.update { list ->
                    list.map { if (it.id == runId) it.copy(title = check.title, memo = check.memo) else it }
                }
                Result.success(Unit)
            }
        }

    override fun delete(runId: String): Result<Unit> {
        runs.update { list -> list.filterNot { it.id == runId } }
        return Result.success(Unit)
    }

    companion object {
        // TODO(REC-02): PR #15(weekId) 머지 후 :domain의 SEOUL로 바꾼다.
        private val ZONE = ZoneId.of("Asia/Seoul")

        /** 한강공원 러닝 5개(최신순). Preview에서 바로 쓸 수 있다. */
        val SAMPLE_RUNS: List<RunRecord> = listOf(
            fakeRun(
                id = "fake-run-5",
                title = "11월 10일 저녁 러닝",
                memo = "반포대교 무지개분수는 겨울이라 꺼져 있었다",
                start = LocalDateTime.of(2026, 11, 10, 19, 20),
                movingSec = 1_560,
                pausedSec = 90,
                waypoints = listOf(
                    37.5110 to 126.9950, 37.5150 to 127.0050, 37.5195 to 127.0130,
                    37.5240 to 127.0200, 37.5195 to 127.0130, 37.5150 to 127.0050,
                ),
            ),
            fakeRun(
                id = "fake-run-4",
                title = "여의도 아침 한 바퀴",
                memo = null,
                start = LocalDateTime.of(2026, 11, 8, 7, 5),
                movingSec = 1_380,
                pausedSec = 0,
                waypoints = listOf(
                    37.5260 to 126.9230, 37.5285 to 126.9330, 37.5300 to 126.9400,
                    37.5275 to 126.9450, 37.5240 to 126.9380, 37.5225 to 126.9280,
                    37.5260 to 126.9230,
                ),
            ),
            fakeRun(
                id = "fake-run-3",
                title = "11월 6일 밤 러닝",
                memo = "망원에서 상수까지, 바람이 셌다",
                start = LocalDateTime.of(2026, 11, 6, 22, 15),
                movingSec = 1_140,
                pausedSec = 45,
                waypoints = listOf(
                    37.5555 to 126.8950, 37.5520 to 126.9050, 37.5480 to 126.9150,
                    37.5440 to 126.9230,
                ),
            ),
            fakeRun(
                id = "fake-run-2",
                title = "11월 4일 점심 러닝",
                memo = null,
                start = LocalDateTime.of(2026, 11, 4, 12, 10),
                movingSec = 960,
                pausedSec = 30,
                waypoints = listOf(
                    37.5300 to 127.0600, 37.5290 to 127.0680, 37.5270 to 127.0760,
                    37.5230 to 127.0830,
                ),
            ),
            fakeRun(
                id = "fake-run-1",
                title = "잠실 첫 러닝",
                memo = "팀 만들고 처음 달린 날",
                start = LocalDateTime.of(2026, 11, 2, 18, 40),
                movingSec = 1_200,
                pausedSec = 120,
                waypoints = listOf(
                    37.5190 to 127.0820, 37.5170 to 127.0900, 37.5160 to 127.0990,
                    37.5175 to 127.1060,
                ),
            ),
        )

        /** 거리는 경로로 계산(`haversineM`), 페이스는 저장 규칙과 같이 반올림한 거리로 계산한다. */
        private fun fakeRun(
            id: String,
            title: String,
            memo: String?,
            start: LocalDateTime,
            movingSec: Long,
            pausedSec: Long,
            waypoints: List<Pair<Double, Double>>,
        ): RunRecord {
            val path = densify(waypoints.map { (lat, lng) -> LatLngValue(lat, lng) })
            val distanceM = path.zipWithNext { a, b -> haversineM(a.lat, a.lng, b.lat, b.lng) }
                .sum()
                .roundToLong()
            val startedAtMs = start.atZone(ZONE).toInstant().toEpochMilli()
            return RunRecord(
                id = id,
                title = title,
                memo = memo,
                startedAtMs = startedAtMs,
                endedAtMs = startedAtMs + (movingSec + pausedSec) * 1_000L,
                movingSec = movingSec,
                distanceM = distanceM,
                paceSecPerKm = (movingSec * 1_000.0 / distanceM).roundToLong(),
                path = path,
            )
        }

        /** 지도에 매끄럽게 보이도록 구간마다 점을 [steps]개로 나눈다. */
        private fun densify(points: List<LatLngValue>, steps: Int = 8): List<LatLngValue> =
            points.zipWithNext { a, b ->
                (0 until steps).map { i ->
                    val t = i.toDouble() / steps
                    LatLngValue(a.lat + (b.lat - a.lat) * t, a.lng + (b.lng - a.lng) * t)
                }
            }.flatten() + points.last()
    }
}
