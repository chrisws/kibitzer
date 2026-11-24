package com.kibitzer.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kibitzer.mobile.game.Card
import com.kibitzer.mobile.ui.theme.CardBack
import com.kibitzer.mobile.ui.theme.CardFace
import com.kibitzer.mobile.ui.theme.CardRed

@Composable
fun CardView(
    card: Card,
    faceDown: Boolean = false,
    modifier: Modifier = Modifier
) {
    val suitColor = when (card.suit) {
        'H', 'D' -> CardRed
        else -> Color.Black
    }

    Box(
        modifier = modifier
            .size(width = 48.dp, height = 64.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (faceDown) CardBack else CardFace),
        contentAlignment = Alignment.Center
    ) {
        if (!faceDown) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = card.name.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = suitColor
                )
                Text(
                    text = card.suit.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = suitColor
                )
            }
        }
    }
}