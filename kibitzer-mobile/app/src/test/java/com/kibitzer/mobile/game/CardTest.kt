package com.kibitzer.mobile.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CardTest {

    @Test
    fun `card count is 52`() {
        assertEquals(52, Card.entries.size)
    }

    @Test
    fun `rank values are correct`() {
        assertEquals(2, Card.TWO_CLUBS.rank)
        assertEquals(10, Card.TEN_CLUBS.rank)
        assertEquals(11, Card.JACK_CLUBS.rank)
        assertEquals(12, Card.QUEEN_CLUBS.rank)
        assertEquals(13, Card.KING_CLUBS.rank)
        assertEquals(14, Card.ACE_CLUBS.rank)
    }

    @Test
    fun `suit values are correct`() {
        assertEquals('C', Card.TWO_CLUBS.suit)
        assertEquals('D', Card.TWO_DIAMONDS.suit)
        assertEquals('H', Card.TWO_HEARTS.suit)
        assertEquals('S', Card.TWO_SPADES.suit)
    }

    @Test
    fun `fromName returns correct card`() {
        assertEquals(Card.ACE_SPADES, Card.fromName("AS"))
        assertEquals(Card.TWO_CLUBS, Card.fromName("2C"))
        assertEquals(Card.TEN_HEARTS, Card.fromName("XH"))
    }

    @Test
    fun `fromName returns null for invalid name`() {
        assertNull(Card.fromName("ZZ"))
        assertNull(Card.fromName(""))
    }

    @Test
    fun `rank2A maps 2 to 0 and A to 12`() {
        assertEquals(0, rank2A(Card.TWO_CLUBS))
        assertEquals(12, rank2A(Card.ACE_CLUBS))
        assertEquals(6, rank2A(Card.EIGHT_CLUBS))
    }

    @Test
    fun `rank32 maps 3 to 0 and 2 to 12`() {
        assertEquals(0, rank32(Card.THREE_CLUBS))
        assertEquals(12, rank32(Card.TWO_CLUBS))
        assertEquals(11, rank32(Card.ACE_CLUBS))
    }
}