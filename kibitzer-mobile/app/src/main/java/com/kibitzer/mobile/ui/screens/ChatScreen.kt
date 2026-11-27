package com.kibitzer.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kibitzer.mobile.model.GameViewModel
import com.kibitzer.mobile.ui.components.ChatPanel

@Composable
fun ChatScreen(
  viewModel: GameViewModel,
  onBack: () -> Unit,
) {
  val state by viewModel.state.collectAsState()
  var chatInput by remember { mutableStateOf("") }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Chat",
        style = MaterialTheme.typography.titleLarge,
      )
      Button(onClick = onBack) {
        Text("Back")
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    ChatPanel(
      messages = state.chatLog,
      onSend = { text ->
        viewModel.chat(text)
        chatInput = ""
      },
      input = chatInput,
      onInputChange = { chatInput = it },
      modifier =
        Modifier
          .fillMaxWidth()
          .weight(1f),
    )
  }
}
