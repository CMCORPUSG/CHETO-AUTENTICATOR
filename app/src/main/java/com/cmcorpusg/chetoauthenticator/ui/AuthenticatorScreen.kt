package com.cmcorpusg.chetoauthenticator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.cmcorpusg.chetoauthenticator.core.TotpEngine
import com.cmcorpusg.chetoauthenticator.data.AuthAccount
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
fun AuthenticatorScreen(
    accounts: List<AuthAccount>,
    backupEnabled: Boolean,
    lastBackupMillis: Long,
    onScanQr: () -> Unit,
    onAddManual: (String, String, String) -> Unit,
    onDelete: (AuthAccount) -> Unit,
    onCopyCode: (String) -> Unit,
    onConfigureBackup: (String) -> Unit,
    onBackupNow: () -> Unit,
    onRestore: (String) -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var manualDialog by remember { mutableStateOf(false) }
    var backupDialog by remember { mutableStateOf(false) }
    var restoreDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("CHETO Authenticator") })
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onScanQr,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Escanear QR")
                        }
                        OutlinedButton(
                            onClick = { manualDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clave manual")
                        }
                    }
                }

                if (accounts.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp)) {
                                Text(
                                    "Aún no hay cuentas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Agrega Google, Microsoft, Twitch, Kick u otra plataforma TOTP escaneando su QR o ingresando la clave secreta."
                                )
                            }
                        }
                    }
                }

                items(accounts, key = { it.id }) { account ->
                    AccountCard(
                        account = account,
                        epochMillis = now,
                        onCopyCode = onCopyCode,
                        onDelete = { onDelete(account) }
                    )
                }

                item {
                    HorizontalDivider()
                    Text(
                        "Copia de seguridad",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (backupEnabled) "Google Drive: activado" else "Google Drive: no configurado"
                    )
                    if (lastBackupMillis > 0) {
                        Text(
                            "Último backup: " +
                                DateFormat.getDateTimeInstance().format(Date(lastBackupMillis)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { backupDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (backupEnabled) "Cambiar clave" else "Configurar")
                        }
                        OutlinedButton(
                            onClick = onBackupNow,
                            enabled = backupEnabled,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Backup ahora")
                        }
                    }
                    OutlinedButton(
                        onClick = { restoreDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restaurar desde Google Drive")
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    if (manualDialog) {
        ManualAccountDialog(
            onDismiss = { manualDialog = false },
            onSave = { issuer, label, secret ->
                onAddManual(issuer, label, secret)
                manualDialog = false
            }
        )
    }

    if (backupDialog) {
        RecoveryPasswordDialog(
            title = "Configurar backup cifrado",
            confirmLabel = "Guardar y conectar Drive",
            requireConfirmation = true,
            onDismiss = { backupDialog = false },
            onConfirm = {
                onConfigureBackup(it)
                backupDialog = false
            }
        )
    }

    if (restoreDialog) {
        RecoveryPasswordDialog(
            title = "Restaurar backup",
            confirmLabel = "Restaurar",
            requireConfirmation = false,
            onDismiss = { restoreDialog = false },
            onConfirm = {
                onRestore(it)
                restoreDialog = false
            }
        )
    }
}

@Composable
private fun AccountCard(
    account: AuthAccount,
    epochMillis: Long,
    onCopyCode: (String) -> Unit,
    onDelete: () -> Unit
) {
    val epochSeconds = epochMillis / 1000
    val code = runCatching {
        TotpEngine.generate(
            secretBase32 = account.secret,
            epochSeconds = epochSeconds,
            digits = account.digits,
            period = account.period,
            algorithm = account.algorithm
        )
    }.getOrDefault("------")
    val remaining = TotpEngine.secondsRemaining(epochSeconds, account.period)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                account.issuer,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(account.label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                formatCode(code),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Cambia en $remaining s", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onCopyCode(code) }) {
                    Text("Copiar")
                }
                TextButton(onClick = onDelete) {
                    Text("Eliminar")
                }
            }
        }
    }
}

private fun formatCode(code: String): String {
    if (code.length == 6) return code.chunked(3).joinToString(" ")
    if (code.length == 8) return code.chunked(4).joinToString(" ")
    return code
}

@Composable
private fun ManualAccountDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var issuer by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar cuenta manual") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = issuer,
                    onValueChange = { issuer = it },
                    label = { Text("Servicio: Google, Twitch, Kick…") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Cuenta o correo") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("Clave secreta TOTP") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = secret.isNotBlank(),
                onClick = { onSave(issuer, label, secret) }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun RecoveryPasswordDialog(
    title: String,
    confirmLabel: String,
    requireConfirmation: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }

    val valid = password.length >= 10 &&
        (!requireConfirmation || password == confirmation)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Esta contraseña permite recuperar tus cuentas en otro celular. No se guarda en GitHub ni en Google Drive."
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña de recuperación") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                if (requireConfirmation) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        label = { Text("Repetir contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
                if (password.isNotEmpty() && password.length < 10) {
                    Text(
                        "Usa al menos 10 caracteres.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onConfirm(password) }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
