package com.cmcorpusg.chetoauthenticator.identity

import androidx.fragment.app.FragmentActivity
import com.cmcorpusg.chetoauthenticator.R
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MicrosoftIdentityClient(
    private val activity: FragmentActivity
) {
    suspend fun signIn(): IdentityAuthResult {
        val app = createApplication()
        return suspendCancellableCoroutine { continuation ->
            app.signIn(
                activity,
                null,
                arrayOf("User.Read"),
                object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        if (!continuation.isActive) return
                        val account = authenticationResult.account
                        val email = account.username.trim()
                        if (email.isBlank()) {
                            continuation.resumeWithException(
                                IllegalStateException("Microsoft no devolvió un correo")
                            )
                            return
                        }
                        val claims = account.claims
                        val displayName = claims?.get("name")?.toString().orEmpty()
                        continuation.resume(
                            IdentityAuthResult(
                                provider = "microsoft",
                                subject = account.id,
                                email = email,
                                displayName = displayName,
                                photo = ""
                            )
                        )
                    }

                    override fun onError(exception: MsalException) {
                        if (continuation.isActive) continuation.resumeWithException(exception)
                    }

                    override fun onCancel() {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                MicrosoftSignInCancelledException()
                            )
                        }
                    }
                }
            )
        }
    }

    private suspend fun createApplication(): ISingleAccountPublicClientApplication =
        suspendCancellableCoroutine { continuation ->
            PublicClientApplication.createSingleAccountPublicClientApplication(
                activity.applicationContext,
                R.raw.auth_config_single_account,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
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
