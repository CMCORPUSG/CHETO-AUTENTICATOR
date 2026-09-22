package com.cmcorpusg.chetoauthenticator.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cmcorpusg.chetoauthenticator.R
import com.cmcorpusg.chetoauthenticator.data.NativeVault
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.PublicClientApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NativeOneDriveBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settings = BackupSettings(applicationContext)
        if (!settings.oneDriveEnabled) return@withContext Result.success()

        val vaultStore = NativeVault(applicationContext)
        if (!vaultStore.exists()) return@withContext Result.success()

        try {
            // Compare locally before touching Microsoft. If the vault did not change,
            // this periodic run performs zero Microsoft authorization/API calls.
            val plain = NativeVault.exportPortableJson(vaultStore.read())
            val fingerprint = BackupContentFingerprint.sha256(plain)
            if (settings.lastOneDriveContentHash == fingerprint) {
                settings.lastError = null
                return@withContext Result.success()
            }

            val accountId = settings.oneDriveAccountId
            if (accountId.isNullOrBlank()) {
                settings.lastError = "Microsoft OneDrive requiere seleccionar una cuenta dentro de CHETO"
                return@withContext Result.success()
            }

            val recoveryKeys = RecoveryKeyStore(applicationContext, "onedrive")
            val key = recoveryKeys.getKey()
            val salt = recoveryKeys.getSalt()
            if (key == null || salt == null) {
                settings.lastError = "OneDrive requiere volver a configurar la contraseña de backup dentro de CHETO"
                return@withContext Result.success()
            }

            val app = PublicClientApplication.createMultipleAccountPublicClientApplication(
                applicationContext,
                R.raw.auth_config_single_account
            )
            val account = app.accounts.firstOrNull { it.id == accountId }
            if (account == null) {
                settings.lastError = "Microsoft OneDrive requiere volver a iniciar sesión dentro de CHETO"
                return@withContext Result.success()
            }

            val silent = AcquireTokenSilentParameters.Builder()
                .withScopes(listOf("Files.ReadWrite.AppFolder", "User.Read"))
                .forAccount(account)
                .fromAuthority(account.authority)
                .build()

            val result = app.acquireTokenSilent(silent)
            val encrypted = BackupCrypto.encrypt(plain, key, salt)
            OneDriveBackupClient("cheto_native_backup_").upload(
                result.accessToken,
                encrypted
            )

            settings.lastOneDriveContentHash = fingerprint
            settings.lastOneDriveBackupEpochMillis = System.currentTimeMillis()
            settings.lastError = null
            Result.success()
        } catch (error: Exception) {
            settings.lastError = error.message?.take(200)
                ?: "No se pudo completar el backup automático de OneDrive"
            // Avoid WorkManager backoff retries that would create unnecessary
            // cloud calls. The next periodic cycle or a manual action will retry.
            Result.success()
        }
    }
}
