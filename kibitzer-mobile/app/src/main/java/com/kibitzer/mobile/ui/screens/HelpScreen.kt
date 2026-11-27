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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen(
  onBack: () -> Unit,
  onConnectionHelp: () -> Unit,
  onGameRules: () -> Unit,
  onAbout: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = "Help",
      style = MaterialTheme.typography.displayLarge,
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onConnectionHelp, modifier = Modifier.fillMaxWidth()) {
      Text("Connection Help")
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onGameRules, modifier = Modifier.fillMaxWidth()) {
      Text("Game Rules")
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onAbout, modifier = Modifier.fillMaxWidth()) {
      Text("About")
    }
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
      Text("Back")
    }
  }
}
