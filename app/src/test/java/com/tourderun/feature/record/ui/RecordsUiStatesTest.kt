package com.tourderun.feature.record.ui

import com.tourderun.core.data.DataError
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.record.FakeRunRepository
import com.tourderun.core.data.record.RunRepository
import com.tourderun.domain.record.RunRecord
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** S11: 로그인 uid에 따라 기록 목록을 구독한다. */
class RecordsUiStatesTest {

    private val samples = FakeRunRepository.SAMPLE_RUNS

    /** 부른 (uid, limit)을 기록하고 uid별 목록을 돌려주는 가짜 조회. */
    private class Recorder(private val byUid: Map<String, Loaded<List<RunRecord>>>) {
        val calls = mutableListOf<Pair<String, Long>>()
        fun observe(uid: String, limit: Long): Flow<Loaded<List<RunRecord>>> {
            calls += uid to limit
            return flowOf(requireNotNull(byUid[uid]) { "모르는 uid: $uid" })
        }
    }

    /** [uids]·[limits]를 바꾸는 [block]을 실행하는 동안 나온 상태를 모은다. */
    private fun collect(
        uids: MutableStateFlow<String?>,
        limits: MutableStateFlow<Long>,
        recorder: Recorder,
        block: suspend () -> Unit = {},
    ): List<RecordsUiState> = runBlocking {
        val states = mutableListOf<RecordsUiState>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            recordsUiStates(
                uids = uids,
                limits = limits,
                observeRuns = recorder::observe,
                onUidChanged = { limits.value = RunRepository.PAGE_SIZE },
            ).toList(states)
        }
        settle()
        block()
        settle()
        job.cancel()
        states
    }

    /** flatMapLatest 안쪽 채널까지 값이 다 흘러가도록 한 스레드에서 여러 번 양보한다. */
    private suspend fun settle() = repeat(SETTLE_YIELDS) { yield() }

    @Test fun `uid null → 구독하지 않고 빈 목록`() {
        val recorder = Recorder(emptyMap())

        val states = collect(MutableStateFlow(null), MutableStateFlow(RunRepository.PAGE_SIZE), recorder)

        assertEquals(emptyList<Pair<String, Long>>(), recorder.calls)
        val last = states.last()
        assertEquals(false, last.isLoading)
        assertTrue(last.runs.isEmpty())
        assertEquals(false, last.canLoadMore)
    }

    @Test fun `uid가 있으면 그 uid로 20개 구독`() {
        val recorder = Recorder(mapOf("a" to Loaded.Ready(samples, isFromCache = false)))

        val states = collect(MutableStateFlow("a"), MutableStateFlow(RunRepository.PAGE_SIZE), recorder)

        assertEquals(listOf("a" to 20L), recorder.calls)
        assertEquals(samples.map { it.id }, states.last().runs.map { it.runId })
    }

    @Test fun `uid가 바뀌면 새 uid로 다시 구독하고 그 사이 불러오는 중`() {
        val recorder = Recorder(
            mapOf(
                "a" to Loaded.Ready(samples.take(2), isFromCache = false),
                "b" to Loaded.Ready(samples.take(1), isFromCache = false),
            ),
        )
        val uids = MutableStateFlow<String?>("a")

        val states = collect(uids, MutableStateFlow(RunRepository.PAGE_SIZE), recorder) { uids.value = "b" }

        assertEquals(listOf("a" to 20L, "b" to 20L), recorder.calls)
        val afterA = states.indexOfLast { it.runs.size == 2 }
        assertEquals(RecordsUiState.Loading, states[afterA + 1])
        assertEquals(listOf(samples.first().id), states.last().runs.map { it.runId })
    }

    @Test fun `로그아웃하면 빈 목록`() {
        val recorder = Recorder(mapOf("a" to Loaded.Ready(samples, isFromCache = false)))
        val uids = MutableStateFlow<String?>("a")

        val states = collect(uids, MutableStateFlow(RunRepository.PAGE_SIZE), recorder) { uids.value = null }

        assertTrue(states.last().runs.isEmpty())
        assertEquals(false, states.last().isLoading)
    }

    @Test fun `같은 uid에서 개수만 늘면 불러오는 중 없이 다시 구독`() {
        val recorder = Recorder(mapOf("a" to Loaded.Ready(samples, isFromCache = false)))
        val limits = MutableStateFlow(RunRepository.PAGE_SIZE)

        val states = collect(MutableStateFlow("a"), limits, recorder) { limits.value = 40L }

        assertEquals(listOf("a" to 20L, "a" to 40L), recorder.calls)
        assertEquals(1, states.count { it.isLoading })
    }

    @Test fun `uid가 바뀌면 20개부터 다시 구독`() {
        val recorder = Recorder(
            mapOf(
                "a" to Loaded.Ready(samples, isFromCache = false),
                "b" to Loaded.Ready(samples.take(1), isFromCache = false),
            ),
        )
        val uids = MutableStateFlow<String?>("a")
        val limits = MutableStateFlow(RunRepository.PAGE_SIZE)

        collect(uids, limits, recorder) {
            limits.value = 60L
            settle()
            uids.value = "b"
        }

        assertEquals(listOf("a" to 20L, "a" to 60L, "b" to 20L), recorder.calls)
        assertEquals(RunRepository.PAGE_SIZE, limits.value)
    }

    @Test fun `첫 목록 전 실패 → 불러오는 중을 끝내고 실패 표시`() {
        val recorder = Recorder(mapOf("a" to Loaded.Failed(DataError.PermissionDenied)))

        val states = collect(MutableStateFlow("a"), MutableStateFlow(RunRepository.PAGE_SIZE), recorder)

        assertEquals(RecordsUiState.Loading, states.first())
        val last = states.last()
        assertEquals(DataError.PermissionDenied, last.loadError)
        assertEquals(false, last.isLoading)
        assertTrue(last.runs.isEmpty())
        assertEquals(false, last.canLoadMore)
    }

    @Test fun `더 불러오기 실패 → 받아 둔 목록은 두고 실패 표시, 더 불러오기 멈춤`() {
        val results = mutableMapOf<Long, Loaded<List<RunRecord>>>(
            RunRepository.PAGE_SIZE to Loaded.Ready(samples, isFromCache = false),
            40L to Loaded.Failed(DataError.Unavailable),
        )
        val limits = MutableStateFlow(RunRepository.PAGE_SIZE)

        val states = collect(MutableStateFlow("a"), limits, LimitRecorder(results)) { limits.value = 40L }

        val last = states.last()
        assertEquals(samples.map { it.id }, last.runs.map { it.runId })
        assertEquals(DataError.Unavailable, last.loadError)
        assertEquals(false, last.canLoadMore)
    }

    @Test fun `받은 목록에는 실패 표시가 없다`() {
        val recorder = Recorder(mapOf("a" to Loaded.Ready(samples, isFromCache = false)))

        val states = collect(MutableStateFlow("a"), MutableStateFlow(RunRepository.PAGE_SIZE), recorder)

        assertEquals(null, states.last().loadError)
    }

    /** 개수별로 다른 결과를 주는 가짜 조회. */
    private class LimitRecorder(private val byLimit: Map<Long, Loaded<List<RunRecord>>>) {
        fun observe(@Suppress("UNUSED_PARAMETER") uid: String, limit: Long): Flow<Loaded<List<RunRecord>>> =
            flowOf(requireNotNull(byLimit[limit]) { "모르는 개수: $limit" })
    }

    private fun collect(
        uids: MutableStateFlow<String?>,
        limits: MutableStateFlow<Long>,
        recorder: LimitRecorder,
        block: suspend () -> Unit = {},
    ): List<RecordsUiState> = runBlocking {
        val states = mutableListOf<RecordsUiState>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            recordsUiStates(
                uids = uids,
                limits = limits,
                observeRuns = recorder::observe,
                onUidChanged = { limits.value = RunRepository.PAGE_SIZE },
            ).toList(states)
        }
        settle()
        block()
        settle()
        job.cancel()
        states
    }

    private companion object {
        const val SETTLE_YIELDS = 50
    }
}
