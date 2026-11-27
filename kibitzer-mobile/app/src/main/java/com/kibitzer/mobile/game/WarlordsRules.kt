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

class WarlordsRules : Rules() {
  override fun canPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean {
    if (hand.size == 0) return false
    if (hand.hasEqual(Card.TWO_CLUBS, ::rank32)) return true
    if (deck.discardSize == 0) return true
    val last = deck.lastPlay ?: return true
    if (rank32(last) == rank32(Card.TWO_CLUBS)) return true
    return hand.hasHigher(last, deck.lastSize, ::rank32)
  }

  override fun canRevoke(): Boolean = false

  override fun clearDiscard(hand: Hand): Boolean =
    hand.hasEqual(Card.THREE_CLUBS, ::rank32) ||
      hand.hasEqual(Card.TEN_CLUBS, ::rank32) ||
      hand.size == 4

  override fun faceDown(): Boolean = false

  override fun getRank(): CardRank = ::rank32

  override fun handSize(players: Int): Int = 52 / players

  override fun isValidPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean {
    if (hand.size == 0) {
      android.util.Log.d("Play", "Warlords.isValidPlay: REJECT - empty hand")
      return false
    }
    if (!hand.isEqual(hand.peek(), ::rank32)) {
      android.util.Log.d("Play", "Warlords.isValidPlay: REJECT - cards not all same rank: $hand")
      return false
    }
    if (hand.hasEqual(Card.TWO_CLUBS, ::rank32)) {
      android.util.Log.d("Play", "Warlords.isValidPlay: ACCEPT - playing 2s")
      return true
    }
    if (deck.discardSize == 0) {
      android.util.Log.d("Play", "Warlords.isValidPlay: ACCEPT - empty discard")
      return true
    }
    val last = deck.lastPlay ?: return true
    if (rank32(last) == rank32(Card.TWO_CLUBS)) {
      android.util.Log.d("Play", "Warlords.isValidPlay: ACCEPT - last was 2s")
      return true
    }
    val rankOk = hand.hasHigher(last, 1, ::rank32)
    val sizeOk = hand.size == deck.lastSize
    android.util.Log.d(
      "Play",
      "Warlords.isValidPlay: checking rank: hand=$hand > last=$last? $rankOk, size: ${hand.size}==${deck.lastSize}? $sizeOk",
    )
    return rankOk && sizeOk
  }

  override fun findValidCards(
    deck: Deck,
    hand: Hand,
  ): List<Card> {
    if (deck.discardSize == 0) return hand.toList()
    val last = deck.lastPlay ?: return hand.toList()
    val lastRank = rank32(last)
    if (lastRank == rank32(Card.TWO_CLUBS)) return hand.toList()
    return hand.toList().filter { card ->
      val r = rank32(card)
      r == rank32(Card.TWO_CLUBS) || r > lastRank
    }
  }

  override fun isWinningPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = hand.size == 0

  override fun invalidReason(
    deck: Deck,
    hand: Hand,
  ): String {
    if (hand.size == 0) return "You have no cards to play"
    if (!hand.isEqual(hand.peek(), ::rank32)) {
      return "All cards must be the same rank"
    }
    if (deck.discardSize == 0) return "Should be valid on an empty discard"
    val last = deck.lastPlay ?: return "Should be valid"
    if (rank32(last) == rank32(Card.TWO_CLUBS)) return "2s are always valid"
    if (hand.size != deck.lastSize) {
      return "You must play ${deck.lastSize} card${if (deck.lastSize != 1) "s" else ""} (last play was ${deck.lastSize})"
    }
    if (!hand.hasHigher(last, 1, ::rank32)) {
      return "Your rank is not high enough to beat ${rankName(last)}"
    }
    return "Not a valid play"
  }

  private fun rankName(card: Card): String =
    when (card.rank) {
      2 -> "2s"
      3 -> "3s"
      4 -> "4s"
      5 -> "5s"
      6 -> "6s"
      7 -> "7s"
      8 -> "8s"
      9 -> "9s"
      10 -> "10s"
      11 -> "Jacks"
      12 -> "Queens"
      13 -> "Kings"
      14 -> "Aces"
      else -> "?"
    }

  override fun name(): String = "Warlords and Scumbags"

  override fun url(): String = "https://en.wikipedia.org/wiki/President_(card_game)"

  override fun noPlayTakesDiscard(): Boolean = true

  override fun setNextTurn(hand: Hand): Boolean = true
}
