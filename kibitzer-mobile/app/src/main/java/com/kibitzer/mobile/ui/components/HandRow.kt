package com.kibitzer.mobile.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kibitzer.mobile.game.Card

@Composable
fun HandRow(
    cards: List<Card>,
    faceDown: Boolean = false,
    onPlay: (List<Card>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCards by remember { mutableStateOf(mutableSetOf<Card>()) }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            items(cards) { card ->
                val isSelected = card in selectedCards
                CardView(
                    card = card,
                    faceDown = faceDown,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                )
            }
        }
        if (selectedCards.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${selectedCards.size} selected — tap to play",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}