/*
 * This file is part of kibitzer
 *
 * Copyright (C) 2026 Chris Warren-Smith. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kibitzer.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kibitzer.mobile.game.Game
import com.kibitzer.mobile.model.ConnectionState
import com.kibitzer.mobile.model.GameViewModel
import com.kibitzer.mobile.ui.components.GameDetailsCard
import com.kibitzer.mobile.ui.components.PlayerAvatar

@Composable
fun LobbyScreen(
  viewModel: GameViewModel,
  onBack: () -> Unit,
  onGameStart: () -> Unit,
) {
  val state by viewModel.state.collectAsState()

  LaunchedEffect(Unit) {
    viewModel.connect()
  }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = "Play with Friends",
      style = MaterialTheme.typography.headlineLarge,
    )

    // --- Connection / Scan section ---
    when (state.connectionState) {
      ConnectionState.SCANNING -> {
        Text(
          text = "Scanning for games…",
          style = MaterialTheme.typography.bodyLarge,
        )
      }
      ConnectionState.CONNECTING -> {
        Text(
          text = "Connecting…",
          style = MaterialTheme.typography.bodyLarge,
        )
      }
      ConnectionState.CONNECTED -> {
        Text(
          text = "Connected",
          style = MaterialTheme.typography.bodyLarge,
        )
      }
      else -> {
        Text(
          text = "Not connected",
          style = MaterialTheme.typography.bodyLarge,
        )
      }
    }

    // --- Discovered games ---
    if (state.discoveredGames.isNotEmpty()) {
      Text(
        text = "Available Games",
        style = MaterialTheme.typography.titleMedium,
      )
      state.discoveredGames.forEach { game ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = game.name.ifBlank { game.address },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
          )
          Button(onClick = { viewModel.joinGame(game.address) }) {
            Text("Join")
          }
        }
      }
    }

    // --- Host new game ---
    OutlinedButton(onClick = { viewModel.hostGame() }, modifier = Modifier.fillMaxWidth()) {
      Text("Host New Game")
    }

    // --- Game type ---
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

    // --- Players ---
    if (state.players.isNotEmpty()) {
      Text(
        text = "Players",
        style = MaterialTheme.typography.titleMedium,
      )

      LazyColumn(
        modifier = Modifier.weight(1f),
      ) {
        items(state.players) { player ->
          Card(
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
          ) {
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .padding(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              PlayerAvatar(
                nickname = player.nickname,
                active = player.active,
                isAi = player.isAi,
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = player.nickname.ifBlank { "Empty" },
                  style = MaterialTheme.typography.bodyLarge,
                )
                if (player.active) {
                  Text(
                    text = "${player.handSize} cards",
                    style = MaterialTheme.typography.bodySmall,
                  )
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    // --- Bottom buttons ---
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
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
