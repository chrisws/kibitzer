package com.kibitzer.mobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
        // Fanned/overlapping card layout using padding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            cards.forEachIndexed { index, card ->
                val isSelected = card in selectedCards
                CardView(
                    card = card,
                    faceDown = faceDown,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(
                            start = (index * 22).dp,
                            top = if (isSelected) 8.dp else 0.dp
                        )
                        .zIndex(index.toFloat())
                        .clickable {
                            if (isSelected) {
                                selectedCards.remove(card)
                            } else {
                                selectedCards.add(card)
                            }
                        }
                )
            }
        }

        // Play button
        if (selectedCards.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = {
                    onPlay(selectedCards.toList())
                    selectedCards.clear()
                }) {
                    Text("Play ${selectedCards.size} card${if (selectedCards.size != 1) "s" else ""}")
                }
            }
        }
    }
}