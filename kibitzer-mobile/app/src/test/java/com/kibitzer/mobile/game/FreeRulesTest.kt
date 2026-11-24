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
}