package com.tourderun.feature.record.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.core.data.record.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * S11 내 기록 (REC-02). 20개부터 시작해 끝까지 내리면 20개씩 늘려 다시 구독한다.
 * 다시 구독하는 동안에는 직전 목록을 그대로 보여 준다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordsViewModel @Inject constructor(
    runRepository: RunRepository,
) : ViewModel() {

    private val limit = MutableStateFlow(RunRepository.PAGE_SIZE)

    val uiState: StateFlow<RecordsUiState> = limit
        .flatMapLatest { limit -> runRepository.observeRuns(MY_UID, limit).map { recordsUiState(it, limit) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordsUiState.Loading)

    /** 지금 보이는 개수 기준이라 여러 번 불려도 한 쪽만 늘어난다. */
    fun loadMore() {
        val state = uiState.value
        if (state.canLoadMore) limit.value = state.runs.size + RunRepository.PAGE_SIZE
    }

    private companion object {
        // TODO(AUTH-01): SessionRepository가 생기면 로그인한 uid로 바꾼다. 가짜 Repository는 uid를 보지 않는다.
        const val MY_UID = ""
    }
}
