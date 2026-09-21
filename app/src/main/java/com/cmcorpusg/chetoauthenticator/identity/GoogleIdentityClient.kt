package com.cmcorpusg.chetoauthenticator.identity

import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.fragment.app.FragmentActivity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.SecureRandom
import java.util.Base64

data class IdentityAuthResult(
    val provider: String,
    val subject: String,
    val email: String,
    val displayName: String,
    val photo: String
)

class GoogleIdentityClient(
    private val activity: FragmentActivity,
    private val serverClientId: String
) {
    suspend fun signIn(): IdentityAuthResult {
        require(serverClientId.isNotBlank()) { "Google Sign-In no está configurado" }

        val nonce = ByteArray(32).also(SecureRandom()::nextBytes)
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

        val option = GetSignInWithGoogleOption.Builder(serverClientId)
            .setNonce(nonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val response = CredentialManager.create(activity).getCredential(
            context = activity,
            request = request
        )
        val credential = response.credential
        require(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "Google no devolvió una credencial compatible" }

        val google = GoogleIdTokenCredential.createFrom(credential.data)
        val email = google.email?.trim().orEmpty()
        require(email.isNotBlank()) { "Google no devolvió un correo" }

        // The ID token is intentionally not persisted. CHETO only stores the
        // provider identity metadata needed to display the linked account.
        return IdentityAuthResult(
            provider = "google",
            subject = google.uniqueId,
            email = email,
            displayName = google.displayName.orEmpty(),
            photo = google.profilePictureUri?.toString().orEmpty()
        )
    }
}
