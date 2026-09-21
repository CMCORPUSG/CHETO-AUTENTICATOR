package com.cmcorpusg.chetoauthenticator.ui

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cmcorpusg.chetoauthenticator.data.MobileVault

@Composable
internal fun UserProfileScreen(
    vault: MobileVault,
    onUpdate: (MobileVault) -> Unit,
    onPhoto: () -> Unit,
    googleConfigured: Boolean,
    microsoftConfigured: Boolean,
    onLinkGoogle: () -> Unit,
    onLinkMicrosoft: () -> Unit,
    onVerifyEmail: (String) -> Unit,
    onUnlinkIdentity: (String, String) -> Unit,
    onMessage: (String) -> Unit
) {
    var name by remember(vault.name) { mutableStateOf(vault.name) }
    var username by remember(vault.username) { mutableStateOf(vault.username) }
    var email by remember { mutableStateOf("") }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileAvatar(vault.name.ifBlank { "CHETO" }, vault.photo, 84)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(vault.name.ifBlank { "Perfil CHETO" }, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        vault.username.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Sin username",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        vault.emails.firstOrNull() ?: "Sin correo principal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${vault.accounts.size} cuentas · ${vault.categories.size} categorías",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                OutlinedButton(onClick = onPhoto, shape = ControlShape, modifier = Modifier.height(40.dp)) {
                    Icon(Icons.Rounded.PhotoCamera, null)
                    Text("  Cambiar foto")
                }
            }
        }

        SectionHeader("Datos personales", "Nombre visible y username son datos distintos")
        PremiumCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Nombre", name, { name = it })
                Field("Username", username, {
                    username = it.filter { ch -> ch.isLetterOrDigit() || ch in "._-" }.take(30)
                })
                Text(
                    "El username admite letras, números, punto, guion y guion bajo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        val normalizedName = name.trim()
                        val normalizedUser = username.trim().removePrefix("@")
                        when {
                            normalizedName.isBlank() -> onMessage("Escribe un nombre")
                            normalizedUser.isNotBlank() && normalizedUser.length < 3 -> onMessage("El username debe tener al menos 3 caracteres")
                            else -> {
                                onUpdate(vault.copy(name = normalizedName, username = normalizedUser))
                                onMessage("Perfil guardado")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = ControlShape
                ) {
                    Icon(Icons.Rounded.Save, null)
                    Text("  Guardar perfil")
                }
            }
        }

        SectionHeader("Identidad vinculada", "Puedes vincular varias cuentas Google y Microsoft")
        PremiumCard(Modifier.fillMaxWidth()) {
            Column {
                if (vault.linkedIdentities.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Aún no hay proveedores vinculados", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Vincular una cuenta verifica la propiedad del correo por Google o Microsoft. CHETO no guarda tu contraseña del proveedor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    vault.linkedIdentities.forEachIndexed { index, identity ->
                        val providerName = if (identity.provider == "google") "Google" else "Microsoft"
                        val providerLogo = ServiceCatalog.logoUrlFor(providerName).orEmpty()
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            Avatar(providerName, identity.photo.ifBlank { providerLogo }, 42)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(providerName, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Rounded.CheckCircle, "Cuenta verificada", tint = ChetoSuccess)
                                }
                                Text(identity.email, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                identity.displayName.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                            IconButton(onClick = { onUnlinkIdentity(identity.provider, identity.subject) }) {
                                Icon(Icons.Rounded.LinkOff, "Desvincular")
                            }
                        }
                        if (index < vault.linkedIdentities.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onLinkGoogle,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = ControlShape
            ) {
                Avatar("Google", ServiceCatalog.logoUrlFor("Google").orEmpty(), 24)
                Text(if (googleConfigured) "  + Google" else "  Configurar Google")
            }
            OutlinedButton(
                onClick = onLinkMicrosoft,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = ControlShape
            ) {
                Avatar("Microsoft", ServiceCatalog.logoUrlFor("Microsoft").orEmpty(), 24)
                Text(if (microsoftConfigured) "  + Microsoft" else "  Configurar Microsoft")
            }
        }

        SectionHeader("Correos", "Un correo nuevo se agrega solo después de verificar su propiedad")
        if (vault.emails.isNotEmpty()) {
            PremiumCard(Modifier.fillMaxWidth()) {
                Column {
                    vault.emails.forEachIndexed { index, value ->
                        val verified = vault.verifiedEmails.any { it.equals(value, true) }
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            Avatar(EmailProvider.nameFor(value), EmailProvider.logoUrlFor(value).orEmpty(), 42)
                            Column(Modifier.weight(1f)) {
                                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (verified) Icon(Icons.Rounded.CheckCircle, "Verificado", tint = ChetoSuccess)
                                    Text(
                                        buildString {
                                            if (index == 0) append("Principal · ")
                                            append(EmailProvider.nameFor(value))
                                            append(if (verified) " · Verificado" else " · Pendiente")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (verified) ChetoSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (index != 0) {
                                IconButton(onClick = { onUpdate(vault.copy(emails = listOf(value) + (vault.emails - value))) }) {
                                    Icon(Icons.Rounded.Star, "Hacer principal")
                                }
                            }
                            IconButton(
                                onClick = {
                                    onUpdate(
                                        vault.copy(
                                            emails = vault.emails.filterNot { it.equals(value, true) },
                                            verifiedEmails = vault.verifiedEmails.filterNot { it.equals(value, true) }
                                        )
                                    )
                                }
                            ) {
                                Icon(Icons.Rounded.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (index < vault.emails.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        PremiumCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconTile(Icons.Rounded.Email, null)
                    Text("Agregar correo verificado", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("correo@dominio.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = ControlShape
                )
                Button(
                    onClick = {
                        val normalized = email.trim().lowercase()
                        when {
                            !Patterns.EMAIL_ADDRESS.matcher(normalized).matches() ->
                                onMessage("Introduce un correo válido")
                            vault.emails.any { it.equals(normalized, true) } ->
                                onMessage("Ese correo ya está en tu perfil")
                            else -> {
                                onVerifyEmail(normalized)
                                email = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = ControlShape
                ) {
                    Icon(Icons.Rounded.Add, null)
                    Text("  Verificar y agregar")
                }
                Text(
                    "Gmail se verifica con Google y Outlook/Hotmail con Microsoft. Otros dominios requerirán el servicio de código por correo antes de agregarse.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
