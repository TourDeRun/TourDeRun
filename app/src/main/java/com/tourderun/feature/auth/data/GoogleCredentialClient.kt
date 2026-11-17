package com.tourderun.feature.auth.data

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/** 계정 선택 창의 결과. */
sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult   // 사용자가 창을 닫음 → 아무 문구도 안 보임
    data object NoAccount : GoogleSignInResult   // 기기에 구글 계정이 없음
    data object Failed : GoogleSignInResult      // 그 밖의 실패
}

/**
 * S01: 기기의 구글 계정 선택 창(Credential Manager)을 띄우고 ID 토큰을 받는다 (AUTH-01).
 * 창을 띄우려면 Activity Context가 필요하다. ViewModel은 Context를 받지 않으므로 Screen이 부르고 결과만 ViewModel에 넘긴다.
 */
class GoogleCredentialClient {

    suspend fun requestIdToken(activityContext: Context): GoogleSignInResult {
        // google-services.json에서 자동으로 만들어지는 "웹 클라이언트 ID". 파일이 없는 CI 빌드에서도 컴파일되게 이름으로 찾는다.
        val resId = activityContext.resources.getIdentifier("default_web_client_id", "string", activityContext.packageName)
        if (resId == 0) return GoogleSignInResult.Failed

        val option = GetSignInWithGoogleOption.Builder(activityContext.getString(resId)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleSignInResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleSignInResult.Failed
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "쓸 수 있는 구글 계정 없음: ${e.type} ${e.errorMessage}")
            GoogleSignInResult.NoAccount
        } catch (e: GetCredentialException) {
            Log.w(TAG, "계정 선택 실패: ${e.type}")
            GoogleSignInResult.Failed
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "ID 토큰 해석 실패")
            GoogleSignInResult.Failed
        }
    }

    private companion object {
        const val TAG = "TDR/auth"
    }
}