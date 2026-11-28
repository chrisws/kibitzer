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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun <T> OverlappingCardRow(
  overlap: Dp = 0.dp,
  spacing: Dp = 0.dp,
  cardWidth: Dp,
  modifier: Modifier = Modifier,
  items: List<T>,
  itemContent: @Composable (Modifier, T) -> Unit,
) {
  val step = cardWidth - overlap + spacing
  val totalWidth =
    if (items.isEmpty()) {
      0.dp
    } else {
      cardWidth + step * (items.size - 1).toFloat()
    }

  Box(
    modifier = modifier.width(totalWidth),
    contentAlignment = Alignment.TopStart,
  ) {
    items.forEachIndexed { index, item ->
      val x = step * index.toFloat()
      itemContent(
        Modifier.offset(x = x),
        item,
      )
    }
  }
}