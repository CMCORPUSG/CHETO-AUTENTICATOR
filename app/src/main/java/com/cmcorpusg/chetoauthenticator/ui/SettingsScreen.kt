package com.cmcorpusg.chetoauthenticator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cmcorpusg.chetoauthenticator.data.MobileVault

@Composable
internal fun SettingsPage(
    vault: MobileVault,
    biometricReady: Boolean,
    onUpdate: (MobileVault) -> Unit,
    onCategories: () -> Unit,
    onPin: () -> Unit,
    onBiometricSetup: () -> Unit,
    onSecurityCenter: () -> Unit,
    onSensitiveAction: (String, () -> Unit) -> Unit
) {
    var showAutoLock by remember { mutableStateOf(false) }
    val autoLockLabel = when (vault.lockTimeoutSeconds) {
        0 -> "Inmediatamente"
        30 -> "Después de 30 segundos"
        60 -> "Después de 1 minuto"
        300 -> "Después de 5 minutos"
        else -> "Inmediatamente"
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(Modifier.fillMaxWidth(), shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            InfoRow(Icons.Rounded.Security, "Bóveda local protegida", "PIN · Android Keystore · bloqueo automático")
        }

        SettingsGroup("Seguridad") {
            ActionRow(
                Icons.Rounded.Security,
                "Centro de seguridad",
                "Estado de biometría, bloqueo, capturas y backup",
                onSecurityCenter
            )
            GroupDivider()
            ToggleRow(
                Icons.Rounded.Fingerprint,
                "Biometría",
                when {
                    vault.biometric && biometricReady -> "Huella/biometría confirmada y lista"
                    vault.biometric && !biometricReady -> "Falta registrar biometría en Android"
                    biometricReady -> "Disponible · activa y confirma tu identidad"
                    else -> "Toca para registrar una huella en Android"
                },
                vault.biometric && biometricReady
            ) { enabled ->
                if (enabled) {
                    onBiometricSetup()
                } else {
                    onSensitiveAction("Desactivar biometría") {
                        onUpdate(vault.copy(biometric = false))
                    }
                }
            }
            if (!biometricReady) {
                GroupDivider()
                ActionRow(
                    Icons.Rounded.Fingerprint,
                    "Registrar huella",
                    "Abre el asistente seguro de Android",
                    onBiometricSetup
                )
            }
            GroupDivider()
            ActionRow(
                Icons.Rounded.Schedule,
                "Bloqueo automático",
                autoLockLabel
            ) { showAutoLock = true }
            GroupDivider()
            ToggleRow(Icons.Rounded.VisibilityOff, "Ocultar códigos", "Revelar cada TOTP al tocar", vault.hideCodes) { onUpdate(vault.copy(hideCodes = it)) }
            GroupDivider()
            ToggleRow(Icons.Rounded.Screenshot, "Permitir capturas", "Desactiva la protección de pantalla", vault.screenshots) { enabled ->
                if (enabled) {
                    onSensitiveAction("Permitir capturas") {
                        onUpdate(vault.copy(screenshots = true))
                    }
                } else {
                    onUpdate(vault.copy(screenshots = false))
                }
            }
        }
        SettingsGroup("Apariencia") {
            ToggleRow(Icons.Rounded.DarkMode, "Modo oscuro", "Tema oscuro en toda la app", vault.dark) { onUpdate(vault.copy(dark = it)) }
        }
        SettingsGroup("Administración") {
            ActionRow(Icons.Rounded.Key, "Cambiar PIN", "Actualiza tus 6 dígitos", onPin)
            GroupDivider()
            ActionRow(Icons.Rounded.Category, "Categorías", "Organiza cuentas y colores", onCategories)
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CHETO Authenticator 0.10.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Android nativo · Kotlin + Compose", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showAutoLock) {
        AlertDialog(
            onDismissRequest = { showAutoLock = false },
            title = { Text("Bloqueo automático") },
            text = {
                Column {
                    listOf(
                        0 to "Inmediatamente",
                        30 to "30 segundos",
                        60 to "1 minuto",
                        300 to "5 minutos"
                    ).forEach { (seconds, label) ->
                        TextButton(
                            onClick = {
                                val apply = {
                                    onUpdate(vault.copy(lockTimeoutSeconds = seconds))
                                    showAutoLock = false
                                }
                                if (seconds > vault.lockTimeoutSeconds) {
                                    onSensitiveAction("Aumentar tiempo de desbloqueo") { apply() }
                                } else {
                                    apply()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAutoLock = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 3.dp))
        PremiumCard(Modifier.fillMaxWidth()) { Column { content() } }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        IconTile(icon, null)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        IconTile(icon, null)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(Modifier.padding(start = 67.dp), color = MaterialTheme.colorScheme.outlineVariant)
}
