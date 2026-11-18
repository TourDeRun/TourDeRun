package com.tourderun.feature.auth.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tourderun.R
import com.tourderun.core.designsystem.component.Mascot
import com.tourderun.core.designsystem.component.MascotFrame
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSize
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme
import com.tourderun.feature.auth.data.GoogleCredentialClient
import kotlinx.coroutines.launch

/** S01 로그인 화면 상태. */
data class LoginUiState(
    /** 구글 로그인 진행 중이면 버튼을 막고 스피너를 보인다. */
    val isLoading: Boolean = false,
)

/** S01 로그인. 계정 선택 창을 띄우고 결과를 ViewModel에 넘긴다. 화면 이동은 EntryGraph가 세션 상태를 보고 한다. */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current // Activity. 계정 창을 띄울 때 필요
    val scope = rememberCoroutineScope() // 버튼 클릭에서 suspend 함수를 부르기 위한 범위
    val client = remember { GoogleCredentialClient() }

    // 일회성 이벤트 받기: 화면이 떠 있는 동안 계속 듣는다
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.ShowMessage ->
                    Toast.makeText(context, event.messageRes, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LoginContent(
        uiState = uiState,
        onGoogleSignIn = {
            viewModel.onAction(LoginAction.SignInStarted)
            scope.launch {
                val result = client.requestIdToken(context)
                viewModel.onAction(LoginAction.CredentialReceived(result))
            }
        },
        modifier = modifier,
    )
}

/** S01 로그인 화면 그리기만 한다 (상태 없음, Preview 대상). */
@Composable
fun LoginContent(
    uiState: LoginUiState,
    onGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bgSurface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s8, bottom = 40.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppIcon(Modifier.size(112.dp), contentDescription = stringResource(R.string.login_app_icon_desc))
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.app_name), style = TdrTheme.typography.display, color = colors.brand)
            Spacer(Modifier.height(TdrSpacing.s2))
            Text(
                stringResource(R.string.login_tagline),
                style = TdrTheme.typography.body,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
            )
        }
        Surface(
            onClick = onGoogleSignIn,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(TdrSize.buttonLg),
            shape = TdrShapes.md,
            color = colors.bgSurface,
            contentColor = colors.ink,
            border = BorderStroke(1.dp, colors.borderControl),
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = colors.brand, strokeWidth = 2.dp)
                } else {
                    Image(TdrIcons.GoogleLogo, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.login_google), style = TdrTheme.typography.subtitle)
            }
        }
        Spacer(Modifier.height(TdrSpacing.s3))
        Text(
            stringResource(R.string.login_hint),
            style = TdrTheme.typography.caption,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 앱 아이콘: 파란 둥근 사각 + 뛰뛰. */
@Composable
private fun AppIcon(modifier: Modifier, contentDescription: String) {
    val brand = TdrTheme.colors.brand
    Box(
        modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(brand, cornerRadius = CornerRadius(size.minDimension * 0.23f))
        }
        Mascot(size = 92.dp, frame = MascotFrame.None, bandColor = TdrTheme.colors.brandStrong)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LoginContentPreview() {
    TdrTheme { LoginContent(LoginUiState(), onGoogleSignIn = {}) }
}
