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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeckTest {
  @Test
  fun `shuffle fills pack with 52 cards`() {
    val deck = Deck()
    deck.shuffle()
    assertEquals(52, deck.packSize)
    assertEquals(0, deck.discardSize)
  }

  @Test
  fun `deal moves cards from pack to hand`() {
    val deck = Deck()
    deck.shuffle()
    val hand = Hand()
    deck.deal(hand, 5)
    assertEquals(5, hand.size)
    assertEquals(47, deck.packSize)
  }

  @Test
  fun `pickup moves cards from pack to hand`() {
    val deck = Deck()
    deck.shuffle()
    val hand = Hand()
    val picked = deck.pickup(hand, 3)
    assertEquals(3, picked.size)
    assertEquals(3, hand.size)
    assertEquals(49, deck.packSize)
  }

  @Test
  fun `putdown moves cards to discard`() {
    val deck = Deck()
    deck.shuffle()
    val hand = Hand()
    hand.add(Card.ACE_SPADES)
    hand.add(Card.KING_SPADES)
    deck.putdown(hand)
    assertEquals(2, deck.discardSize)
    assertEquals(Card.KING_SPADES, deck.lastPlay)
    assertEquals(2, deck.lastSize)
  }

  @Test
  fun `clearDiscard empties discard`() {
    val deck = Deck()
    deck.shuffle()
    val hand = Hand()
    hand.add(Card.ACE_SPADES)
    deck.putdown(hand)
    deck.clearDiscard()
    assertEquals(0, deck.discardSize)
    assertNull(deck.lastPlay)
  }

  @Test
  fun `takeDiscard moves discard to hand`() {
    val deck = Deck()
    deck.shuffle()
    val hand = Hand()
    hand.add(Card.ACE_SPADES)
    hand.add(Card.KING_SPADES)
    deck.putdown(hand)
    val target = Hand()
    deck.takeDiscard(target)
    assertEquals(2, target.size)
    assertEquals(0, deck.discardSize)
  }

  @Test
  fun `copy creates independent deck`() {
    val deck = Deck()
    deck.shuffle()
    val copy = deck.copy()
    assertEquals(deck.packSize, copy.packSize)
    assertEquals(deck.discardSize, copy.discardSize)
  }

  @Test
  fun `deal with 0 cards leaves hand empty`() {
    val deck = Deck()
    deck.shuffle()
    val hand = Hand()
    deck.deal(hand, 0)
    assertEquals(0, hand.size)
    assertEquals(52, deck.packSize)
  }
}
