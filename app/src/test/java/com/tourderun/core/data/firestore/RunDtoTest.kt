package com.tourderun.core.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.tourderun.domain.model.LatLngValue
import com.tourderun.domain.record.RunRecord
import com.tourderun.domain.record.RunTextCheck
import com.tourderun.domain.record.defaultRunTitle
import com.tourderun.domain.stats.SEOUL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunDtoTest {
    // 2026-11-10 19:20 KST
    private val startMs = 1_794_306_000_000L
    private val endMs = startMs + 1_650_000L

    private fun ts(ms: Long) = Timestamp(ms / 1_000, ((ms % 1_000) * 1_000_000).toInt())

    private fun doc(
        title: Any? = "11월 10일 저녁 러닝",
        memo: Any? = "반포대교",
        path: Any? = listOf(GeoPoint(37.511, 126.995), GeoPoint(37.515, 127.005)),
    ): Map<String, Any?> = mapOf(
        RunFields.UID to "u1",
        RunFields.TEAM_ID to "t1",
        RunFields.STARTED_AT to ts(startMs),
        RunFields.ENDED_AT to ts(endMs),
        RunFields.MOVING_SEC to 1_560L,
        RunFields.DISTANCE_M to 5_230L,
        RunFields.PACE_SEC_PER_KM to 298L,
        RunFields.PERIOD_ID to "2026-W46",
        RunFields.TITLE to title,
        RunFields.MEMO to memo,
        RunFields.PATH to path,
    )

    @Test fun `정상 문서 → 모든 필드가 그대로`() {
        val record = runDtoOf("r1", doc()).toRunRecord()

        assertEquals(
            RunRecord(
                id = "r1",
                title = "11월 10일 저녁 러닝",
                memo = "반포대교",
                startedAtMs = startMs,
                endedAtMs = endMs,
                movingSec = 1_560,
                distanceM = 5_230,
                paceSecPerKm = 298,
                path = listOf(LatLngValue(37.511, 126.995), LatLngValue(37.515, 127.005)),
            ),
            record,
        )
    }

    @Test fun `path 필드 없음 → 빈 경로`() {
        val fields = doc() - RunFields.PATH

        assertEquals(emptyList<LatLngValue>(), runDtoOf("r1", fields).path)
    }

    @Test fun `path에 GeoPoint가 아닌 값 → 그 점만 빼고 읽는다`() {
        val dto = runDtoOf("r1", doc(path = listOf(GeoPoint(37.5, 127.0), "x", null, GeoPoint(37.6, 127.1))))

        assertEquals(listOf(LatLngValue(37.5, 127.0), LatLngValue(37.6, 127.1)), dto.path)
    }

    @Test fun `memo 빈 문자열 → null`() {
        assertNull(runDtoOf("r1", doc(memo = "")).memo)
        assertNull(runDtoOf("r1", doc(memo = "   ")).memo)
        assertNull(runDtoOf("r1", doc(memo = null)).memo)
    }

    @Test fun `점 5,000개 → 순서 그대로 5,000개`() {
        val points = List(5_000) { i -> LatLngValue(37.0 + i * 0.0001, 127.0 + i * 0.0001) }

        val dto = runDtoOf("r1", doc(path = points.toGeoPoints()))

        assertEquals(5_000, dto.path.size)
        assertEquals(points, dto.path)
    }

    @Test fun `필드가 모두 없음 → 0과 기본 제목, 끝 시각은 시작 시각`() {
        val dto = runDtoOf("r1", emptyMap())

        assertEquals(RunDto("r1", defaultRunTitle(0, SEOUL), null, 0, 0, 0, 0, 0, emptyList()), dto)
    }

    @Test fun `제목이 공백뿐 → 시작 시각 기본 제목`() {
        assertEquals(defaultRunTitle(startMs, SEOUL), runDtoOf("r1", doc(title = "  ")).title)
    }

    @Test fun `숫자가 Double로 와도 Long으로`() {
        val dto = runDtoOf("r1", doc() + (RunFields.DISTANCE_M to 5_230.0))

        assertEquals(5_230L, dto.distanceM)
    }

    @Test fun `목록 변환 → 문서 순서 그대로, path는 비운다`() {
        val list = runListOf(listOf("r2" to doc(title = "둘"), "r1" to doc(title = "하나")))

        assertEquals(listOf("r2", "r1"), list.map { it.id })
        assertEquals(listOf("둘", "하나"), list.map { it.title })
        assertEquals(listOf(emptyList<LatLngValue>(), emptyList()), list.map { it.path })
    }

    @Test fun `상세 변환 → path를 읽는다`() {
        assertEquals(2, runDtoOf("r1", doc(), includePath = true).path.size)
    }

    @Test fun `제목·메모 수정 필드 → title과 memo 두 개만, 빈 메모는 빈 문자열`() {
        val fields = titleMemoFields(RunTextCheck.Valid(title = "아침 러닝", memo = null))

        assertEquals(mapOf(RunFields.TITLE to "아침 러닝", RunFields.MEMO to ""), fields)
    }

    @Test fun `빈 메모를 쓰고 다시 읽으면 null`() {
        val written = titleMemoFields(RunTextCheck.Valid(title = "아침 러닝", memo = null))

        assertNull(runDtoOf("r1", doc() + written).memo)
    }
}
