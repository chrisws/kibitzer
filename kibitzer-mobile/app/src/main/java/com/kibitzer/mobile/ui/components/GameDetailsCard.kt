package com.kibitzer.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kibitzer.mobile.game.Game

@Composable
fun GameDetailsCard(
  game: Game,
  modifier: Modifier = Modifier,
) {
  Card(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = game.name.replace("_", " "),
        style = MaterialTheme.typography.titleSmall,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = game.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Players: ${game.minPlayers}\u2013${game.maxPlayers}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
      )
    }
  }
}
