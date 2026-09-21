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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onCategories: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Todos") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    var revealed by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis() / 1000; delay(1000) } }
    LaunchedEffect(revealed) { if (revealed != null) { delay(10_000); revealed = null } }

    val accounts = vault.accounts.filter {
        (category == "Todos" || it.category == category) &&
            (it.issuer + " " + it.label).contains(search, true)
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
                        Text("${vault.accounts.size} cuentas · códigos offline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(shape = RoundedCornerShape(50), color = ChetoSuccess.copy(alpha = .13f)) {
                        Text("Activa", Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = ChetoSuccess)
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
                placeholder = { Text("Buscar cuenta o servicio") },
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
                onCodeClick = { if (vault.hideCodes) revealed = account.id else onCopy(code) },
                onCopy = { onCopy(code) },
                onEdit = { onEdit(account) },
                onDelete = { onDelete(account) }
            )
        }
    }
}

@Composable
private fun TotpCard(
    account: MobileAccount,
    code: String,
    seconds: Int,
    accent: Color,
    hidden: Boolean,
    onCodeClick: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 15.dp, top = 14.dp, end = 10.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Avatar(account.issuer, account.photo, 44)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(account.issuer, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(account.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onCopy, modifier = Modifier.size(39.dp)) { Icon(Icons.Rounded.ContentCopy, "Copiar", Modifier.size(19.dp)) }
                IconButton(onClick = onEdit, modifier = Modifier.size(39.dp)) { Icon(Icons.Rounded.Edit, "Editar", Modifier.size(19.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(39.dp)) { Icon(Icons.Rounded.Delete, "Eliminar", Modifier.size(19.dp), tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
