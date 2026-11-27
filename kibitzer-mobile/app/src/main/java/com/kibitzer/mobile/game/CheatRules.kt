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

data class CheatState(
  val phase: Phase = Phase.BIDDING,
  val bid: Int = 0,
  val bidSessionId: Int = -1,
  val called: Boolean = false,
  val calledBy: Int = -1,
  val playedCards: List<Card> = emptyList(),
  val currentRank: Int = 1,
) {
  enum class Phase { BIDDING, PLAYING, RESOLVING }

  fun withBid(
    sessionId: Int,
    count: Int,
  ) = copy(phase = Phase.PLAYING, bid = count, bidSessionId = sessionId)

  fun withPlayedCards(cards: List<Card>) = copy(playedCards = cards)

  fun withCalled(sessionId: Int) = copy(called = true, calledBy = sessionId, phase = Phase.RESOLVING)

  fun nextTurn() =
    copy(
      phase = Phase.BIDDING,
      bid = 0,
      bidSessionId = -1,
      called = false,
      calledBy = -1,
      playedCards = emptyList(),
      currentRank = if (currentRank >= 13) 1 else currentRank + 1,
    )

  fun reset() = CheatState()

  fun isBidValid(played: List<Card>): Boolean {
    val matching = played.count { rank2A(it) == currentRank }
    return matching >= bid
  }
}

class CheatRules : Rules() {
  override fun canPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = hand.size > 0

  override fun canRevoke(): Boolean = false

  override fun clearDiscard(hand: Hand): Boolean = false

  override fun faceDown(): Boolean = true

  override fun getRank(): CardRank = ::rank2A

  override fun handSize(players: Int): Int = (if (players <= 4) 52 else 104) / players

  override fun isValidPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = hand.size > 0

  override fun isWinningPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = hand.size == 0

  override fun name(): String = "Cheat"

  override fun url(): String = "https://en.wikipedia.org/wiki/Cheat_(card_game)"

  override fun noPlayTakesDiscard(): Boolean = true

  override fun setNextTurn(hand: Hand): Boolean = true
}
