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
