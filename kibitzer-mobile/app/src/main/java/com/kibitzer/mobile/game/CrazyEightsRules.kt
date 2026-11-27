/*
 * This file is part of kibitzer
 *
 * Copyright (C) 2026 Chris Warren-Smith. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kibitzer.mobile.game

class CrazyEightsRules : Rules() {
  override fun canPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean {
    if (hand.size == 0) return false
    if (deck.discardSize == 0) return true
    val last = deck.lastPlay ?: return true
    if (hand.hasEqual(Card.EIGHT_CLUBS, ::rank2A)) return true
    return hand.hasEqual(last, ::rank2A)
  }

  override fun canRevoke(): Boolean = false

  override fun clearDiscard(hand: Hand): Boolean = false

  override fun faceDown(): Boolean = false

  override fun getRank(): CardRank = ::rank2A

  override fun handSize(players: Int): Int = if (players <= 2) 7 else 5

  override fun isValidPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean {
    if (hand.size == 0) return false
    if (hand.size != 1) return false
    val card = hand.peek()
    if (deck.discardSize == 0) return true
    val last = deck.lastPlay ?: return true
    if (rank2A(card) == rank2A(Card.EIGHT_CLUBS)) return true
    return rank2A(card) == rank2A(last)
  }

  override fun findValidCards(
    deck: Deck,
    hand: Hand,
  ): List<Card> {
    if (deck.discardSize == 0) return hand.toList()
    val last = deck.lastPlay ?: return hand.toList()
    return hand.toList().filter { card ->
      rank2A(card) == rank2A(Card.EIGHT_CLUBS) || rank2A(card) == rank2A(last)
    }
  }

  override fun isWinningPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = hand.size == 0

  override fun name(): String = "Crazy Eights"

  override fun url(): String = "https://en.wikipedia.org/wiki/Crazy_Eights"

  override fun noPlayTakesDiscard(): Boolean = true

  override fun setNextTurn(hand: Hand): Boolean = true
}
