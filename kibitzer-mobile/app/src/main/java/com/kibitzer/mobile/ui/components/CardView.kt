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
