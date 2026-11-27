package com.kibitzer.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kibitzer.mobile.ui.theme.PlayerActive
import com.kibitzer.mobile.ui.theme.PlayerInactive

@Composable
fun PlayerAvatar(
  nickname: String,
  active: Boolean = false,
  isAi: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val bgColor =
    when {
      isAi -> Color(0xFF7E57C2)
      active -> PlayerActive
      else -> PlayerInactive
    }

  Box(
    modifier =
      modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(bgColor),
    contentAlignment = Alignment.Center,
  ) {
    val initial = nickname.firstOrNull()?.uppercase() ?: "?"
    Text(
      text = initial,
      style = MaterialTheme.typography.titleMedium,
      color = Color.White,
    )
  }
}
