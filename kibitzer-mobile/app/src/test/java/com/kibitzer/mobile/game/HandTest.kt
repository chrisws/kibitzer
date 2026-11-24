package com.kibitzer.mobile.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandTest {

    @Test
    fun `empty hand has size 0`() {
        val hand = Hand()
        assertEquals(0, hand.size)
    }

    @Test
    fun `add increases size`() {
        val hand = Hand()
        hand.add(Card.ACE_SPADES)
        hand.add(Card.KING_HEARTS)
        assertEquals(2, hand.size)
    }

    @Test
    fun `has returns true for contained card`() {
        val hand = Hand()
        hand.add(Card.ACE_SPADES)
        assertTrue(hand.has(Card.ACE_SPADES))
        assertFalse(hand.has(Card.KING_SPADES))
    }

    @Test
    fun `remove removes card`() {
        val hand = Hand()
        hand.add(Card.ACE_SPADES)
        hand.add(Card.KING_SPADES)
        hand.remove(Card.ACE_SPADES)
        assertEquals(1, hand.size)
        assertFalse(hand.has(Card.ACE_SPADES))
    }

    @Test
    fun `remove hand removes all cards`() {
        val hand = Hand()
        hand.add(Card.ACE_SPADES)
        hand.add(Card.KING_SPADES)
        hand.add(Card.QUEEN_HEARTS)

        val toRemove = Hand()
        toRemove.add(Card.ACE_SPADES)
        toRemove.add(Card.KING_SPADES)

        hand.remove(toRemove)
        assertEquals(1, hand.size)
        assertTrue(hand.has(Card.QUEEN_HEARTS))
    }

    @Test
    fun `sort orders by rank`() {
        val hand = Hand()
        hand.add(Card.ACE_SPADES)
        hand.add(Card.TWO_CLUBS)
        hand.add(Card.KING_HEARTS)

        hand.sort { it.rank }
        assertEquals(Card.TWO_CLUBS, hand.get(0))
        assertEquals(Card.KING_HEARTS, hand.get(1))
        assertEquals(Card.ACE_SPADES, hand.get(2))
    }

    @Test
    fun `toJson and fromJson roundtrip`() {
        val hand = Hand()
        hand.add(Card.ACE_SPADES)
        hand.add(Card.TWO_CLUBS)

        val json = hand.toJson()
        val restored = Hand.fromJson(json)

        assertEquals(hand.size, restored.size)
        assertTrue(restored.has(Card.ACE_SPADES))
        assertTrue(restored.has(Card.TWO_CLUBS))
    }

    @Test
    fun `fromJson with empty string returns empty hand`() {
        val hand = Hand.fromJson("")
        assertEquals(0, hand.size)
    }

    @Test
    fun `copy constructor creates independent copy`() {
        val hand = Hand()
        hand.add(Card.ACE_SPADES)

        val copy = Hand(hand)
        copy.add(Card.KING_SPADES)

        assertEquals(1, hand.size)
        assertEquals(2, copy.size)
    }

    @Test
    fun `peek returns last card`() {
        val hand = Hand()
        hand.add(Card.ACE_SPADES)
        hand.add(Card.KING_SPADES)
        assertEquals(Card.KING_SPADES, hand.peek())
    }

    @Test
    fun `pop removes and returns last card`() {
        val hand = Hand()
        hand.add(Card.ACE_SPADES)
        hand.add(Card.KING_SPADES)
        assertEquals(Card.KING_SPADES, hand.pop())
        assertEquals(1, hand.size)
    }
}