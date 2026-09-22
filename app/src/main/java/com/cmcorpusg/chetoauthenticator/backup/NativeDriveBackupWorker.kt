package com.cmcorpusg.chetoauthenticator.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cmcorpusg.chetoauthenticator.data.NativeVault
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NativeDriveBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settings = BackupSettings(applicationContext)
        if (!settings.driveEnabled) return@withContext Result.success()

        val vaultStore = NativeVault(applicationContext)
        if (!vaultStore.exists()) return@withContext Result.success()

        try {
            // Compare locally before touching Google. If the vault did not change,
            // this periodic run performs zero Google authorization/API calls.
            val plain = NativeVault.exportPortableJson(vaultStore.read())
            val fingerprint = BackupContentFingerprint.sha256(plain)
            if (settings.lastGoogleContentHash == fingerprint) {
                settings.lastError = null
                return@withContext Result.success()
            }

            val recoveryKeys = RecoveryKeyStore(applicationContext)
            val key = recoveryKeys.getKey()
            val salt = recoveryKeys.getSalt()
            if (key == null || salt == null) {
                settings.lastError = "Google Drive requiere volver a configurar la contraseña de backup dentro de CHETO"
                return@withContext Result.success()
            }

            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(Scopes.DRIVE_APPFOLDER)))
                .build()

            val authResult = Tasks.await(
                Identity.getAuthorizationClient(applicationContext).authorize(request)
            )

            if (authResult.hasResolution() || authResult.accessToken.isNullOrBlank()) {
                settings.lastError = "Google Drive requiere volver a autorizarse dentro de CHETO"
                // Do not use Result.retry(): the user asked for cloud access to remain
                // on-demand/periodic instead of causing extra background retries.
                return@withContext Result.success()
            }

            val encrypted = BackupCrypto.encrypt(plain, key, salt)
            DriveBackupClient("cheto_native_backup_").upload(
                authResult.accessToken!!,
                encrypted
            )

            settings.lastGoogleContentHash = fingerprint
            settings.lastBackupEpochMillis = System.currentTimeMillis()
            settings.lastError = null
            Result.success()
        } catch (error: Exception) {
            settings.lastError = error.message?.take(200)
                ?: "No se pudo completar el backup automático de Google Drive"
            // Wait for the next normal 24 h cycle. Manual "Guardar ahora" remains
            // available and is the place where interactive OAuth should occur.
            Result.success()
        }
    }
}
