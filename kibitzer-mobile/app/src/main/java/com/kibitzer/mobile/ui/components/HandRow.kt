package com.kibitzer.mobile.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kibitzer.mobile.game.Card

@Composable
fun HandRow(
    cards: List<Card>,
    faceDown: Boolean = false,
    selectedCards: Set<Card>,
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        cards.forEach { card ->
            val isSelected = card in selectedCards
            CardView(
                card = card,
                faceDown = faceDown,
                modifier = Modifier
                    .size(width = 72.dp, height = 100.dp)
                    .offset(y = if (isSelected) (-6).dp else 0.dp)
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) Color.Blue else Color.Transparent
                    )
                    .clickable { onCardClick(card) }
            )
        }
    }
}