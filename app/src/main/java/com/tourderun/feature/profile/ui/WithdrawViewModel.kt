package com.tourderun.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourderun.R
import com.tourderun.core.data.DataError
import com.tourderun.core.data.account.AccountRepository
import com.tourderun.core.data.firestore.toDataError
import com.tourderun.core.data.run.RunActivity
import com.tourderun.core.ui.blockedMessageRes
import com.tourderun.core.data.session.GoogleSignInResult
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.core.data.session.teamId
import com.tourderun.core.data.session.uid
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    /** AUTH-04 로그아웃 버튼. 러닝 중이면 안내만, 아니면 확인 창. */
    data object LogoutClick : WithdrawAction
    data object LogoutDismiss : WithdrawAction
    /** 확인 창의 로그아웃. 실행 직전에 러닝 상태를 다시 본다. */
    data object LogoutConfirm : WithdrawAction
    data object Click : WithdrawAction
    data object Dismiss : WithdrawAction
    /** 확인을 눌렀다. 화면이 이어서 구글 계정 창을 띄운다(Activity가 필요해서). */
    data object ReauthStarted : WithdrawAction
    data class CredentialReceived(val result: GoogleSignInResult) : WithdrawAction
}

/**
 * S13 계정 동작: 로그아웃(AUTH-04)·회원 탈퇴(AUTH-05). 성공하면 세션이 SignedOut이 되어 화면 이동은 `followEntryStage`가 한다.
 *
 * RUN-02: 러닝 측정·저장 대기 중이면 막는다. 판단은 늘 저장소를 **지금 읽은 값**(`busyReason.first()`)으로 한다
 * (미리 받아 둔 값은 첫 조회 전 null일 수 있다). 버튼을 누를 때와, 확인 창에서 실제로 실행하기 직전에 두 번 본다(#132 리뷰).
 */
@HiltViewModel
class WithdrawViewModel @Inject constructor(
    private val session: SessionRepository,
    private val accountRepository: AccountRepository,
    private val runActivity: RunActivity,
) : ViewModel() {

    private val _showLogoutConfirm = MutableStateFlow(false)
    /** AUTH-04 로그아웃 확인 창이 열려 있는지(회전에도 ViewModel이 남아 유지된다). */
    val showLogoutConfirm: StateFlow<Boolean> = _showLogoutConfirm.asStateFlow()

    private val _messages = Channel<Int>(Channel.BUFFERED)
    /** 한 번만 보여 줄 안내(문구 리소스 ID). */
    val messages: Flow<Int> = _messages.receiveAsFlow()

    private val _uiState = MutableStateFlow(WithdrawUiState())
    val uiState: StateFlow<WithdrawUiState> = _uiState.asStateFlow()

    fun onAction(action: WithdrawAction) {
        when (action) {
            WithdrawAction.LogoutClick -> ifNotBusy { _showLogoutConfirm.value = true }
            WithdrawAction.LogoutDismiss -> _showLogoutConfirm.value = false
            WithdrawAction.LogoutConfirm -> {
                _showLogoutConfirm.value = false
                ifNotBusy { session.signOut() }
            }
            WithdrawAction.Click -> ifNotBusy { _uiState.value = WithdrawUiState(showConfirm = true) }
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

    /** 지금 러닝 때문에 막혀 있으면 이유에 맞는 안내를 보내고 [block]을 하지 않는다. */
    private fun ifNotBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            val reason = runActivity.busyReason.first()
            if (reason != null) _messages.send(reason.blockedMessageRes()) else block()
        }
    }

    private fun withdraw(idToken: String) {
        val state = session.state.value
        val uid = state.uid ?: return
        viewModelScope.launch {
            // 재인증 창이 떠 있는 동안 러닝이 시작·저장 대기가 됐을 수 있어 실행 직전에 다시 본다
            val reason = runActivity.busyReason.first()
            if (reason != null) {
                _uiState.value = WithdrawUiState()
                _messages.send(reason.blockedMessageRes())
                return@launch
            }
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
