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

class GoFishHandler : GameHandler {
  override fun handlePutdown(
    controller: Controller,
    player: Player,
    hand: Hand,
  ): Message {
    // Go Fish: putdown is not the primary action; ask is.
    // If a player puts down a completed set of 4, discard it.
    if (hand.size == 4 && hand.isEqual(hand.peek(), ::rank2A)) {
      player.hand.remove(hand)
      return Message("message", "${player.name()} discarded a set of 4")
    }
    return Message("message", "use ask to request cards in Go Fish")
  }

  fun handleAsk(
    controller: Controller,
    player: Player,
    data: String,
  ): Message {
    // Format: ask:<targetSessionId>:<rankChar>
    // e.g. ask:2:K means "player 2, do you have a King?"
    val parts = data.split(":")
    if (parts.size < 2) {
      return Message("message", "invalid ask format")
    }
    val targetId =
      parts[0].toIntOrNull()
        ?: return Message("message", "invalid target")
    val rankChar = parts[1].trim()[0].uppercaseChar()

    val target =
      controller.findSession(targetId)
        ?: return Message("message", "target not found")

    // Find all cards of that rank in target's hand
    val matching = target.hand.toList().filter { it.code[0] == rankChar }
    if (matching.isEmpty()) {
      // Go fish - draw from deck
      return Message("message", "${target.name()} says: Go fish!")
    } else {
      // Hit - transfer all matching cards
      val matchHand = Hand().apply { matching.forEach { add(it) } }
      target.hand.remove(matchHand)
      player.hand.addAll(matchHand)
      player.hand.sort(::rank2A)
      // Check for completed sets of 4
      discardCompletedSets(player)
      return Message("message", "${target.name()} gave ${matching.size} card(s) of rank $rankChar")
    }
  }

  private fun discardCompletedSets(player: Player) {
    val byRank = player.hand.toList().groupBy { rank2A(it) }
    for ((rank, cards) in byRank) {
      if (cards.size == 4) {
        val setHand = Hand().apply { cards.forEach { add(it) } }
        player.hand.remove(setHand)
      }
    }
  }
}
