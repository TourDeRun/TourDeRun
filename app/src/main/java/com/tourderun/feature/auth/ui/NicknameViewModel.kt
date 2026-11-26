package com.tourderun.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.R
import com.tourderun.core.data.DataError
import com.tourderun.core.data.firestore.toDataError
import com.tourderun.core.data.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface NicknameAction {
    data class NicknameChanged(val value: String) : NicknameAction
    data object Submit : NicknameAction
}

/**
 * S02 닉네임 설정 (AUTH-02). 저장에 성공해도 화면을 직접 옮기지 않는다.
 * users 문서가 생기면 SessionRepository.state가 NoTeam이 되고, EntryGraph가 팀 시작 화면으로 보낸다.
 */
@HiltViewModel
class NicknameViewModel @Inject constructor(
    private val session: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NicknameUiState())
    val uiState: StateFlow<NicknameUiState> = _uiState.asStateFlow()

    fun onAction(action: NicknameAction) {
        when (action) {
            is NicknameAction.NicknameChanged -> {
                _uiState.update { it.copy(nickname = action.value, errorRes = null) }
            }
            NicknameAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _uiState.value
        if (current.isSubmitting || !current.isValid) return

        _uiState.update { it.copy(isSubmitting = true, errorRes = null) }
        viewModelScope.launch {
            session.createUser(current.nickname.trim())
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorRes = nicknameErrorRes(error.toDataError()),
                        )
                    }
                }
        }
    }
}

/** users 만들기 실패 → 문구. 오프라인·10초 제한(createUser의 withTimeout)이면 연결 문구. */
internal fun nicknameErrorRes(error: DataError): Int = when (error) {
    DataError.Unavailable -> R.string.nickname_error_network
    DataError.PermissionDenied, DataError.Other -> R.string.nickname_error_failed
}
