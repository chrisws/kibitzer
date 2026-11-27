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

abstract class Rules {
  abstract fun canPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean

  abstract fun canRevoke(): Boolean

  abstract fun clearDiscard(hand: Hand): Boolean

  abstract fun faceDown(): Boolean

  abstract fun getRank(): CardRank

  abstract fun handSize(players: Int): Int

  abstract fun isValidPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean

  open fun findValidCards(
    deck: Deck,
    hand: Hand,
  ): List<Card> = hand.toList()

  abstract fun isWinningPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean

  abstract fun name(): String

  abstract fun url(): String

  abstract fun noPlayTakesDiscard(): Boolean

  abstract fun setNextTurn(hand: Hand): Boolean

  open fun invalidReason(
    deck: Deck,
    hand: Hand,
  ): String = "Not a valid play"
}
