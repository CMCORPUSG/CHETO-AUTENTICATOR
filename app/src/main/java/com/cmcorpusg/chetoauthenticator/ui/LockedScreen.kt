package com.cmcorpusg.chetoauthenticator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LockedScreen(onUnlock: () -> Unit) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "CHETO Authenticator",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "Tus códigos están protegidos localmente.",
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Button(onClick = onUnlock) {
                Text("Desbloquear")
            }
        }
    }
}
