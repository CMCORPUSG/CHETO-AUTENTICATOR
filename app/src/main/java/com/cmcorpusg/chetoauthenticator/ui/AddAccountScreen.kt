package com.cmcorpusg.chetoauthenticator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cmcorpusg.chetoauthenticator.data.MobileAccount

@Composable
internal fun AddAccountScreen(
    onDismiss: () -> Unit,
    onScan: (Boolean) -> Unit,
    onManual: (MobileAccount) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val services = remember(query) {
        ServiceCatalog.suggestions.filter { it.contains(query.trim(), ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding().imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
                    Column(Modifier.weight(1f)) {
                        Text("Agregar cuenta", style = MaterialTheme.typography.headlineSmall)
                        Text("QR individual, migración o clave manual", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = ScreenPadding,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AddMethodCard(Icons.Rounded.QrCodeScanner, "Escanear QR", "Cuenta o migración", Modifier.weight(1f)) { onScan(false) }
                            AddMethodCard(Icons.Rounded.PhotoLibrary, "Desde imagen", "Cuenta o migración", Modifier.weight(1f)) { onScan(true) }
                        }
                    }
                    item {
                        Text(
                            "Compatible con QR TOTP y exportaciones de Google Authenticator con varias cuentas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        Card(
                            Modifier.fillMaxWidth().clickable { onManual(MobileAccount()) },
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            InfoRow(Icons.Rounded.EditNote, "Introducir manualmente", "Añade la clave y configura el servicio") {
                                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(2.dp))
                        SectionHeader("Servicios", "Selecciona uno para completar su identidad y logo")
                    }
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Rounded.Search, null) },
                            placeholder = { Text("Buscar servicio") },
                            shape = ControlShape
                        )
                    }
                    items(services, key = { it }) { service ->
                        PremiumCard(Modifier.fillMaxWidth().clickable {
                            onManual(MobileAccount(issuer = service, photo = ServiceCatalog.logoUrlFor(service).orEmpty()))
                        }) {
                            Row(
                                Modifier.fillMaxWidth().padding(13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Avatar(service, ServiceCatalog.logoUrlFor(service).orEmpty(), 44)
                                Column(Modifier.weight(1f)) {
                                    Text(service, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text(ServiceCatalog.domainFor(service).orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMethodCard(icon: ImageVector, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    PremiumCard(modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            IconTile(icon, null)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
