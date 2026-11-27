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

import kotlin.random.Random

fun rankToChar(rank: Int): Char =
  when (rank) {
    0 -> '2'
    1 -> '3'
    2 -> '4'
    3 -> '5'
    4 -> '6'
    5 -> '7'
    6 -> '8'
    7 -> '9'
    8 -> 'X'
    9 -> 'J'
    10 -> 'Q'
    11 -> 'K'
    12 -> 'A'
    else -> '?'
  }

fun <T> List<T>.randomElement(rng: Random): T = this[rng.nextInt(this.size)]

fun cardDisplayName(card: Card): String {
  val rankName =
    when (card.code[0]) {
      '2' -> "Two"
      '3' -> "Three"
      '4' -> "Four"
      '5' -> "Five"
      '6' -> "Six"
      '7' -> "Seven"
      '8' -> "Eight"
      '9' -> "Nine"
      'X' -> "Ten"
      'J' -> "Jack"
      'Q' -> "Queen"
      'K' -> "King"
      'A' -> "Ace"
      else -> "?"
    }
  val suitName =
    when (card.code[1]) {
      'C' -> "Clubs"
      'D' -> "Diamonds"
      'H' -> "Hearts"
      'S' -> "Spades"
      else -> "?"
    }
  return "$rankName of $suitName"
}
