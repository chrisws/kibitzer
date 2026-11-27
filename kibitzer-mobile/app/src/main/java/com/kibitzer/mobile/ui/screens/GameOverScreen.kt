package com.kibitzer.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kibitzer.mobile.model.GameViewModel

@Composable
fun GameOverScreen(
  viewModel: GameViewModel,
  onPlayAgain: () -> Unit,
  onHome: () -> Unit,
) {
  val state by viewModel.state.collectAsState()

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = "Game Over",
      style = MaterialTheme.typography.displayLarge,
    )
    Spacer(modifier = Modifier.height(16.dp))
    if (state.winner.isNotEmpty()) {
      Text(
        text = "${state.winner} wins!",
        style = MaterialTheme.typography.headlineLarge,
      )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = state.message,
      style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(modifier = Modifier.height(48.dp))
    Button(onClick = onPlayAgain) {
      Text("Play Again")
    }
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedButton(onClick = onHome) {
      Text("Home")
    }
  }
}
