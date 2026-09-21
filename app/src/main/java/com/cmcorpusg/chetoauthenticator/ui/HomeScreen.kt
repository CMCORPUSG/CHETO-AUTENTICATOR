package com.cmcorpusg.chetoauthenticator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmcorpusg.chetoauthenticator.backup.BackupSettings
import com.cmcorpusg.chetoauthenticator.core.TotpEngine
import com.cmcorpusg.chetoauthenticator.data.MobileAccount
import com.cmcorpusg.chetoauthenticator.data.MobileVault
import kotlinx.coroutines.delay

@Composable
internal fun HomeScreen(
    vault: MobileVault,
    onCopy: (String) -> Unit,
    onEdit: (MobileAccount) -> Unit,
    onDelete: (MobileAccount) -> Unit,
    onToggleFavorite: (MobileAccount) -> Unit,
    onBulkDelete: (Set<String>) -> Unit,
    onBulkCategory: (Set<String>, String) -> Unit,
    onBulkFavorite: (Set<String>, Boolean) -> Unit,
    onCategories: () -> Unit
) {
    val context = LocalContext.current
    val backupSettings = remember { BackupSettings(context) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Todos") }
    var sortMode by remember { mutableStateOf("Favoritos") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    var revealed by remember { mutableStateOf<String?>(null) }
    var manageAccounts by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showMoveCategory by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis() / 1000; delay(1000) } }
    LaunchedEffect(revealed) { if (revealed != null) { delay(10_000); revealed = null } }

    val filtered = vault.accounts.filter {
        (category == "Todos" || it.category == category) &&
            (!favoritesOnly || it.favorite) &&
            (it.issuer + " " + it.label + " " + it.category + " " + it.notes).contains(search, true)
    }
    val accounts = when (sortMode) {
        "A-Z" -> filtered.sortedWith(compareBy<MobileAccount> { it.issuer.lowercase() }.thenBy { it.label.lowercase() })
        "Categoría" -> filtered.sortedWith(compareBy<MobileAccount> { it.category.lowercase() }.thenBy { it.issuer.lowercase() })
        else -> filtered.sortedWith(compareByDescending<MobileAccount> { it.favorite }.thenBy { it.issuer.lowercase() }.thenBy { it.label.lowercase() })
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(Icons.Rounded.Shield, null, background = Color.White.copy(alpha = .58f))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Bóveda protegida", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${vault.accounts.size} cuentas · ${vault.accounts.count { it.favorite }} favoritas · " +
                                if (backupSettings.driveEnabled) "backup activo" else "backup manual",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(shape = RoundedCornerShape(50), color = ChetoSuccess.copy(alpha = .13f)) {
                        Text(
                            if (backupSettings.driveEnabled) "Protegida" else "Local",
                            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = ChetoSuccess
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                placeholder = { Text("Buscar servicio, cuenta, categoría o nota") },
                shape = ControlShape
            )
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("Todos") + vault.categories).forEach { item ->
                    val color = categoryColor(item, vault.categoryColors)
                    FilterChip(
                        selected = item == category,
                        onClick = { category = item },
                        label = { Text(item) },
                        colors = FilterChipDefaults.filterChipColors(containerColor = color.copy(alpha = .08f), selectedContainerColor = color.copy(alpha = .18f))
                    )
                }
                AssistChip(onClick = onCategories, label = { Text("Categorías") }, leadingIcon = { Icon(Icons.Rounded.Category, null, Modifier.size(17.dp)) })
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {
                        manageAccounts = !manageAccounts
                        selectedIds = emptySet()
                    },
                    label = { Text(if (manageAccounts) "Salir de selección" else "Gestionar") },
                    leadingIcon = { Icon(if (manageAccounts) Icons.Rounded.Close else Icons.Rounded.SelectAll, null, Modifier.size(17.dp)) }
                )
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly },
                    label = { Text("Solo favoritas") },
                    leadingIcon = { Icon(if (favoritesOnly) Icons.Rounded.Star else Icons.Rounded.StarBorder, null, Modifier.size(17.dp)) }
                )
                AssistChip(
                    onClick = {
                        sortMode = when (sortMode) {
                            "Favoritos" -> "A-Z"
                            "A-Z" -> "Categoría"
                            else -> "Favoritos"
                        }
                    },
                    label = { Text("Orden: $sortMode") },
                    leadingIcon = { Icon(Icons.Rounded.Sort, null, Modifier.size(17.dp)) }
                )
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        "${accounts.size} visibles",
                        Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (manageAccounts) {
            item {
                PremiumCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${selectedIds.size} seleccionada(s)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "Gestiona varias cuentas sin abrirlas una por una.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    selectedIds = if (selectedIds.size == accounts.size) {
                                        emptySet()
                                    } else {
                                        accounts.map { it.id }.toSet()
                                    }
                                }
                            ) {
                                Text(if (selectedIds.size == accounts.size && accounts.isNotEmpty()) "Ninguna" else "Todas")
                            }
                        }
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                enabled = selectedIds.isNotEmpty(),
                                onClick = {
                                    onBulkFavorite(selectedIds, true)
                                    selectedIds = emptySet()
                                },
                                label = { Text("Favoritas") },
                                leadingIcon = { Icon(Icons.Rounded.Star, null, Modifier.size(17.dp)) }
                            )
                            AssistChip(
                                enabled = selectedIds.isNotEmpty(),
                                onClick = { showMoveCategory = true },
                                label = { Text("Mover") },
                                leadingIcon = { Icon(Icons.Rounded.DriveFileMove, null, Modifier.size(17.dp)) }
                            )
                            AssistChip(
                                enabled = selectedIds.isNotEmpty(),
                                onClick = {
                                    onBulkDelete(selectedIds)
                                    selectedIds = emptySet()
                                    manageAccounts = false
                                },
                                label = { Text("Eliminar") },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null, Modifier.size(17.dp)) }
                            )
                        }
                    }
                }
            }
        }
        if (accounts.isEmpty()) {
            item {
                PremiumCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        IconTile(Icons.Rounded.Shield, null)
                        Spacer(Modifier.height(4.dp))
                        Text(if (vault.accounts.isEmpty()) "Tu bóveda está lista" else "Sin resultados", style = MaterialTheme.typography.titleMedium)
                        Text(if (vault.accounts.isEmpty()) "Toca + para agregar tu primera cuenta" else "Prueba otra búsqueda o categoría", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        items(accounts, key = { it.id }) { account ->
            val code = remember(account, now / account.period) {
                runCatching { TotpEngine.generate(account.secret, now, account.digits, account.period, account.algorithm) }.getOrDefault("------")
            }
            TotpCard(
                account = account,
                code = code,
                seconds = TotpEngine.secondsRemaining(now, account.period),
                accent = categoryColor(account.category, vault.categoryColors),
                hidden = vault.hideCodes && revealed != account.id,
                selected = selectedIds.contains(account.id),
                selectionMode = manageAccounts,
                onSelect = {
                    selectedIds = if (selectedIds.contains(account.id)) {
                        selectedIds - account.id
                    } else {
                        selectedIds + account.id
                    }
                },
                onCodeClick = { if (vault.hideCodes) revealed = account.id else onCopy(code) },
                onCopy = { onCopy(code) },
                onFavorite = { onToggleFavorite(account) },
                onEdit = { onEdit(account) },
                onDelete = { onDelete(account) }
            )
        }
    }

    if (showMoveCategory) {
        AlertDialog(
            onDismissRequest = { showMoveCategory = false },
            title = { Text("Mover cuentas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    vault.categories.forEach { target ->
                        TextButton(
                            onClick = {
                                onBulkCategory(selectedIds, target)
                                selectedIds = emptySet()
                                showMoveCategory = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(target, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveCategory = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TotpCard(
    account: MobileAccount,
    code: String,
    seconds: Int,
    accent: Color,
    hidden: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onSelect: () -> Unit,
    onCodeClick: () -> Unit,
    onCopy: () -> Unit,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    PremiumCard(
        Modifier.fillMaxWidth().clickable(enabled = selectionMode, onClick = onSelect)
    ) {
        Column(Modifier.padding(start = 15.dp, top = 14.dp, end = 10.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Avatar(account.issuer, account.photo, 44)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(account.issuer, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(account.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                if (selectionMode) {
                    Icon(
                        if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        if (selected) "Seleccionada" else "No seleccionada",
                        Modifier.size(24.dp),
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    IconButton(onClick = onFavorite, modifier = Modifier.size(37.dp)) {
                        Icon(
                            if (account.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            if (account.favorite) "Quitar de favoritas" else "Marcar favorita",
                            Modifier.size(20.dp),
                            tint = if (account.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = .12f)) {
                    Text(account.category, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (hidden) "••• •••" else code.chunked(if (account.digits == 6) 3 else 4).joinToString(" "),
                    modifier = Modifier.weight(1f).clickable(onClick = onCodeClick),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 7.dp)) {
                    CircularProgressIndicator(progress = { seconds.toFloat() / account.period }, modifier = Modifier.size(39.dp), strokeWidth = 3.dp, color = accent, trackColor = accent.copy(alpha = .14f))
                    Text("$seconds", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (!selectionMode) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(39.dp)) { Icon(Icons.Rounded.ContentCopy, "Copiar", Modifier.size(19.dp)) }
                    IconButton(onClick = onEdit, modifier = Modifier.size(39.dp)) { Icon(Icons.Rounded.Edit, "Editar", Modifier.size(19.dp)) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(39.dp)) { Icon(Icons.Rounded.Delete, "Eliminar", Modifier.size(19.dp), tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
