package com.tourderun.feature.record.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tourderun.core.data.record.RunRepository
import com.tourderun.core.navigation.RecordDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** S12가 지금 무엇을 보여 줄지. */
sealed interface RecordDetailLoad {
    data object Loading : RecordDetailLoad

    /** 기록이 없다(지웠거나 처음부터 없음). 화면을 닫는다. */
    data object Gone : RecordDetailLoad

    data class Ready(val ui: RecordDetailUiState) : RecordDetailLoad
}

/**
 * S12 기록 상세 (REC-03·04·05).
 *
 * 수정·삭제는 오프라인 대기 쓰기라 기다리지 않는다. 쓰기를 시작하면 리스너가 바로 새 값을 주므로
 * 저장 뒤에는 고치던 값을 비우고, 삭제 뒤에는 기록이 null이 되면서 [RecordDetailLoad.Gone]으로 화면이 닫힌다.
 */
@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runRepository: RunRepository,
) : ViewModel() {

    private val runId = savedStateHandle.toRoute<RecordDetailRoute>().runId
    private val edit = MutableStateFlow(Edit())

    val uiState: StateFlow<RecordDetailLoad> = combine(runRepository.observeRun(runId), edit) { record, edit ->
        if (record == null) {
            RecordDetailLoad.Gone
        } else {
            RecordDetailLoad.Ready(recordDetailUiState(record, edit.title, edit.memo, edit.showDeleteConfirm))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordDetailLoad.Loading)

    fun onTitleChange(title: String) = edit.update { it.copy(title = title) }

    fun onMemoChange(memo: String) = edit.update { it.copy(memo = memo) }

    fun onSave() {
        val ui = (uiState.value as? RecordDetailLoad.Ready)?.ui ?: return
        if (!ui.canSave) return
        if (runRepository.updateTitleMemo(runId, ui.title, ui.memo).isSuccess) {
            edit.update { it.copy(title = null, memo = null) }
        }
    }

    fun onDeleteClick() = edit.update { it.copy(showDeleteConfirm = true) }

    fun onDeleteDismiss() = edit.update { it.copy(showDeleteConfirm = false) }

    fun onDeleteConfirm() {
        edit.update { it.copy(showDeleteConfirm = false) }
        runRepository.delete(runId)
    }

    /** 고치는 중인 값. null이면 아직 손대지 않은 칸. */
    private data class Edit(
        val title: String? = null,
        val memo: String? = null,
        val showDeleteConfirm: Boolean = false,
    )
}
