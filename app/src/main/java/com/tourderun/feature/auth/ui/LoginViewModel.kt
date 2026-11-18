package com.tourderun.feature.auth.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.tourderun.R
import com.tourderun.core.data.session.SessionRepository
import com.tourderun.feature.auth.data.GoogleSignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** S01에서 화면이 ViewModel에 알리는 일. */
sealed interface LoginAction {
    /** 버튼을 눌렀다. 계정 선택 창이 뜨기 전. */
    data object SignInStarted : LoginAction

    /** 계정 선택 창이 닫혔다. 결과를 넘긴다. */
    data class CredentialReceived(val result: GoogleSignInResult) : LoginAction
}

/** 한 번만 보여 줄 일. 화면을 돌려도 다시 뜨면 안 되는 것. */
sealed interface LoginEvent {
    data class ShowMessage(@StringRes val messageRes: Int) : LoginEvent
}

/**
 * S01 로그인 (AUTH-01). 로그인에 성공해도 화면을 직접 옮기지 않는다.
 * SessionRepository.state가 바뀌면 EntryGraph가 다음 화면(약관 또는 홈)으로 보낸다.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val session: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            LoginAction.SignInStarted -> {
                _uiState.update { it.copy(isLoading = true) }
            }
            is LoginAction.CredentialReceived -> handleCredential(action.result)
        }
    }

    private fun handleCredential(result: GoogleSignInResult) {
        when (result) {
            is GoogleSignInResult.Success -> viewModelScope.launch {
                session.signInWithGoogle(result.idToken)
                    .onFailure { error ->
                        stopLoading()
                        showMessage(
                            if (error is FirebaseNetworkException) {
                                R.string.login_error_network
                            } else {
                                R.string.login_error_failed
                            },
                        )
                    }
                // 성공이면 아무것도 안 한다. 스피너는 다음 화면으로 넘어갈 때까지 그대로 둔다.
            }
            GoogleSignInResult.Cancelled -> stopLoading()
            GoogleSignInResult.NoAccount -> {
                stopLoading()
                showMessage(R.string.login_error_no_account)
            }
            GoogleSignInResult.Failed -> {
                stopLoading()
                showMessage(R.string.login_error_failed)
            }
        }
    }

    private fun stopLoading() {
        _uiState.update { it.copy(isLoading = false) }
    }

    private fun showMessage(@StringRes messageRes: Int) {
        _events.trySend(LoginEvent.ShowMessage(messageRes))
    }
}