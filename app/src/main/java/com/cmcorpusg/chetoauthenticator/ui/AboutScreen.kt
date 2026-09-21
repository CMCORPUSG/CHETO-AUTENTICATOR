package com.cmcorpusg.chetoauthenticator.ui

import android.os.Build
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
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cmcorpusg.chetoauthenticator.BuildConfig
import com.cmcorpusg.chetoauthenticator.backup.BackupSettings
import com.cmcorpusg.chetoauthenticator.data.MobileVault

@Composable
internal fun AboutScreen(
    vault: MobileVault,
    biometricReady: Boolean,
    googleConfigured: Boolean,
    microsoftConfigured: Boolean
) {
    val context = LocalContext.current
    val backup = BackupSettings(context)
    val linkedGoogle = vault.linkedIdentities.any { it.provider == "google" }
    val linkedMicrosoft = vault.linkedIdentities.any { it.provider == "microsoft" }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconTile(Icons.Rounded.Info, null)
                    Column {
                        Text("CHETO Authenticator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Versión ${BuildConfig.VERSION_NAME} · código ${BuildConfig.VERSION_CODE}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "Autenticador TOTP local con bóveda cifrada, biometría, importación QR y recuperación cifrada.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        SectionHeader("Estado de la bóveda")
        PremiumCard(Modifier.fillMaxWidth()) {
            Column {
                DiagnosticRow(Icons.Rounded.Security, "Cuentas TOTP", "${vault.accounts.size}")
                GroupDividerLocal()
                DiagnosticRow(Icons.Rounded.CheckCircle, "Categorías", "${vault.categories.size}")
                GroupDividerLocal()
                DiagnosticRow(
                    Icons.Rounded.Fingerprint,
                    "Biometría",
                    if (vault.biometric && biometricReady) "Activa" else "PIN disponible"
                )
                GroupDividerLocal()
                DiagnosticRow(
                    Icons.Rounded.Backup,
                    "Backup automático",
                    if (backup.driveEnabled) "Activo" else "No configurado"
                )
            }
        }

        SectionHeader("Identidad")
        PremiumCard(Modifier.fillMaxWidth()) {
            Column {
                DiagnosticRow(
                    Icons.Rounded.Cloud,
                    "Google",
                    when {
                        linkedGoogle -> "Vinculado"
                        googleConfigured -> "Configurado · sin vincular"
                        else -> "Falta OAuth local"
                    }
                )
                GroupDividerLocal()
                DiagnosticRow(
                    Icons.Rounded.Cloud,
                    "Microsoft",
                    when {
                        linkedMicrosoft -> "Vinculado"
                        microsoftConfigured -> "Configurado · sin vincular"
                        else -> "Falta Entra/MSAL local"
                    }
                )
            }
        }

        SectionHeader("Dispositivo")
        PremiumCard(Modifier.fillMaxWidth()) {
            Column {
                DiagnosticRow(
                    Icons.Rounded.PhoneAndroid,
                    "Android",
                    "${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}"
                )
                GroupDividerLocal()
                DiagnosticRow(
                    Icons.Rounded.PhoneAndroid,
                    "Equipo",
                    "${Build.MANUFACTURER} ${Build.MODEL}"
                )
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Privacidad por diseño", fontWeight = FontWeight.Bold)
                Text(
                    "Los códigos TOTP se generan en el dispositivo. CHETO no necesita enviar secretos TOTP para calcularlos.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Las huellas permanecen bajo control de Android y las copias .cheto se cifran antes de guardarse.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DiagnosticRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        IconTile(icon, null)
        Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = ControlShape
        ) {
            Text(
                value,
                Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GroupDividerLocal() {
    androidx.compose.material3.HorizontalDivider(
        Modifier.padding(start = 67.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
