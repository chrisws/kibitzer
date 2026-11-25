package com.kibitzer.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kibitzer.mobile.model.PlayerInfo
import com.kibitzer.mobile.ui.theme.TurnHighlight

@Composable
fun TurnIndicator(
    isMyTurn: Boolean,
    currentTurn: Int,
    players: List<PlayerInfo>,
    modifier: Modifier = Modifier
) {
    val turnPlayer = players.find { it.sessionId == currentTurn }
    val label = when {
        currentTurn == -1 -> "Waiting..."
        isMyTurn -> "Your turn!"
        turnPlayer != null -> "${turnPlayer.nickname}'s turn"
        else -> "Waiting..."
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(
                if (isMyTurn) TurnHighlight else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isMyTurn) Color(0xFF1A1A2E) else MaterialTheme.colorScheme.onSurface
        )
    }
}