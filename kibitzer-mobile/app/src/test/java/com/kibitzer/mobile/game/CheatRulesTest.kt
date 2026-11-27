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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheatRulesTest {
  private val rules = CheatRules()

  @Test
  fun `name is correct`() {
    assertEquals("Cheat", rules.name())
  }

  @Test
  fun `handSize divides 52 for 4 or fewer players`() {
    assertEquals(26, rules.handSize(2))
    assertEquals(13, rules.handSize(4))
  }

  @Test
  fun `handSize divides 104 for more than 4 players`() {
    assertEquals(20, rules.handSize(5))
    assertEquals(17, rules.handSize(6))
  }

  @Test
  fun `canPlay with empty hand is false`() {
    val deck = Deck()
    deck.shuffle()
    val hand = Hand()
    assertFalse(rules.canPlay(deck, hand))
  }

  @Test
  fun `canPlay with any card is true`() {
    val deck = Deck()
    deck.shuffle()
    val hand = Hand()
    hand.add(Card.ACE_SPADES)
    assertTrue(rules.canPlay(deck, hand))
  }

  @Test
  fun `canRevoke is false`() {
    assertFalse(rules.canRevoke())
  }

  @Test
  fun `clearDiscard is always false`() {
    val hand = Hand()
    hand.add(Card.THREE_CLUBS)
    assertFalse(rules.clearDiscard(hand))
  }

  @Test
  fun `faceDown is true`() {
    assertTrue(rules.faceDown())
  }

  @Test
  fun `isValidPlay with empty hand is false`() {
    val deck = Deck()
    val hand = Hand()
    assertFalse(rules.isValidPlay(deck, hand))
  }

  @Test
  fun `isValidPlay with cards is true`() {
    val deck = Deck()
    val hand = Hand()
    hand.add(Card.KING_HEARTS)
    assertTrue(rules.isValidPlay(deck, hand))
  }

  @Test
  fun `isWinningPlay when hand is empty`() {
    val deck = Deck()
    val hand = Hand()
    assertTrue(rules.isWinningPlay(deck, hand))
  }

  @Test
  fun `isWinningPlay with cards is false`() {
    val deck = Deck()
    val hand = Hand()
    hand.add(Card.ACE_SPADES)
    assertFalse(rules.isWinningPlay(deck, hand))
  }

  @Test
  fun `noPlayTakesDiscard is true`() {
    assertTrue(rules.noPlayTakesDiscard())
  }

  @Test
  fun `setNextTurn is true`() {
    assertTrue(rules.setNextTurn(Hand()))
  }

  // --- CheatState tests ---

  @Test
  fun `CheatState defaults`() {
    val state = CheatState()
    assertEquals(CheatState.Phase.BIDDING, state.phase)
    assertEquals(0, state.bid)
    assertEquals(-1, state.bidSessionId)
    assertFalse(state.called)
    assertEquals(-1, state.calledBy)
    assertTrue(state.playedCards.isEmpty())
    assertEquals(1, state.currentRank)
  }

  @Test
  fun `withBid sets phase and bid`() {
    val state = CheatState().withBid(3, 4)
    assertEquals(CheatState.Phase.PLAYING, state.phase)
    assertEquals(4, state.bid)
    assertEquals(3, state.bidSessionId)
  }

  @Test
  fun `withPlayedCards stores cards`() {
    val cards = listOf(Card.ACE_SPADES, Card.ACE_HEARTS)
    val state = CheatState().withPlayedCards(cards)
    assertEquals(2, state.playedCards.size)
  }

  @Test
  fun `withCalled sets resolving phase`() {
    val state = CheatState().withCalled(2)
    assertTrue(state.called)
    assertEquals(2, state.calledBy)
    assertEquals(CheatState.Phase.RESOLVING, state.phase)
  }

  @Test
  fun `nextTurn increments rank`() {
    val state = CheatState(currentRank = 5)
    val next = state.nextTurn()
    assertEquals(6, next.currentRank)
    assertEquals(CheatState.Phase.BIDDING, next.phase)
    assertEquals(0, next.bid)
  }

  @Test
  fun `nextTurn wraps rank at 13`() {
    val state = CheatState(currentRank = 13)
    val next = state.nextTurn()
    assertEquals(1, next.currentRank)
  }

  @Test
  fun `reset returns fresh state`() {
    val state = CheatState(bid = 5, currentRank = 9).reset()
    assertEquals(CheatState.Phase.BIDDING, state.phase)
    assertEquals(0, state.bid)
    assertEquals(1, state.currentRank)
  }

  @Test
  fun `isBidValid checks matching rank count`() {
    val state = CheatState(currentRank = 12, bid = 2)
    val played = listOf(Card.ACE_SPADES, Card.ACE_HEARTS, Card.TWO_CLUBS)
    assertTrue(state.isBidValid(played))
  }

  @Test
  fun `isBidValid fails when not enough matching`() {
    val state = CheatState(currentRank = 12, bid = 3)
    val played = listOf(Card.ACE_SPADES, Card.ACE_HEARTS, Card.TWO_CLUBS)
    assertFalse(state.isBidValid(played))
  }
}
