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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Email
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
    onMessage: (String) -> Unit
) {
    var name by remember(vault.name) { mutableStateOf(vault.name) }
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
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Avatar(vault.name, vault.photo, 72)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(vault.name.ifBlank { "Perfil CHETO" }, style = MaterialTheme.typography.headlineSmall)
                    Text(vault.emails.firstOrNull() ?: "Sin correo principal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${vault.accounts.size} cuentas · ${vault.categories.size} categorías", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                OutlinedButton(onClick = onPhoto, shape = ControlShape, modifier = Modifier.height(40.dp)) {
                    Icon(Icons.Rounded.PhotoCamera, null)
                    Text("  Cambiar foto")
                }
            }
        }

        SectionHeader("Datos personales")
        PremiumCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Nombre", name, { name = it })
                Button(onClick = {
                    if (name.isBlank()) onMessage("Escribe un nombre") else { onUpdate(vault.copy(name = name.trim())); onMessage("Perfil guardado") }
                }, modifier = Modifier.fillMaxWidth().height(44.dp), shape = ControlShape) {
                    Icon(Icons.Rounded.Save, null)
                    Text("  Guardar nombre")
                }
            }
        }

        SectionHeader("Correos", "Se guardan localmente en tu perfil")
        if (vault.emails.isNotEmpty()) {
            PremiumCard(Modifier.fillMaxWidth()) {
                Column {
                    vault.emails.forEachIndexed { index, value ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                            Avatar(EmailProvider.nameFor(value), EmailProvider.logoUrlFor(value).orEmpty(), 42)
                            Column(Modifier.weight(1f)) {
                                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(if (index == 0) "Principal · ${EmailProvider.nameFor(value)}" else EmailProvider.nameFor(value), style = MaterialTheme.typography.bodySmall, color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (index != 0) IconButton(onClick = { onUpdate(vault.copy(emails = listOf(value) + (vault.emails - value))) }) { Icon(Icons.Rounded.Star, "Hacer principal") }
                            IconButton(onClick = { onUpdate(vault.copy(emails = vault.emails - value)) }) { Icon(Icons.Rounded.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error) }
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
                    Text("Agregar correo", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("correo@dominio.com") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = ControlShape)
                Button(onClick = {
                    val normalized = email.trim()
                    if (!Patterns.EMAIL_ADDRESS.matcher(normalized).matches() || vault.emails.any { it.equals(normalized, true) }) onMessage("Introduce un correo válido y no repetido")
                    else { onUpdate(vault.copy(emails = vault.emails + normalized)); email = "" }
                }, modifier = Modifier.fillMaxWidth().height(44.dp), shape = ControlShape) {
                    Icon(Icons.Rounded.Add, null)
                    Text("  Agregar al perfil")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
