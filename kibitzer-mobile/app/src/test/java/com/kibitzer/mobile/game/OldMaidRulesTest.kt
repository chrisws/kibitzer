package com.kibitzer.mobile.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OldMaidRulesTest {
  private val rules = OldMaidRules()

  @Test
  fun `name is correct`() {
    assertEquals("Old Maid", rules.name())
  }

  @Test
  fun `handSize divides 49 by players`() {
    assertEquals(24, rules.handSize(2))
    assertEquals(16, rules.handSize(3))
    assertEquals(12, rules.handSize(4))
  }

  @Test
  fun `canPlay requires at least 2 cards`() {
    val deck = Deck()
    deck.shuffle()
    val hand = Hand()
    hand.add(Card.ACE_SPADES)
    assertFalse(rules.canPlay(deck, hand))
  }

  @Test
  fun `canPlay with 2 cards is true`() {
    val deck = Deck()
    deck.shuffle()
    val hand = Hand()
    hand.add(Card.ACE_SPADES)
    hand.add(Card.KING_HEARTS)
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

  @Test
  fun `OLD_MAID is two of spades`() {
    assertEquals(Card.TWO_SPADES, OldMaidRules.OLD_MAID)
  }

  @Test
  fun `DECK_CARDS has 49 cards`() {
    assertEquals(49, OldMaidRules.DECK_CARDS.size)
  }

  @Test
  fun `DECK_CARDS excludes 2C 2D 2H`() {
    assertFalse(OldMaidRules.DECK_CARDS.contains(Card.TWO_CLUBS))
    assertFalse(OldMaidRules.DECK_CARDS.contains(Card.TWO_DIAMONDS))
    assertFalse(OldMaidRules.DECK_CARDS.contains(Card.TWO_HEARTS))
  }

  @Test
  fun `DECK_CARDS includes 2S`() {
    assertTrue(OldMaidRules.DECK_CARDS.contains(Card.TWO_SPADES))
  }
}
