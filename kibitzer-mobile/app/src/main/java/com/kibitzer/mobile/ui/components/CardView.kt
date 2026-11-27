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

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.kibitzer.mobile.game.Card

@Composable
fun CardView(
  card: Card,
  faceDown: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val bitmap =
    remember(card, faceDown) {
      val assetName = if (faceDown) "back.png" else "${card.code}.png"
      context.assets.open("cards/$assetName").use { stream ->
        BitmapFactory.decodeStream(stream).asImageBitmap()
      }
    }

  Image(
    bitmap = bitmap,
    contentDescription = if (faceDown) "Card back" else card.code,
    contentScale = ContentScale.FillBounds,
    modifier = modifier,
  )
}
