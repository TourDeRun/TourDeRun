package com.tourderun.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.R
import com.tourderun.core.data.DataError
import com.tourderun.core.data.account.AccountRepository
import com.tourderun.core.data.firestore.toDataError
import com.tourderun.core.data.run.RunActivity
import com.tourderun.core.data.session.GoogleSignInResult
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.teamId
import com.tourderun.core.data.session.uid
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
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
    /** RUN-02: 러닝 중이라 막혔다(로그아웃도 이걸로 안내만 한다). */
    data object Blocked : WithdrawAction
    data object Dismiss : WithdrawAction
    /** 확인을 눌렀다. 화면이 이어서 구글 계정 창을 띄운다(Activity가 필요해서). */
    data object ReauthStarted : WithdrawAction
    data class CredentialReceived(val result: GoogleSignInResult) : WithdrawAction
}

/**
 * S13 회원 탈퇴 (AUTH-05). 성공하면 세션이 SignedOut이 되어 화면 이동은 `followEntryStage`가 한다.
 * 러닝 측정·저장 대기 중이면 막는다(RUN-02).
 */
@HiltViewModel
class WithdrawViewModel @Inject constructor(
    private val session: SessionRepository,
    private val accountRepository: AccountRepository,
    runActivity: RunActivity,
) : ViewModel() {

    /** RUN-02 러닝 측정·저장 대기 중이면 true → 로그아웃·탈퇴를 막는다. */
    val runBusy: StateFlow<Boolean> = runActivity.isBusy
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _messages = Channel<Int>(Channel.BUFFERED)
    /** 한 번만 보여 줄 안내(문구 리소스 ID). */
    val messages: Flow<Int> = _messages.receiveAsFlow()

    private val _uiState = MutableStateFlow(WithdrawUiState())
    val uiState: StateFlow<WithdrawUiState> = _uiState.asStateFlow()

    fun onAction(action: WithdrawAction) {
        when (action) {
            WithdrawAction.Click ->
                if (runBusy.value) _messages.trySend(R.string.run_busy_blocked)
                else _uiState.value = WithdrawUiState(showConfirm = true)
            WithdrawAction.Blocked -> _messages.trySend(R.string.run_busy_blocked)
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
