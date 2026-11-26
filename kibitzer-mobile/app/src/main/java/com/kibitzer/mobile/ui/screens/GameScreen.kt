package com.kibitzer.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kibitzer.mobile.game.Card
import com.kibitzer.mobile.model.GameViewModel
import com.kibitzer.mobile.ui.components.ChatPanel
import com.kibitzer.mobile.ui.components.DeckView
import com.kibitzer.mobile.ui.components.HandRow
import com.kibitzer.mobile.ui.components.PileView
import com.kibitzer.mobile.ui.components.TurnIndicator

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onGameOver: () -> Unit,
    onHelp: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var chatInput by remember { mutableStateOf("") }
    var selectedCards by remember { mutableStateOf(setOf<Card>()) }
    var showChat by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.game.name.replace("_", " "),
                style = MaterialTheme.typography.titleLarge
            )
            OutlinedButton(onClick = onHelp) {
                Text("Help")
            }
        }

        // Turn indicator
        TurnIndicator(
            isMyTurn = state.myTurn,
            currentTurn = state.currentTurn,
            players = state.players
        )

        // Last chat line (clickable to open chat dialog)
        val lastChat = state.chatLog.lastOrNull()
        if (lastChat != null) {
            Text(
                text = lastChat,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showChat = true }
            )
        }

        // Game table
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Opponent hands (face down)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.players
                    .filter { it.sessionId != state.mySessionId && it.active }
                    .forEach { player ->
                        DeckView(
                            count = player.handSize,
                            faceDown = state.faceDown
                        )
                    }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Discard pile
            PileView(
                cards = state.discardPile,
                faceDown = state.faceDown
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // My hand
        HandRow(
            cards = state.myHand,
            faceDown = false,
            selectedCards = selectedCards,
            onCardClick = { card ->
                selectedCards = if (card in selectedCards) {
                    selectedCards - card
                } else {
                    selectedCards + card
                }
            }
        )

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { viewModel.pickup() }, modifier = Modifier.weight(1f)) {
                Text("Pick Up")
            }
            Button(
                onClick = {
                    val success = viewModel.putdown(selectedCards.toList())
                    if (success) {
                        selectedCards = emptySet()
                    }
                },
                enabled = selectedCards.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Play ${selectedCards.size}")
            }
            Button(onClick = { viewModel.skip() }, modifier = Modifier.weight(1f)) {
                Text("Skip")
            }
        }

        // Chat dialog
        if (showChat) {
            Dialog(onDismissRequest = { showChat = false }) {
                ChatPanel(
                    messages = state.chatLog,
                    onSend = { text ->
                        viewModel.chat(text)
                        chatInput = ""
                    },
                    input = chatInput,
                    onInputChange = { chatInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    onClose = { showChat = false }
                )
            }
        }
    }
}