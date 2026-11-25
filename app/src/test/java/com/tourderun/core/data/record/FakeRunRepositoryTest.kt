package com.tourderun.core.data.record

import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 가짜 기록 Repository의 [Loaded] 출력과 실패 시나리오(처음 한 번만 실패). */
class FakeRunRepositoryTest {

    private val samples = FakeRunRepository.SAMPLE_RUNS

    @Test fun `실패 없음 → 목록은 Ready, 최신순, 경로 비움`() = runBlocking {
        val loaded = FakeRunRepository(samples).observeRuns(UID, 3).first()

        val ready = loaded as Loaded.Ready
        assertEquals(samples.take(3).map { it.id }, ready.value.map { it.id })
        assertTrue(ready.value.all { it.path.isEmpty() })
        assertEquals(false, ready.isFromCache)
    }

    @Test fun `실패 시나리오 → 목록 첫 구독은 Failed 하나만 내고 끝난다`() = runBlocking {
        val repo = FakeRunRepository(samples, failure = DataError.Unavailable)

        assertEquals(listOf(Loaded.Failed(DataError.Unavailable)), repo.observeRuns(UID, 20).toList())
    }

    @Test fun `실패 시나리오 → 목록 다시 구독하면 Ready`() = runBlocking {
        val repo = FakeRunRepository(samples, failure = DataError.Unavailable)
        val flow = repo.observeRuns(UID, 20)

        flow.toList()
        val retry = flow.first()

        assertEquals(samples.size, (retry as Loaded.Ready).value.size)
    }

    @Test fun `실패 시나리오 → 상세 첫 구독은 Failed, 다시 구독하면 Ready`() = runBlocking {
        val repo = FakeRunRepository(samples, failure = DataError.PermissionDenied)
        val flow = repo.observeRun(samples.first().id)

        assertEquals(listOf(Loaded.Failed(DataError.PermissionDenied)), flow.toList())
        val retry = flow.first() as Loaded.Ready
        assertEquals(samples.first().id, retry.value?.id)
    }

    @Test fun `실패 시나리오 → 목록과 상세는 따로 한 번씩 실패한다`() = runBlocking {
        val repo = FakeRunRepository(samples, failure = DataError.Other)

        repo.observeRuns(UID, 20).toList()

        assertEquals(Loaded.Failed(DataError.Other), repo.observeRun(samples.first().id).first())
    }

    @Test fun `없는 기록 → Ready(null)`() = runBlocking {
        val loaded = FakeRunRepository(samples).observeRun("no-such-run").first()

        assertNull((loaded as Loaded.Ready).value)
    }

    @Test fun `삭제하면 상세가 Ready(null)로 바뀐다`() = runBlocking {
        val repo = FakeRunRepository(samples)
        val id = samples.first().id

        repo.delete(UID, id, samples.first().distanceM)

        assertNull((repo.observeRun(id).first() as Loaded.Ready).value)
    }

    private companion object {
        const val UID = "uid"
    }
}
