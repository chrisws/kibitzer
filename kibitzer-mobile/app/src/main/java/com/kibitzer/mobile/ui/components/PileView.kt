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

package com.kibitzer.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kibitzer.mobile.game.Card

@Composable
fun PileView(
  cards: List<Card>,
  faceDown: Boolean = false,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (cards.isEmpty()) {
      Text(
        text = "Discard",
        style = MaterialTheme.typography.bodySmall,
      )
    } else {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(horizontal = 8.dp),
      ) {
        items(cards) { card ->
          CardView(
            card = card,
            faceDown = faceDown,
          )
        }
      }
      Text(
        text = "${cards.size} card${if (cards.size != 1) "s" else ""}",
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}
