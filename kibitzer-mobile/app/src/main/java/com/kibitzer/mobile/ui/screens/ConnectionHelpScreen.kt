package com.kibitzer.mobile.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConnectionHelpScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Connection Help",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Kibitzer uses Bluetooth Low Energy (BLE) to connect players. " +
                "One device acts as the host and others connect as clients.",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Getting Started",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "1. Enable Bluetooth on all devices\n" +
                "2. One player opens Kibitzer and starts hosting\n" +
                "3. Other players open Kibitzer and scan for the host\n" +
                "4. Connect to the host when it appears\n" +
                "5. Set your nickname and choose a game\n" +
                "6. Start playing!",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Troubleshooting",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "- Make sure Bluetooth is enabled\n" +
                "- Keep devices within 10 meters\n" +
                "- Restart the app if connection drops\n" +
                "- Only one host can be active at a time\n" +
                "- Maximum 6 players per game",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}