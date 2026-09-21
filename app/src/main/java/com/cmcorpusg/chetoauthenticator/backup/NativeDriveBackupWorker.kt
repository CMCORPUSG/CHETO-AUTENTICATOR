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

        val recoveryKeys = RecoveryKeyStore(applicationContext)
        val key = recoveryKeys.getKey() ?: return@withContext Result.failure()
        val salt = recoveryKeys.getSalt() ?: return@withContext Result.failure()

        try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(Scopes.DRIVE_APPFOLDER)))
                .build()

            val authResult = Tasks.await(
                Identity.getAuthorizationClient(applicationContext).authorize(request)
            )

            if (authResult.hasResolution() || authResult.accessToken.isNullOrBlank()) {
                settings.lastError = "Google Drive requiere volver a autorizarse dentro de CHETO"
                return@withContext Result.retry()
            }

            val plain = NativeVault.exportPortableJson(vaultStore.read())
            val encrypted = BackupCrypto.encrypt(plain, key, salt)
            DriveBackupClient("cheto_native_backup_").upload(
                authResult.accessToken!!,
                encrypted
            )

            settings.lastBackupEpochMillis = System.currentTimeMillis()
            settings.lastError = null
            Result.success()
        } catch (error: Exception) {
            settings.lastError = error.message?.take(200)
            Result.retry()
        }
    }
}
