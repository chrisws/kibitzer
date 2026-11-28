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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
  compact: Boolean = false,
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
      OverlappingCardRow(
        overlap = if (compact) 40.dp else 0.dp,
        cardWidth = 72.dp,
        modifier =
          Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        items = cards,
      ) { itemModifier, card ->
        CardView(
          card = card,
          faceDown = faceDown,
          modifier = itemModifier.size(width = 72.dp, height = 100.dp),
        )
      }
      Text(
        text = "${cards.size} card${if (cards.size != 1) "s" else ""}",
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}