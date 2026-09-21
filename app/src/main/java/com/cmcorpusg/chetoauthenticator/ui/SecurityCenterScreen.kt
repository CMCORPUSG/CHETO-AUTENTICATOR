package com.cmcorpusg.chetoauthenticator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cmcorpusg.chetoauthenticator.backup.BackupSettings
import com.cmcorpusg.chetoauthenticator.data.MobileVault

@Composable
internal fun SecurityCenterScreen(
    vault: MobileVault,
    biometricReady: Boolean,
    onLockNow: () -> Unit
) {
    val context = LocalContext.current
    val backup = BackupSettings(context)
    val protectedCount = listOf(
        vault.biometric && biometricReady,
        !vault.screenshots,
        vault.lockTimeoutSeconds <= 60,
        vault.hideCodes,
        backup.driveEnabled
    ).count { it }

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
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconTile(Icons.Rounded.Security, null)
                    Column {
                        Text("Centro de seguridad", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "$protectedCount de 5 protecciones recomendadas activas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "CHETO protege la bóveda local, los códigos TOTP y las copias cifradas. Revisa aquí el estado real del dispositivo.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        SecurityStatus(
            Icons.Rounded.Fingerprint,
            "Biometría",
            if (vault.biometric && biometricReady) "Activa y confirmada" else "PIN disponible como respaldo",
            vault.biometric && biometricReady
        )
        SecurityStatus(
            Icons.Rounded.Schedule,
            "Bloqueo automático",
            when (vault.lockTimeoutSeconds) {
                0 -> "Inmediato al salir"
                30 -> "30 segundos"
                60 -> "1 minuto"
                300 -> "5 minutos"
                else -> "Inmediato"
            },
            vault.lockTimeoutSeconds <= 60
        )
        SecurityStatus(
            Icons.Rounded.Screenshot,
            "Protección de pantalla",
            if (vault.screenshots) "Capturas permitidas" else "Capturas y vista reciente protegidas",
            !vault.screenshots
        )
        SecurityStatus(
            Icons.Rounded.VisibilityOff,
            "Códigos ocultos",
            if (vault.hideCodes) "Se revelan temporalmente al tocar" else "Visibles mientras la bóveda está abierta",
            vault.hideCodes
        )
        SecurityStatus(
            Icons.Rounded.Backup,
            "Recuperación",
            if (backup.driveEnabled) "Backup automático de Drive configurado" else "Usa copia local .cheto o conecta Drive",
            backup.driveEnabled
        )

        Button(onClick = onLockNow, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Lock, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Bloquear CHETO ahora")
        }
        Text(
            "Las huellas permanecen bajo control de Android. CHETO no almacena plantillas biométricas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SecurityStatus(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    protected: Boolean
) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            IconTile(icon, null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (protected) "Protegido" else "Revisar",
                style = MaterialTheme.typography.labelMedium,
                color = if (protected) ChetoSuccess else MaterialTheme.colorScheme.error
            )
        }
    }
}
