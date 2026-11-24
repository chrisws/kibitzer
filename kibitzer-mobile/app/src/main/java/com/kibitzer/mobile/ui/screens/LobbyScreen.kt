package com.kibitzer.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kibitzer.mobile.game.Game
import com.kibitzer.mobile.model.GameViewModel
import com.kibitzer.mobile.ui.components.PlayerAvatar

@Composable
fun LobbyScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onGameStart: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var nickname by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Lobby",
            style = MaterialTheme.typography.headlineLarge
        )

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("Nickname") },
            modifier = Modifier.fillMaxWidth()
        )

        if (nickname.isNotBlank()) {
            Button(onClick = { viewModel.setNickname(nickname) }) {
                Text("Set Name")
            }
        }

        Text(
            text = "Game",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Game.entries.forEach { game ->
                val selected = state.game == game
                OutlinedButton(
                    onClick = { viewModel.setGame(game) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(game.name.replace("_", " "), maxLines = 1)
                }
            }
        }

        Text(
            text = "Players",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(state.players) { player ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerAvatar(
                            nickname = player.nickname,
                            active = player.active,
                            isAi = player.isAi
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = player.nickname.ifBlank { "Empty" },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (player.active) {
                                Text(
                                    text = "${player.handSize} cards",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(onClick = onGameStart, modifier = Modifier.weight(2f)) {
                Text("Start Game")
            }
        }
    }
}