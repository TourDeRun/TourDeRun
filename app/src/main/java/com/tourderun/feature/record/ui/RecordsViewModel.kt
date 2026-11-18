package com.tourderun.feature.record.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.Loaded
import com.tourderun.core.data.record.RunRepository
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.uid
import com.tourderun.domain.record.RunRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * 로그인 uid·불러올 개수 → S11 상태.
 *
 * - uid가 null(로그인 전·로그아웃)이면 구독하지 않고 빈 목록을 낸다.
 * - uid가 바뀌면 이전 계정 구독을 끊고 다시 구독한다. 새 목록이 올 때까지는 불러오는 중(이전 계정 목록을 보이지 않는다).
 * - uid가 바뀌면 개수도 20개로 되돌린다([onUidChanged]. null로 바뀔 때도 불러서 같은 계정으로 다시 로그인해도 20개부터).
 * - 같은 uid에서 개수만 늘면 다시 구독하는 동안 직전 목록을 그대로 둔다.
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
            limits.flatMapLatest { limit ->
                observeRuns(uid, limit).mapNotNull { loaded ->
                    // TODO(REC-02): 불러오기 실패 화면. 지금은 실패를 건너뛰어 직전 상태(처음이면 불러오는 중)를 유지한다.
                    (loaded as? Loaded.Ready)?.let { recordsUiState(it.value, limit) }
                }
            }.onStart { emit(RecordsUiState.Loading) }
        }
    }

/**
 * S11 내 기록 (REC-02). 로그인한 계정의 기록을 20개부터 보여 주고, 끝까지 내리면 20개씩 늘려 다시 구독한다.
 * uid는 [SessionRepository]에서 읽는다.
 */
@HiltViewModel
class RecordsViewModel @Inject constructor(
    runRepository: RunRepository,
    sessionRepository: SessionRepository,
) : ViewModel() {

    private val limit = MutableStateFlow(RunRepository.PAGE_SIZE)

    val uiState: StateFlow<RecordsUiState> =
        recordsUiStates(
            uids = sessionRepository.state.map { it.uid },
            limits = limit,
            observeRuns = runRepository::observeRuns,
            onUidChanged = { limit.value = RunRepository.PAGE_SIZE },
        )
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordsUiState.Loading)

    /** 지금 보이는 개수 기준이라 여러 번 불려도 한 쪽만 늘어난다. */
    fun loadMore() {
        val state = uiState.value
        if (state.canLoadMore) limit.value = state.runs.size + RunRepository.PAGE_SIZE
    }
}
