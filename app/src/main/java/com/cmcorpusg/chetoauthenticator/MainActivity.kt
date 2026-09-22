package com.cmcorpusg.chetoauthenticator

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.cmcorpusg.chetoauthenticator.backup.BackupCrypto
import com.cmcorpusg.chetoauthenticator.backup.BackupScheduler
import com.cmcorpusg.chetoauthenticator.backup.BackupSettings
import com.cmcorpusg.chetoauthenticator.backup.DriveBackupClient
import com.cmcorpusg.chetoauthenticator.backup.RecoveryKeyStore
import com.cmcorpusg.chetoauthenticator.core.OtpAuthParser
import com.cmcorpusg.chetoauthenticator.core.TotpEngine
import com.cmcorpusg.chetoauthenticator.data.AuthAccount
import com.cmcorpusg.chetoauthenticator.data.SecureAccountStore
import com.cmcorpusg.chetoauthenticator.ui.AuthenticatorScreen
import com.cmcorpusg.chetoauthenticator.ui.LockedScreen
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.android.gms.tasks.Task
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

class MainActivity : FragmentActivity() {
    private lateinit var store: SecureAccountStore
    private lateinit var backupSettings: BackupSettings
    private var accounts by mutableStateOf<List<AuthAccount>>(emptyList())
    private var unlocked by mutableStateOf(false)
    private var backupEnabled by mutableStateOf(false)
    private var lastBackupMillis by mutableStateOf(0L)
    private var pendingDriveAction: ((String) -> Unit)? = null

    private val authorizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK || result.data == null) {
                toast("Autorización de Google Drive cancelada")
                pendingDriveAction = null
                return@registerForActivityResult
            }

            try {
                val authorizationResult = Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(result.data!!)
                val token = authorizationResult.accessToken
                if (!token.isNullOrBlank()) {
                    pendingDriveAction?.invoke(token)
                } else {
                    toast("Google Drive no devolvió un token de acceso")
                }
            } catch (_: ApiException) {
                toast("No se pudo completar la autorización de Google Drive")
            } finally {
                pendingDriveAction = null
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SecureAccountStore(this)
        backupSettings = BackupSettings(this)
        accounts = store.load()
        backupEnabled = backupSettings.driveEnabled
        lastBackupMillis = backupSettings.lastBackupEpochMillis

        setContent {
            if (!unlocked) {
                LockedScreen(onUnlock = ::authenticate)
            } else {
                AuthenticatorScreen(
                    accounts = accounts,
                    backupEnabled = backupEnabled,
                    lastBackupMillis = lastBackupMillis,
                    onScanQr = ::scanQr,
                    onAddManual = ::addManual,
                    onDelete = ::deleteAccount,
                    onCopyCode = ::copyCode,
                    onConfigureBackup = ::configureBackup,
                    onBackupNow = ::backupNow,
                    onRestore = ::restoreFromDrive
                )
            }
        }

        window.decorView.post { authenticate() }
    }

    private fun authenticate() {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
        val manager = BiometricManager.from(this)
        if (manager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            unlocked = true
            return
        }

        val prompt = BiometricPrompt(
            this,
            mainExecutor(),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    unlocked = true
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("CHETO Authenticator")
            .setSubtitle("Confirma tu identidad para ver tus códigos 2FA")
            .setAllowedAuthenticators(authenticators)
            .setNegativeButtonText("Cancelar")
            .build()

        prompt.authenticate(info)
    }

    private fun scanQr() {
        GmsBarcodeScanning.getClient(this)
            .startScan()
            .addOnSuccessListener { barcode ->
                val raw = barcode.rawValue ?: return@addOnSuccessListener
                runCatching { OtpAuthParser.parse(raw) }
                    .onSuccess { account ->
                        saveAccount(account)
                        toast("Cuenta ${account.issuer} agregada")
                    }
                    .onFailure { toast(it.message ?: "QR TOTP no válido") }
            }
            .addOnFailureListener { toast("No se pudo abrir el lector QR") }
    }

    private fun addManual(issuer: String, label: String, secret: String) {
        runCatching {
            val normalized = secret.trim().replace(" ", "")
            TotpEngine.generate(normalized)
            AuthAccount(
                issuer = issuer.trim().ifBlank { "Cuenta" },
                label = label.trim().ifBlank { "Sin etiqueta" },
                secret = normalized
            )
        }.onSuccess {
            saveAccount(it)
            toast("Cuenta agregada")
        }.onFailure {
            toast("La clave TOTP no es válida")
        }
    }

    private fun saveAccount(account: AuthAccount) {
        accounts = accounts + account
        store.save(accounts)
        backupAfterChange()
    }

    private fun deleteAccount(account: AuthAccount) {
        accounts = accounts.filterNot { it.id == account.id }
        store.save(accounts)
        backupAfterChange()
    }

    private fun copyCode(code: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Código 2FA", code))
        toast("Código copiado")
    }

    private fun configureBackup(password: String) {
        runCatching {
            RecoveryKeyStore(this).configure(password.toCharArray())
        }.onFailure {
            toast(it.message ?: "No se pudo configurar la clave de recuperación")
            return
        }

        authorizeDrive { token ->
            backupSettings.driveEnabled = true
            backupEnabled = true
            BackupScheduler.schedule(this)
            uploadWithToken(token)
        }
    }

    private fun backupNow() {
        if (!backupEnabled) {
            toast("Primero configura Google Drive")
            return
        }
        authorizeDrive(::uploadWithToken)
    }

    private fun uploadWithToken(token: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val recovery = RecoveryKeyStore(this@MainActivity)
                val key = recovery.getKey() ?: error("Falta la clave local de recuperación")
                val salt = recovery.getSalt() ?: error("Falta el salt de recuperación")
                val encrypted = BackupCrypto.encrypt(store.exportPlainJson(), key, salt)
                DriveBackupClient().upload(token, encrypted)

                backupSettings.lastBackupEpochMillis = System.currentTimeMillis()
                backupSettings.lastError = null

                withContext(Dispatchers.Main) {
                    lastBackupMillis = backupSettings.lastBackupEpochMillis
                    toast("Backup cifrado guardado en Google Drive")
                }
            } catch (error: Exception) {
                backupSettings.lastError = error.message?.take(200)
                withContext(Dispatchers.Main) {
                    toast("No se pudo guardar el backup")
                }
            }
        }
    }

    private fun restoreFromDrive(password: String) {
        authorizeDrive { token ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val encrypted = DriveBackupClient().downloadLatest(token)
                        ?: error("No existe un backup de CHETO Authenticator")
                    val json = BackupCrypto.decryptWithPassword(encrypted, password.toCharArray())
                    store.importPlainJson(json)

                    RecoveryKeyStore(this@MainActivity).configure(password.toCharArray())
                    backupSettings.driveEnabled = true
                    BackupScheduler.schedule(this@MainActivity)

                    val restored = store.load()
                    withContext(Dispatchers.Main) {
                        accounts = restored
                        backupEnabled = true
                        toast("Backup restaurado: ${restored.size} cuentas")
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        toast("No se pudo restaurar. Verifica la contraseña de recuperación.")
                    }
                }
            }
        }
    }

    private fun authorizeDrive(action: (String) -> Unit) {
        pendingDriveAction = action
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(Scopes.DRIVE_APPFOLDER)))
            .build()

        val task: Task<AuthorizationResult> =
            Identity.getAuthorizationClient(this).authorize(request)

        task.addOnSuccessListener { result ->
            if (result.hasResolution()) {
                val pendingIntent = result.pendingIntent
                if (pendingIntent == null) {
                    pendingDriveAction = null
                    toast("Google Drive requiere autorización")
                    return@addOnSuccessListener
                }
                authorizationLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            } else {
                val token = result.accessToken
                if (!token.isNullOrBlank()) {
                    pendingDriveAction?.invoke(token)
                } else {
                    toast("No se obtuvo acceso a Google Drive")
                }
                pendingDriveAction = null
            }
        }.addOnFailureListener {
            pendingDriveAction = null
            toast("No se pudo solicitar acceso a Google Drive")
        }
    }

    private fun backupAfterChange() {
        if (backupEnabled) BackupScheduler.schedule(this)
    }

    private fun mainExecutor(): Executor =
        androidx.core.content.ContextCompat.getMainExecutor(this)

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
