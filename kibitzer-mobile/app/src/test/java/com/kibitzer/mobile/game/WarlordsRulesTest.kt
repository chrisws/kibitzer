package com.kibitzer.mobile.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WarlordsRulesTest {

    private val rules = WarlordsRules()

    @Test
    fun `name is correct`() {
        assertEquals("Warlords and Scumbags", rules.name())
    }

    @Test
    fun `handSize divides 52 by players`() {
        assertEquals(26, rules.handSize(2))
        assertEquals(13, rules.handSize(4))
        assertEquals(10, rules.handSize(5))
    }

    @Test
    fun `canPlay with empty hand is false`() {
        val deck = Deck()
        deck.shuffle()
        val hand = Hand()
        assertFalse(rules.canPlay(deck, hand))
    }

    @Test
    fun `canPlay with 2C is always true`() {
        val deck = Deck()
        deck.shuffle()
        val hand = Hand()
        hand.add(Card.TWO_CLUBS)
        assertTrue(rules.canPlay(deck, hand))
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
    fun `canPlay requires higher rank than last play`() {
        val deck = Deck()
        deck.shuffle()
        val played = Hand()
        played.add(Card.ACE_SPADES)
        deck.putdown(played)

        val hand = Hand()
        hand.add(Card.TWO_CLUBS)
        // 2C is always playable
        assertTrue(rules.canPlay(deck, hand))

        val hand2 = Hand()
        hand2.add(Card.THREE_CLUBS)
        // 3 is lower than A in rank32 (A=11, 3=0)
        assertFalse(rules.canPlay(deck, hand2))
    }

    @Test
    fun `clearDiscard on 3C`() {
        val hand = Hand()
        hand.add(Card.THREE_CLUBS)
        assertTrue(rules.clearDiscard(hand))
    }

    @Test
    fun `clearDiscard on 4 cards`() {
        val hand = Hand()
        hand.add(Card.FOUR_CLUBS)
        hand.add(Card.FIVE_CLUBS)
        hand.add(Card.SIX_CLUBS)
        hand.add(Card.SEVEN_CLUBS)
        assertTrue(rules.clearDiscard(hand))
    }

    @Test
    fun `noPlayTakesDiscard is true`() {
        assertTrue(rules.noPlayTakesDiscard())
    }

    @Test
    fun `faceDown is false`() {
        assertFalse(rules.faceDown())
    }

    @Test
    fun `isWinningPlay when hand is empty`() {
        val deck = Deck()
        val hand = Hand()
        assertTrue(rules.isWinningPlay(deck, hand))
    }

    @Test
    fun `findValidCards with empty discard returns all cards`() {
        val deck = Deck()
        val hand = Hand().apply { add(Card.TWO_CLUBS); add(Card.FIVE_HEARTS) }
        val valid = rules.findValidCards(deck, hand)
        assertEquals(2, valid.size)
    }

    @Test
    fun `findValidCards filters to matching ranks`() {
        val deck = Deck()
        deck.putdown(Hand().apply { add(Card.FIVE_CLUBS) })
        val hand = Hand().apply {
            add(Card.FOUR_HEARTS)   // rank 4 = 5-1, valid
            add(Card.FIVE_SPADES)    // rank 5 = match, valid
            add(Card.SIX_DIAMONDS)   // rank 6 = 5+1, valid
            add(Card.TWO_CLUBS)      // 2s always valid
            add(Card.TEN_HEARTS)     // rank 10, not valid
        }
        val valid = rules.findValidCards(deck, hand)
        assertEquals(4, valid.size)
        assertTrue(valid.contains(Card.FOUR_HEARTS))
        assertTrue(valid.contains(Card.FIVE_SPADES))
        assertTrue(valid.contains(Card.SIX_DIAMONDS))
        assertTrue(valid.contains(Card.TWO_CLUBS))
        assertFalse(valid.contains(Card.TEN_HEARTS))
    }

    @Test
    fun `findValidCards returns all when last was 2s`() {
        val deck = Deck()
        deck.putdown(Hand().apply { add(Card.TWO_CLUBS) })
        val hand = Hand().apply { add(Card.ACE_SPADES); add(Card.THREE_HEARTS) }
        val valid = rules.findValidCards(deck, hand)
        assertEquals(2, valid.size)
    }

    // --- invalidReason tests ---

    @Test
    fun `invalidReason for empty hand`() {
        val deck = Deck()
        deck.shuffle()
        val hand = Hand()
        assertEquals("You have no cards to play", rules.invalidReason(deck, hand))
    }

    @Test
    fun `invalidReason for mixed ranks`() {
        val deck = Deck()
        deck.shuffle()
        deck.putdown(Hand().apply { add(Card.FIVE_CLUBS) })
        val hand = Hand().apply {
            add(Card.FIVE_HEARTS)
            add(Card.SIX_SPADES)
        }
        assertEquals("All cards must be the same rank", rules.invalidReason(deck, hand))
    }

    @Test
    fun `invalidReason for wrong card count`() {
        val deck = Deck()
        deck.shuffle()
        val played = Hand().apply {
            add(Card.NINE_CLUBS)
            add(Card.NINE_HEARTS)
            add(Card.NINE_SPADES)
        }
        deck.putdown(played)
        val hand = Hand().apply { add(Card.TEN_CLUBS) }
        assertEquals("You must play 3 cards (last play was 3)", rules.invalidReason(deck, hand))
    }

    @Test
    fun `invalidReason for rank too low`() {
        val deck = Deck()
        deck.shuffle()
        deck.putdown(Hand().apply { add(Card.ACE_CLUBS) })
        val hand = Hand().apply { add(Card.THREE_CLUBS) }
        assertEquals("Your rank is not high enough to beat Aces", rules.invalidReason(deck, hand))
    }

    @Test
    fun `invalidReason returns generic for empty discard edge case`() {
        val deck = Deck()
        deck.shuffle()
        // Empty discard - should be valid, so reason is the fallback
        val hand = Hand().apply { add(Card.FIVE_CLUBS) }
        assertEquals("Should be valid on an empty discard", rules.invalidReason(deck, hand))
    }
}