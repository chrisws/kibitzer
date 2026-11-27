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

class FreeRules : Rules() {
  override fun canPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean {
    if (hand.size == 0) return false
    if (deck.discardSize == 0) return true
    val last = deck.lastPlay ?: return true
    return hand.hasHigher(last, 1, ::rank2A)
  }

  override fun canRevoke(): Boolean = true

  override fun clearDiscard(hand: Hand): Boolean = false

  override fun faceDown(): Boolean = false

  override fun getRank(): CardRank = ::rank2A

  override fun handSize(players: Int): Int = 7

  override fun isValidPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = true

  override fun findValidCards(
    deck: Deck,
    hand: Hand,
  ): List<Card> {
    val last = deck.lastPlay
    if (last == null) return hand.toList()
    return hand.toList().filter { rank2A(it) > rank2A(last) }
  }

  override fun isWinningPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = false

  override fun name(): String = "Rules Free"

  override fun url(): String = "https://en.wikipedia.org/wiki/Card_game"

  override fun noPlayTakesDiscard(): Boolean = false

  override fun setNextTurn(hand: Hand): Boolean = true
}
