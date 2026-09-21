package com.cmcorpusg.chetoauthenticator.ui

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
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cmcorpusg.chetoauthenticator.data.MobileVault
import com.cmcorpusg.chetoauthenticator.data.TrashedAccount

@Composable
internal fun TrashScreen(
    vault: MobileVault,
    onRestore: (TrashedAccount) -> Unit,
    onDeleteForever: (TrashedAccount) -> Unit,
    onEmptyTrash: () -> Unit
) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader(
            "Papelera protegida",
            "Las cuentas eliminadas permanecen cifradas hasta que las restaures o las borres definitivamente."
        )

        PremiumCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "${vault.trash.size} cuenta(s) en papelera",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Eliminar definitivamente requiere una nueva confirmación de seguridad.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (vault.trash.isEmpty()) {
            PremiumCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconTile(Icons.Rounded.DeleteSweep, null)
                    Text("La papelera está vacía", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Las cuentas que elimines aparecerán aquí antes del borrado definitivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            vault.trash
                .sortedByDescending { it.deletedAtEpochMillis }
                .forEach { trashed ->
                    val account = trashed.account
                    PremiumCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(11.dp)
                            ) {
                                Avatar(account.issuer, account.photo, 44)
                                Column(Modifier.weight(1f)) {
                                    Text(account.issuer, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        account.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Eliminada: " + LimaClock.nowLabel(trashed.deletedAtEpochMillis),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onRestore(trashed) },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = ControlShape
                                ) {
                                    Icon(Icons.Rounded.Restore, contentDescription = null)
                                    Text("  Restaurar")
                                }
                                OutlinedButton(
                                    onClick = { onDeleteForever(trashed) },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = ControlShape
                                ) {
                                    Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                                    Text("  Borrar")
                                }
                            }
                        }
                    }
                }

            OutlinedButton(
                onClick = onEmptyTrash,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = ControlShape
            ) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                Text("  Vaciar papelera")
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}
