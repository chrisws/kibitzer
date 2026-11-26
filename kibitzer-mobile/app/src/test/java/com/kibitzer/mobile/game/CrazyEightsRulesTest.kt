package com.kibitzer.mobile.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrazyEightsRulesTest {

    private val rules = CrazyEightsRules()

    @Test
    fun `name is correct`() {
        assertEquals("Crazy Eights", rules.name())
    }

    @Test
    fun `handSize is 7 for 2 players`() {
        assertEquals(7, rules.handSize(2))
    }

    @Test
    fun `handSize is 5 for 3+ players`() {
        assertEquals(5, rules.handSize(3))
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
    fun `canPlay with empty discard is true`() {
        val deck = Deck()
        deck.shuffle()
        val hand = Hand()
        hand.add(Card.ACE_SPADES)
        assertTrue(rules.canPlay(deck, hand))
    }

    @Test
    fun `canPlay with 8 is always true`() {
        val deck = Deck()
        deck.shuffle()
        val played = Hand()
        played.add(Card.ACE_SPADES)
        deck.putdown(played)

        val hand = Hand()
        hand.add(Card.EIGHT_CLUBS)
        assertTrue(rules.canPlay(deck, hand))
    }

    @Test
    fun `canPlay matching rank`() {
        val deck = Deck()
        deck.shuffle()
        val played = Hand()
        played.add(Card.ACE_SPADES)
        deck.putdown(played)

        val hand = Hand()
        hand.add(Card.ACE_HEARTS)
        assertTrue(rules.canPlay(deck, hand))
    }

    @Test
    fun `canPlay non-matching rank is false`() {
        val deck = Deck()
        deck.shuffle()
        val played = Hand()
        played.add(Card.ACE_SPADES)
        deck.putdown(played)

        val hand = Hand()
        hand.add(Card.KING_CLUBS)
        assertFalse(rules.canPlay(deck, hand))
    }

    @Test
    fun `isValidPlay requires single card`() {
        val deck = Deck()
        deck.shuffle()
        val hand = Hand()
        hand.add(Card.ACE_SPADES)
        hand.add(Card.KING_SPADES)
        assertFalse(rules.isValidPlay(deck, hand))
    }

    @Test
    fun `isWinningPlay when hand is empty`() {
        val deck = Deck()
        val hand = Hand()
        assertTrue(rules.isWinningPlay(deck, hand))
    }

    @Test
    fun `findValidCards with empty discard returns all`() {
        val deck = Deck()
        val hand = Hand().apply { add(Card.TWO_CLUBS); add(Card.ACE_SPADES) }
        val valid = rules.findValidCards(deck, hand)
        assertEquals(2, valid.size)
    }

    @Test
    fun `findValidCards matches rank or 8s`() {
        val deck = Deck()
        deck.putdown(Hand().apply { add(Card.FIVE_CLUBS) })
        val hand = Hand().apply {
            add(Card.FIVE_HEARTS)    // rank 5, matches
            add(Card.EIGHT_SPADES)    // 8, always valid
            add(Card.KING_DIAMONDS)   // rank 13, not valid
        }
        val valid = rules.findValidCards(deck, hand)
        assertEquals(2, valid.size)
        assertTrue(valid.contains(Card.FIVE_HEARTS))
        assertTrue(valid.contains(Card.EIGHT_SPADES))
        assertFalse(valid.contains(Card.KING_DIAMONDS))
    }
}