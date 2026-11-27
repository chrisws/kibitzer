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

class OldMaidRules : Rules() {
  companion object {
    val OLD_MAID = Card.TWO_SPADES
    val DECK_CARDS: List<Card> =
      Card.entries.filter {
        it != Card.TWO_CLUBS && it != Card.TWO_DIAMONDS && it != Card.TWO_HEARTS
      }
  }

  override fun canPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = hand.size > 1

  override fun canRevoke(): Boolean = false

  override fun clearDiscard(hand: Hand): Boolean = false

  override fun faceDown(): Boolean = true

  override fun getRank(): CardRank = ::rank2A

  override fun handSize(players: Int): Int = 49 / players

  override fun isValidPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = true

  override fun isWinningPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = hand.size == 0

  override fun name(): String = "Old Maid"

  override fun url(): String = "https://en.wikipedia.org/wiki/Old_Maid_(card_game)"

  override fun noPlayTakesDiscard(): Boolean = false

  override fun setNextTurn(hand: Hand): Boolean = true
}
