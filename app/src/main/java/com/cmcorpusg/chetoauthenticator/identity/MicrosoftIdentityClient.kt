package com.cmcorpusg.chetoauthenticator.identity

import androidx.fragment.app.FragmentActivity
import com.cmcorpusg.chetoauthenticator.R
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MicrosoftIdentityClient(
    private val activity: FragmentActivity
) {
    suspend fun acquireOneDriveToken(): String =
        acquireToken(arrayOf("Files.ReadWrite.AppFolder", "User.Read")).accessToken

    suspend fun signIn(): IdentityAuthResult {
        val authenticationResult = acquireToken(arrayOf("User.Read"))
        val account = authenticationResult.account
        val email = account.username.trim()
        require(email.isNotBlank()) { "Microsoft no devolvió un correo" }
        val claims = account.claims
        val displayName = claims?.get("name")?.toString().orEmpty()

        return IdentityAuthResult(
            provider = "microsoft",
            subject = account.id,
            email = email,
            displayName = displayName,
            photo = ""
        )
    }

    private suspend fun acquireToken(scopes: Array<String>): IAuthenticationResult {
        val app = createApplication()
        return suspendCancellableCoroutine { continuation ->
            val parameters = AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(activity)
                .withScopes(scopes.toList())
                .withCallback(
                    object : AuthenticationCallback {
                        override fun onSuccess(authenticationResult: IAuthenticationResult) {
                            if (continuation.isActive) continuation.resume(authenticationResult)
                        }

                        override fun onError(exception: MsalException) {
                            if (continuation.isActive) continuation.resumeWithException(exception)
                        }

                        override fun onCancel() {
                            if (continuation.isActive) {
                                continuation.resumeWithException(MicrosoftSignInCancelledException())
                            }
                        }
                    }
                )
                .build()

            app.acquireToken(parameters)
        }
    }

    private suspend fun createApplication(): IMultipleAccountPublicClientApplication =
        suspendCancellableCoroutine { continuation ->
            PublicClientApplication.createMultipleAccountPublicClientApplication(
                activity.applicationContext,
                R.raw.auth_config_single_account,
                object : IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
                    override fun onCreated(application: IMultipleAccountPublicClientApplication) {
                        if (continuation.isActive) continuation.resume(application)
                    }

                    override fun onError(exception: MsalException) {
                        if (continuation.isActive) continuation.resumeWithException(exception)
                    }
                }
            )
        }
}

class MicrosoftSignInCancelledException : Exception("Inicio de sesión con Microsoft cancelado")
