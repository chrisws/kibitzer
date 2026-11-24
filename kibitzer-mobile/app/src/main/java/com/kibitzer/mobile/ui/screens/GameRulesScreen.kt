package com.kibitzer.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
fun GameRulesScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Game Rules",
            style = MaterialTheme.typography.headlineLarge
        )

        Text("Warlords", style = MaterialTheme.typography.titleMedium)
        Text(
            "Play the lowest card. If you can't, pick up the deck. " +
                "First to empty their hand wins.",
            style = MaterialTheme.typography.bodyLarge
        )

        Text("Free", style = MaterialTheme.typography.titleMedium)
        Text(
            "Play any card. If you can't, pick up the deck. " +
                "First to empty their hand wins.",
            style = MaterialTheme.typography.bodyLarge
        )

        Text("Crazy Eights", style = MaterialTheme.typography.titleMedium)
        Text(
            "Match by rank or suit. Eights are wild and change the suit. " +
                "First to empty their hand wins.",
            style = MaterialTheme.typography.bodyLarge
        )

        Text("Cheat", style = MaterialTheme.typography.titleMedium)
        Text(
            "Bid how many cards you can play. Play matching cards. " +
                "Others can call your bluff. Loser picks up the discard.",
            style = MaterialTheme.typography.bodyLarge
        )

        Text("Old Maid", style = MaterialTheme.typography.titleMedium)
        Text(
            "Discard pairs. Draw from opponents until you have a pair. " +
                "Last one with the Old Maid (single card) loses.",
            style = MaterialTheme.typography.bodyLarge
        )

        Text("Go Fish", style = MaterialTheme.typography.titleMedium)
        Text(
            "Ask opponents for cards to complete sets of four. " +
                "First to complete all sets wins.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}