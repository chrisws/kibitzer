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

class Deck {
  val pack = Hand()
  val discard = Hand()
  var lastPlay: Card? = null
    private set
  var lastSize: Int = 0
    private set

  val packSize: Int get() = pack.size
  val discardSize: Int get() = discard.size

  fun shuffle() {
    discard.clear()
    lastPlay = null
    lastSize = 0
    pack.clear()
    for (card in Card.entries) {
      pack.add(card)
    }
    for (i in pack.size - 1 downTo 1) {
      val j = Random.nextInt(i + 1)
      pack.swap(i, j)
    }
  }

  fun deal(
    hand: Hand,
    count: Int,
  ) {
    hand.clear()
    repeat(count) {
      if (pack.size > 0) {
        hand.add(pack.pop())
      }
    }
  }

  fun pickup(
    hand: Hand,
    count: Int,
  ): Hand {
    val result = Hand()
    repeat(count) {
      if (pack.size > 0) {
        val card = pack.pop()
        result.add(card)
        hand.add(card)
      }
    }
    return result
  }

  fun pickupFromDiscard(
    hand: Hand,
    count: Int,
  ): Hand {
    val result = Hand()
    repeat(count) {
      if (discard.size > 0) {
        val card = discard.pop()
        result.add(card)
        hand.add(card)
      }
    }
    lastPlay = if (discard.size > 0) discard.peek() else null
    lastSize = 0
    return result
  }

  fun putdown(hand: Hand) {
    discard.addAll(hand)
    lastPlay = discard.peek()
    lastSize = hand.size
  }

  fun clearDiscard() {
    discard.clear()
    lastPlay = null
    lastSize = 0
  }

  fun takeDiscard(hand: Hand) {
    hand.addAll(discard)
    discard.clear()
    lastPlay = null
    lastSize = 0
  }

  fun getPackJson(): String = pack.toJson()

  fun getDiscardJson(): String = discard.toJson()

  fun restoreFrom(other: Deck) {
    pack.clear()
    pack.addAll(other.pack)
    discard.clear()
    discard.addAll(other.discard)
    lastPlay = other.lastPlay
    lastSize = other.lastSize
  }

  fun copy(): Deck {
    val d = Deck()
    d.pack.addAll(pack)
    d.discard.addAll(discard)
    d.lastPlay = lastPlay
    d.lastSize = lastSize
    return d
  }
}
