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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kibitzer.mobile.model.SettingsManager

@Composable
fun SettingsScreen(
  onBack: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = "Settings",
      style = MaterialTheme.typography.headlineMedium,
    )

    HorizontalDivider()

    Text(
      text = "Profile",
      style = MaterialTheme.typography.titleMedium,
    )

    OutlinedTextField(
      value = SettingsManager.nickname,
      onValueChange = { SettingsManager.updateNickname(it) },
      label = { Text("Your Name") },
      placeholder = { Text("You") },
      modifier = Modifier.fillMaxWidth(),
    )

    HorizontalDivider()

    Text(
      text = "Display",
      style = MaterialTheme.typography.titleMedium,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Compact cards",
          style = MaterialTheme.typography.bodyLarge,
        )
        Text(
          text = "Overlap cards to save space",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Switch(
        checked = SettingsManager.compactCards,
        onCheckedChange = { SettingsManager.updateCompactCards(it) },
      )
    }

    Spacer(modifier = Modifier.height(32.dp))

    OutlinedButton(
      onClick = onBack,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Back")
    }
  }
}