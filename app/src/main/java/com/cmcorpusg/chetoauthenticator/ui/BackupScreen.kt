package com.cmcorpusg.chetoauthenticator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cmcorpusg.chetoauthenticator.backup.BackupSettings
import com.cmcorpusg.chetoauthenticator.backup.RecoveryKeyStore
import com.cmcorpusg.chetoauthenticator.data.MobileVault

@Composable
internal fun BackupPage(
    vault: MobileVault,
    busy: Boolean,
    action: (String) -> Unit,
    onDisableAuto: ((() -> Unit)) -> Unit
) {
    val context = LocalContext.current
    val settings = remember { BackupSettings(context) }
    val recovery = remember { RecoveryKeyStore(context) }
    var automatic by remember { mutableStateOf(settings.driveEnabled) }
    LaunchedEffect(busy) { automatic = settings.driveEnabled }
    val lastBackup = if (settings.lastBackupEpochMillis > 0) LimaClock.nowLabel(settings.lastBackupEpochMillis) else "Sin copias todavía"
    val lastVerified = if (settings.lastVerifiedBackupEpochMillis > 0) {
        LimaClock.nowLabel(settings.lastVerifiedBackupEpochMillis)
    } else {
        "Nunca verificada"
    }
    val backupAgeHours = if (settings.lastBackupEpochMillis > 0) {
        (System.currentTimeMillis() - settings.lastBackupEpochMillis).coerceAtLeast(0L) / 3_600_000L
    } else Long.MAX_VALUE
    val freshness = when {
        settings.lastBackupEpochMillis <= 0L -> "Sin copia"
        backupAgeHours < 36 -> "Reciente"
        backupAgeHours < 72 -> "Revisar"
        else -> "Desactualizada"
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader("Protege tu bóveda", "Copias cifradas que solo tú puedes abrir")
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = if (automatic) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(if (automatic) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff, null)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(if (automatic) "Backup automático activo" else "Backup automático inactivo", style = MaterialTheme.typography.titleMedium)
                    Text(lastBackup, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(50), color = if (automatic) ChetoSuccess.copy(alpha = .13f) else MaterialTheme.colorScheme.outlineVariant) {
                    Text(if (automatic) "Activo" else "Manual", Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = if (automatic) ChetoSuccess else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        settings.lastError?.takeIf { it.isNotBlank() }?.let {
            Text("Último aviso: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        PremiumCard(Modifier.fillMaxWidth()) {
            Column {
                InfoRow(
                    Icons.Rounded.VerifiedUser,
                    "Estado de recuperación",
                    "${vault.accounts.size} cuentas · $freshness"
                )
                androidx.compose.material3.HorizontalDivider(
                    Modifier.padding(start = 67.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                InfoRow(
                    Icons.Rounded.Lock,
                    "Clave para backup automático",
                    if (recovery.hasConfiguredKey()) "Configurada en este dispositivo" else "Se configura al conectar Drive"
                )
                androidx.compose.material3.HorizontalDivider(
                    Modifier.padding(start = 67.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                InfoRow(
                    Icons.Rounded.Verified,
                    "Última verificación",
                    lastVerified
                )
            }
        }

        SectionHeader("Copia local")
        BackupOptionCard(
            icon = Icons.Rounded.Backup,
            title = "Archivo .cheto",
            subtitle = "Guárdalo en tu PC, USB o almacenamiento seguro",
            primaryLabel = "Exportar copia",
            secondaryLabel = "Restaurar archivo",
            enabled = !busy,
            onPrimary = { action("export") },
            onSecondary = { action("restore") }
        )
        OutlinedButton(
            onClick = { action("restoreMerge") },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(43.dp),
            shape = ControlShape
        ) {
            Text("Fusionar copia sin reemplazar tu bóveda")
        }
        OutlinedButton(
            onClick = { action("verify") },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(43.dp),
            shape = ControlShape
        ) {
            Icon(Icons.Rounded.Verified, contentDescription = null)
            Text("  Verificar copia sin restaurar")
        }

        SectionHeader("Nube privada")
        BackupOptionCard(
            icon = Icons.Rounded.Cloud,
            title = "Google Drive",
            subtitle = "Cifrado antes de subir; CHETO solo usa su carpeta privada",
            primaryLabel = if (automatic) "Sincronizar ahora" else "Conectar Drive",
            secondaryLabel = "Restaurar desde Drive",
            enabled = !busy,
            onPrimary = { action("drive") },
            onSecondary = { action("driveRestore") }
        )
        if (automatic) {
            OutlinedButton(
                onClick = { action("driveVerify") },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(43.dp),
                shape = ControlShape
            ) {
                Icon(Icons.Rounded.Verified, contentDescription = null)
                Text("  Verificar última copia de Drive")
            }
            OutlinedButton(
                onClick = { action("driveRestoreMerge") },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(43.dp),
                shape = ControlShape
            ) {
                Text("Fusionar última copia de Drive")
            }
        }
        if (automatic) {
            OutlinedButton(
                onClick = { onDisableAuto { automatic = false } },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(43.dp),
                shape = ControlShape
            ) { Text("Desactivar backup automático") }
        }

        Card(Modifier.fillMaxWidth(), shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.secondary)
                Text("Guarda tu contraseña de recuperación fuera del teléfono. No podemos recuperarla por ti.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BackupOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    primaryLabel: String,
    secondaryLabel: String,
    enabled: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                IconTile(icon, null)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPrimary, enabled = enabled, modifier = Modifier.weight(1f).height(43.dp), shape = ControlShape) {
                    Icon(Icons.Rounded.Upload, null)
                    Text("  $primaryLabel", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(onClick = onSecondary, enabled = enabled, modifier = Modifier.weight(1f).height(43.dp), shape = ControlShape) {
                    Icon(Icons.Rounded.Download, null)
                    Text("  $secondaryLabel", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
