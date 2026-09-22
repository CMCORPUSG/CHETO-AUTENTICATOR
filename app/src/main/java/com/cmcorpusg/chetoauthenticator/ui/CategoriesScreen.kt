package com.cmcorpusg.chetoauthenticator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cmcorpusg.chetoauthenticator.data.MobileVault

@Composable
internal fun CategoryManagerScreen(
    vault: MobileVault,
    onUpdate: (MobileVault) -> Unit,
    onBack: () -> Unit,
    onMessage: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<String?>(null) }
    var selectedHex by remember { mutableStateOf(categoryPalette.keys.first()) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
            Column(Modifier.weight(1f)) {
                Text("Organiza tu bóveda", style = MaterialTheme.typography.titleLarge)
                Text("Colores y grupos para tus cuentas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(
            Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            InfoRow(Icons.Rounded.Category, "${vault.categories.size} categorías", "${vault.accounts.size} cuentas organizadas")
        }
        PremiumCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SectionHeader(if (editing == null) "Nueva categoría" else "Editar categoría")
                Field(if (editing == null) "Nombre" else "Nuevo nombre", text, { text = it })
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    categoryPalette.forEach { (hex, color) ->
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(color).clickable { selectedHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedHex == hex) Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Button(
                    onClick = {
                        val name = text.trim()
                        val old = editing
                        if (name.isBlank() || vault.categories.any { it.equals(name, true) && it != old }) {
                            onMessage("Elige un nombre único")
                        } else {
                            val categories = if (old == null) vault.categories + name else vault.categories.map { if (it == old) name else it }
                            val accounts = vault.accounts.map { if (it.category == old) it.copy(category = name) else it }
                            val colors = vault.categoryColors.toMutableMap().apply { if (old != null && old != name) remove(old); put(name, selectedHex) }
                            onUpdate(vault.copy(categories = categories, accounts = accounts, categoryColors = colors))
                            editing = null; text = ""; selectedHex = categoryPalette.keys.first()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = ControlShape
                ) { Text(if (editing == null) "Crear categoría" else "Guardar cambios") }
            }
        }

        SectionHeader("Tus categorías")
        vault.categories.forEach { category ->
            val accent = categoryColor(category, vault.categoryColors)
            PremiumCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(11.dp).clip(CircleShape).background(accent))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("${vault.accounts.count { it.category == category }} cuentas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (category != "Sin categoría") {
                        IconButton(onClick = { editing = category; text = category; selectedHex = categoryColorHex(category, vault.categoryColors) }, modifier = Modifier.size(38.dp)) {
                            Icon(Icons.Rounded.Edit, "Editar", Modifier.size(18.dp))
                        }
                        IconButton(onClick = {
                            val colors = vault.categoryColors.toMutableMap().apply { remove(category) }
                            onUpdate(vault.copy(categories = vault.categories - category, accounts = vault.accounts.map { if (it.category == category) it.copy(category = "Sin categoría") else it }, categoryColors = colors))
                            if (editing == category) { editing = null; text = ""; selectedHex = categoryPalette.keys.first() }
                        }, modifier = Modifier.size(38.dp)) {
                            Icon(Icons.Rounded.Delete, "Eliminar", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}
