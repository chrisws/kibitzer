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

enum class Game(
  val description: String,
  val minPlayers: Int = 2,
  val maxPlayers: Int = 6,
) {
  WARLORDS(
    "Classic sequence game. Play matching ranks from 3\u2663 up to 2\u2663 (J, Q, K, A are valid). Four of a kind clears the pile.",
    2,
    6,
  ),
  FREE("Free-for-all. Play any card higher than the last one played. Pick up if you can't.", 2, 6),
  CRAZY_EIGHTS("Match the rank or suit of the last card. 8s let you change suit. First to empty wins.", 2, 6),
  CHEAT("Bid how many of the current rank you hold. Play them truthfully or get caught and pick up.", 2, 6),
  OLD_MAID("Find and discard pairs of matching rank. Last player holding a card loses.", 2, 6),
  GO_FISH("Ask opponents for cards to complete sets of four. First to empty their hand wins.", 2, 6),
  ;

  companion object {
    fun fromName(name: String): Game? = entries.find { it.name.equals(name, ignoreCase = true) }
  }
}
