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

class Room(
  val maxSlots: Int = 6,
) {
  val deck = Deck()
  val slots = MutableList(maxSlots) { -1 }
  var rules: Rules = WarlordsRules()
  var turn: Int = -1
  var cheatState: CheatState = CheatState()
  var activeSuit: Char? = null

  var saveDeck: Deck? = null
  var saveHand: Hand? = null
  var saveSessionId: Int = -1

  fun saveState(player: Player) {
    saveDeck = deck.copy()
    saveHand = Hand(player.hand)
    saveSessionId = player.sessionId
  }

  fun restoreState(players: List<Player>) {
    saveDeck?.let { saved ->
      deck.restoreFrom(saved)
    }
    saveHand?.let { savedHand ->
      val player = players.find { it.sessionId == saveSessionId }
      player?.hand?.let { h ->
        h.clear()
        h.addAll(savedHand)
      }
    }
    saveDeck = null
    saveHand = null
    saveSessionId = -1
  }
}
