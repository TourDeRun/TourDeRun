package com.tourderun.feature.record.ui

import com.tourderun.domain.record.RunRecord
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordMapperTest {
    private val seoul = ZoneId.of("Asia/Seoul")

    private fun ms(year: Int, month: Int, day: Int, hour: Int, minute: Int, zone: ZoneId = seoul) =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun run(
        id: String = "r1",
        title: String = "저녁 러닝",
        memo: String? = null,
        startedAtMs: Long = ms(2026, 11, 6, 19, 2),
        distanceM: Long = 3_240,
    ) = RunRecord(id, title, memo, startedAtMs, startedAtMs + 1_200_000, 1_122, distanceM, 346, emptyList())

    @Test fun `목록 한 줄의 날짜는 한국 시간 기준`() {
        // UTC로는 11월 6일 15:30이지만 한국은 11월 7일 00:30
        val row = run(startedAtMs = ms(2026, 11, 6, 15, 30, ZoneId.of("UTC"))).toRowUi(seoul)
        assertEquals(11, row.month)
        assertEquals(7, row.day)
    }

    @Test fun `받은 개수가 limit과 같으면 더 불러올 수 있다`() {
        val runs = List(20) { run(id = "r$it") }
        assertTrue(recordsUiState(runs, limit = 20).canLoadMore)
    }

    @Test fun `받은 개수가 limit보다 적으면 끝`() {
        val state = recordsUiState(List(5) { run(id = "r$it", distanceM = 1_000) }, limit = 20)
        assertFalse(state.canLoadMore)
        assertEquals(5, state.totalRuns)
        assertEquals(5_000L, state.totalDistanceM)
    }

    @Test fun `기록이 없으면 빈 목록이고 불러오는 중이 아니다`() {
        val state = recordsUiState(emptyList(), limit = 20)
        assertTrue(state.runs.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.canLoadMore)
    }

    @Test fun `시작 시각은 연 월 일 요일 시분`() {
        assertEquals("2026. 11. 6 (금) 19:02", startedAtText(ms(2026, 11, 6, 19, 2), seoul))
        assertEquals("2026. 1. 4 (일) 07:05", startedAtText(ms(2026, 1, 4, 7, 5), seoul))
    }

    @Test fun `손대지 않으면 저장된 값을 보이고 저장할 수 없다`() {
        val ui = recordDetailUiState(run(memo = null), null, null, showDeleteConfirm = false, zone = seoul)
        assertEquals("저녁 러닝", ui.title)
        assertEquals("", ui.memo)
        assertFalse(ui.canSave)
    }

    @Test fun `제목을 바꾸면 저장할 수 있다`() {
        assertTrue(recordDetailUiState(run(), "한강 한 바퀴", null, false, seoul).canSave)
    }

    @Test fun `메모만 바꿔도 저장할 수 있다`() {
        assertTrue(recordDetailUiState(run(memo = null), null, "시원했다", false, seoul).canSave)
    }

    @Test fun `제목을 비우면 저장할 수 없다`() {
        assertFalse(recordDetailUiState(run(), "   ", null, false, seoul).canSave)
    }

    @Test fun `앞뒤 공백만 다르면 바뀐 것이 아니다`() {
        assertFalse(recordDetailUiState(run(memo = null), " 저녁 러닝 ", "  ", false, seoul).canSave)
    }

    @Test fun `고쳤다가 원래 값으로 되돌리면 저장할 수 없다`() {
        assertFalse(recordDetailUiState(run(memo = "메모"), "저녁 러닝", "메모", false, seoul).canSave)
    }
}
