package com.tourderun.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.R
import com.tourderun.core.data.DataError
import com.tourderun.core.data.account.AccountRepository
import com.tourderun.core.data.firestore.toDataError
import com.tourderun.core.data.session.GoogleSignInResult
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.teamId
import com.tourderun.core.data.session.uid
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** AUTH-05 탈퇴 창 상태. */
data class WithdrawUiState(
    val showConfirm: Boolean = false,
    /** 재인증 창이 떠 있거나 탈퇴를 진행 중. */
    val isWorking: Boolean = false,
    val errorRes: Int? = null,
)

sealed interface WithdrawAction {
    data object Click : WithdrawAction
    data object Dismiss : WithdrawAction
    /** 확인을 눌렀다. 화면이 이어서 구글 계정 창을 띄운다(Activity가 필요해서). */
    data object ReauthStarted : WithdrawAction
    data class CredentialReceived(val result: GoogleSignInResult) : WithdrawAction
}

/**
 * S13 회원 탈퇴 (AUTH-05). 성공하면 세션이 SignedOut이 되어 화면 이동은 `followEntryStage`가 한다.
 * TODO(RUN-02): 러닝 측정 중이면 막는다.
 */
@HiltViewModel
class WithdrawViewModel @Inject constructor(
    private val session: SessionRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WithdrawUiState())
    val uiState: StateFlow<WithdrawUiState> = _uiState.asStateFlow()

    fun onAction(action: WithdrawAction) {
        when (action) {
            WithdrawAction.Click -> _uiState.value = WithdrawUiState(showConfirm = true)
            WithdrawAction.Dismiss -> _uiState.update { if (it.isWorking) it else WithdrawUiState() }
            WithdrawAction.ReauthStarted -> _uiState.update { it.copy(isWorking = true, errorRes = null) }
            is WithdrawAction.CredentialReceived -> onCredential(action.result)
        }
    }

    private fun onCredential(result: GoogleSignInResult) {
        when (result) {
            is GoogleSignInResult.Success -> withdraw(result.idToken)
            GoogleSignInResult.Cancelled -> _uiState.update { it.copy(isWorking = false) }
            GoogleSignInResult.NoAccount, GoogleSignInResult.Failed ->
                _uiState.update { it.copy(isWorking = false, errorRes = R.string.withdraw_error_reauth) }
        }
    }

    private fun withdraw(idToken: String) {
        val state = session.state.value
        val uid = state.uid ?: return
        viewModelScope.launch {
            accountRepository.withdraw(uid = uid, teamId = state.teamId, idToken = idToken)
                .onFailure { error ->
                    _uiState.update { it.copy(isWorking = false, errorRes = withdrawErrorRes(error.toDataError())) }
                }
            // 성공이면 그대로 둔다. 세션이 SignedOut이 되면 S01로 간다.
        }
    }
}

/** 탈퇴 실패 → 문구. 온라인 필수라 연결 문제면 다시 시도를 권한다. */
internal fun withdrawErrorRes(error: DataError): Int = when (error) {
    DataError.Unavailable -> R.string.withdraw_error_network
    DataError.PermissionDenied, DataError.Other -> R.string.withdraw_error_failed
}
