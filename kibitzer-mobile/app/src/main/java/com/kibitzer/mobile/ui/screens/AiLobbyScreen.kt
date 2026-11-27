package com.kibitzer.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import com.kibitzer.mobile.game.AiPlayer
import com.kibitzer.mobile.game.Game
import com.kibitzer.mobile.model.GameViewModel
import com.kibitzer.mobile.ui.components.GameDetailsCard

@Composable
fun AiLobbyScreen(
  viewModel: GameViewModel,
  onBack: () -> Unit,
  onGameStart: () -> Unit,
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val state by viewModel.state.collectAsState()
  var numAiPlayers by remember { mutableStateOf(1) }
  var difficulty by remember { mutableStateOf(AiPlayer.Difficulty.MEDIUM) }
  var nickname by remember {
    mutableStateOf(
      android.provider.Settings.Global.getString(
        context.contentResolver,
        "device_name",
      ) ?: "",
    )
  }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = "Play with AI",
      style = MaterialTheme.typography.headlineLarge,
    )

    // --- Game selection ---
    Text(
      text = "Choose a game",
      style = MaterialTheme.typography.titleMedium,
    )

    var gameDropdownExpanded by remember { mutableStateOf(false) }

    Box {
      OutlinedButton(
        onClick = { gameDropdownExpanded = true },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(state.game.name.replace("_", " "))
        Spacer(modifier = Modifier.width(8.dp))
        Text("▼")
      }

      DropdownMenu(
        expanded = gameDropdownExpanded,
        onDismissRequest = { gameDropdownExpanded = false },
      ) {
        Game.entries.forEach { game ->
          DropdownMenuItem(
            text = { Text(game.name.replace("_", " ")) },
            onClick = {
              viewModel.setGame(game)
              gameDropdownExpanded = false
            },
          )
        }
      }
    }

    // --- Game details ---
    GameDetailsCard(game = state.game)

    Spacer(modifier = Modifier.height(8.dp))

    // --- AI player count ---
    Text(
      text = "AI Opponents: $numAiPlayers",
      style = MaterialTheme.typography.titleMedium,
    )
    Slider(
      value = numAiPlayers.toFloat(),
      onValueChange = { numAiPlayers = it.toInt().coerceIn(1, 5) },
      valueRange = 1f..5f,
      steps = 3,
      modifier = Modifier.fillMaxWidth(),
    )

    // --- Difficulty ---
    Text(
      text = "Difficulty",
      style = MaterialTheme.typography.titleMedium,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      AiPlayer.Difficulty.entries.forEach { d ->
        val selected = d == difficulty
        OutlinedButton(
          onClick = { difficulty = d },
          modifier = Modifier.weight(1f),
        ) {
          Text(
            text = d.name,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }

    // --- Nickname ---
    OutlinedTextField(
      value = nickname,
      onValueChange = { nickname = it },
      label = { Text("Your Name") },
      placeholder = { Text("You") },
      modifier = Modifier.fillMaxWidth(),
    )

    // --- Player list preview ---
    Text(
      text = "Players",
      style = MaterialTheme.typography.titleMedium,
    )
    Card(
      modifier =
        Modifier
          .fillMaxWidth()
          .weight(1f),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "●",
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = nickname.ifBlank { "You" },
            style = MaterialTheme.typography.bodyLarge,
          )
        }
        repeat(numAiPlayers) { i ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "●",
              color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "AI Player ${i + 1}",
              style = MaterialTheme.typography.bodyLarge,
            )
          }
        }
      }
    }

    // --- Bottom buttons ---
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
        Text("Back")
      }
      Button(
        onClick = {
          viewModel.startAiGame(
            state.game,
            numAiPlayers,
            difficulty,
            nickname.ifBlank { "You" },
          )
          onGameStart()
        },
        modifier = Modifier.weight(2f),
      ) {
        Text("Start Game")
      }
    }
  }
}
