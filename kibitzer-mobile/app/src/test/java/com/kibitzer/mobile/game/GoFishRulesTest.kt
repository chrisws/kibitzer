package com.kibitzer.mobile.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoFishRulesTest {
  private val rules = GoFishRules()

  @Test
  fun `name is correct`() {
    assertEquals("Go Fish", rules.name())
  }

  @Test
  fun `handSize is 7 for 3 or fewer players`() {
    assertEquals(7, rules.handSize(2))
    assertEquals(7, rules.handSize(3))
  }

  @Test
  fun `handSize is 5 for more than 3 players`() {
    assertEquals(5, rules.handSize(4))
    assertEquals(5, rules.handSize(5))
    assertEquals(5, rules.handSize(6))
  }

  @Test
  fun `canPlay with empty hand is false`() {
    val deck = Deck()
    deck.shuffle()
    val hand = Hand()
    assertFalse(rules.canPlay(deck, hand))
  }

  @Test
  fun `canPlay with cards is true`() {
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
  fun `isValidPlay is always true`() {
    val deck = Deck()
    val hand = Hand()
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
  fun `noPlayTakesDiscard is false`() {
    assertFalse(rules.noPlayTakesDiscard())
  }

  @Test
  fun `setNextTurn is true`() {
    assertTrue(rules.setNextTurn(Hand()))
  }
}
