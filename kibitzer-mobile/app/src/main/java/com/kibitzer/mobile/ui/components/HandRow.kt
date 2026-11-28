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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kibitzer.mobile.game.Card

@Composable
fun HandRow(
  cards: List<Card>,
  faceDown: Boolean = false,
  selectedCards: Set<Card>,
  onCardClick: (Card) -> Unit,
  compact: Boolean = false,
  modifier: Modifier = Modifier,
) {
  OverlappingCardRow(
    overlap = if (compact) 40.dp else 0.dp,
    spacing = if (compact) 0.dp else 4.dp,
    cardWidth = 72.dp,
    modifier =
      modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 8.dp, vertical = 4.dp),
    items = cards,
  ) { itemModifier, card ->
    val isSelected = card in selectedCards
    CardView(
      card = card,
      faceDown = faceDown,
      modifier =
        itemModifier
          .size(width = 72.dp, height = 100.dp)
          .offset(y = if (isSelected) (-6).dp else 0.dp)
          .border(
            width = if (isSelected) 2.dp else 0.dp,
            color = if (isSelected) Color.Blue else Color.Transparent,
          ).clickable { onCardClick(card) },
    )
  }
}