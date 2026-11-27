package com.kibitzer.mobile.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeRulesTest {
  private val rules = FreeRules()

  @Test
  fun `name is correct`() {
    assertEquals("Rules Free", rules.name())
  }

  @Test
  fun `handSize is always 7`() {
    assertEquals(7, rules.handSize(2))
    assertEquals(7, rules.handSize(4))
    assertEquals(7, rules.handSize(6))
  }

  @Test
  fun `canPlay with empty hand is false`() {
    val deck = Deck()
    deck.shuffle()
    val hand = Hand()
    assertFalse(rules.canPlay(deck, hand))
  }

  @Test
  fun `canPlay with empty discard is true`() {
    val deck = Deck()
    deck.shuffle()
    val hand = Hand()
    hand.add(Card.ACE_SPADES)
    assertTrue(rules.canPlay(deck, hand))
  }

  @Test
  fun `canPlay requires higher rank`() {
    val deck = Deck()
    deck.shuffle()
    val played = Hand()
    played.add(Card.ACE_SPADES)
    deck.putdown(played)

    val hand = Hand()
    hand.add(Card.TWO_CLUBS)
    // In rank2A: A=12, 2=0, so 2 is lower than A
    assertFalse(rules.canPlay(deck, hand))
  }

  @Test
  fun `isValidPlay is always true`() {
    val deck = Deck()
    deck.shuffle()
    val hand = Hand()
    hand.add(Card.ACE_SPADES)
    assertTrue(rules.isValidPlay(deck, hand))
  }

  @Test
  fun `clearDiscard is always false`() {
    val hand = Hand()
    hand.add(Card.THREE_CLUBS)
    assertFalse(rules.clearDiscard(hand))
  }

  @Test
  fun `canRevoke is true`() {
    assertTrue(rules.canRevoke())
  }

  @Test
  fun `noPlayTakesDiscard is false`() {
    assertFalse(rules.noPlayTakesDiscard())
  }

  @Test
  fun `findValidCards with no last play returns all`() {
    val deck = Deck()
    val hand =
      Hand().apply {
        add(Card.TWO_CLUBS)
        add(Card.ACE_SPADES)
      }
    val valid = rules.findValidCards(deck, hand)
    assertEquals(2, valid.size)
  }

  @Test
  fun `findValidCards filters to higher ranks`() {
    val deck = Deck()
    deck.putdown(Hand().apply { add(Card.FIVE_CLUBS) })
    val hand =
      Hand().apply {
        add(Card.SIX_HEARTS) // 6 > 5, valid
        add(Card.ACE_SPADES) // 14 > 5, valid
        add(Card.FOUR_DIAMONDS) // 4 < 5, not valid
        add(Card.FIVE_HEARTS) // 5 == 5, not valid (must be strictly higher)
      }
    val valid = rules.findValidCards(deck, hand)
    assertEquals(2, valid.size)
    assertTrue(valid.contains(Card.SIX_HEARTS))
    assertTrue(valid.contains(Card.ACE_SPADES))
    assertFalse(valid.contains(Card.FOUR_DIAMONDS))
    assertFalse(valid.contains(Card.FIVE_HEARTS))
  }
}
