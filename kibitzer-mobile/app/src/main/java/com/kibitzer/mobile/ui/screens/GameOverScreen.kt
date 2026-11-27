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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kibitzer.mobile.model.GameViewModel

@Composable
fun GameOverScreen(
  viewModel: GameViewModel,
  onPlayAgain: () -> Unit,
  onHome: () -> Unit,
) {
  val state by viewModel.state.collectAsState()

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = "Game Over",
      style = MaterialTheme.typography.displayLarge,
    )
    Spacer(modifier = Modifier.height(16.dp))
    if (state.winner.isNotEmpty()) {
      Text(
        text = "${state.winner} wins!",
        style = MaterialTheme.typography.headlineLarge,
      )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = state.message,
      style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(modifier = Modifier.height(48.dp))
    Button(onClick = onPlayAgain) {
      Text("Play Again")
    }
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedButton(onClick = onHome) {
      Text("Home")
    }
  }
}
