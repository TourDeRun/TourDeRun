package com.tourderun.feature.record.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.record.RunRepository
import com.tourderun.core.data.session.MyStats
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.uid
import com.tourderun.domain.record.RunRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * 로그인 uid·불러올 개수 → S11 상태.
 *
 * - uid가 null(로그인 전·로그아웃)이면 구독하지 않고 빈 목록을 낸다.
 * - uid가 바뀌면 이전 계정 구독을 끊고 다시 구독한다. 새 목록이 올 때까지는 불러오는 중(이전 계정 목록을 보이지 않는다).
 * - uid가 바뀌면 개수도 20개로 되돌린다([onUidChanged]. null로 바뀔 때도 불러서 같은 계정으로 다시 로그인해도 20개부터).
 * - 같은 uid에서 개수만 늘면 다시 구독하는 동안 직전 목록을 그대로 둔다.
 * - 리스너가 첫 값 전에 실패하면 직전 목록은 그대로 두고 [RecordsUiState.loadError]를 채운다(더 불러오기는 멈춘다).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun recordsUiStates(
    uids: Flow<String?>,
    limits: Flow<Long>,
    observeRuns: (uid: String, limit: Long) -> Flow<Loaded<List<RunRecord>>>,
    onUidChanged: () -> Unit,
): Flow<RecordsUiState> = uids
    .distinctUntilChanged()
    .flatMapLatest { uid ->
        onUidChanged()
        if (uid == null) {
            flowOf(recordsUiState(emptyList(), RunRepository.PAGE_SIZE))
        } else {
            limits
                .flatMapLatest { limit -> observeRuns(uid, limit).map { loaded -> limit to loaded } }
                .scan(RecordsUiState.Loading) { previous, (limit, loaded) ->
                    when (loaded) {
                        is Loaded.Ready -> recordsUiState(loaded.value, limit)
                        is Loaded.Failed -> previous.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            canLoadMore = false,
                            loadError = loaded.error,
                        )
                    }
                }
        }
    }

/**
 * S11 내 기록 (REC-02). 로그인한 계정의 기록을 20개부터 보여 주고, 끝까지 내리면 20개씩 늘려 다시 구독한다.
 * uid는 [SessionRepository]에서 읽는다. 불러오지 못하면 [retry]로 처음(20개)부터 다시 구독한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordsViewModel @Inject constructor(
    runRepository: RunRepository,
    sessionRepository: SessionRepository,
) : ViewModel() {

    private val limit = MutableStateFlow(RunRepository.PAGE_SIZE)

    /** 다시 시도할 때마다 1씩 늘린다. 값이 바뀌면 리스너를 새로 붙인다. */
    private val attempt = MutableStateFlow(0)

    /** AUTH-03 `users` 누적 값. 읽기 전·실패면 null → 불러온 기록의 합을 그대로 보여 준다. */
    private val myStats: Flow<MyStats?> = sessionRepository.state
        .map { it.uid }
        .distinctUntilChanged()
        .flatMapLatest<String?, MyStats?> { uid ->
            if (uid == null) flowOf(null)
            else sessionRepository.observeMyStats(uid).map { (it as? Loaded.Ready)?.value }
        }
        .onStart { emit(null) }

    val uiState: StateFlow<RecordsUiState> =
        combine(
            attempt.flatMapLatest {
                recordsUiStates(
                    uids = sessionRepository.state.map { it.uid },
                    limits = limit,
                    observeRuns = runRepository::observeRuns,
                    onUidChanged = { limit.value = RunRepository.PAGE_SIZE },
                )
            },
            myStats,
        ) { state, stats -> state.withUserTotals(stats) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordsUiState.Loading)

    /** 지금 보이는 개수 기준이라 여러 번 불려도 한 쪽만 늘어난다. */
    fun loadMore() {
        val state = uiState.value
        if (state.canLoadMore) limit.value = state.runs.size + RunRepository.PAGE_SIZE
    }

    /** 불러오기 실패 후 다시 시도. */
    fun retry() {
        attempt.update { it + 1 }
    }
}
