package com.kibitzer.mobile.game

class CrazyEightsRules : Rules() {

    override fun canPlay(deck: Deck, hand: Hand): Boolean {
        if (hand.size == 0) return false
        if (deck.discardSize == 0) return true
        val last = deck.lastPlay ?: return true
        if (hand.hasEqual(Card.EIGHT_CLUBS, ::rank2A)) return true
        return hand.hasEqual(last, ::rank2A)
    }

    override fun canRevoke(): Boolean = false

    override fun clearDiscard(hand: Hand): Boolean = false

    override fun faceDown(): Boolean = false

    override fun getRank(): CardRank = ::rank2A

    override fun handSize(players: Int): Int = if (players <= 2) 7 else 5

    override fun isValidPlay(deck: Deck, hand: Hand): Boolean {
        if (hand.size == 0) return false
        if (hand.size != 1) return false
        val card = hand.peek()
        if (deck.discardSize == 0) return true
        val last = deck.lastPlay ?: return true
        if (rank2A(card) == rank2A(Card.EIGHT_CLUBS)) return true
        return rank2A(card) == rank2A(last)
    }

    override fun findValidCards(deck: Deck, hand: Hand): List<Card> {
        if (deck.discardSize == 0) return hand.toList()
        val last = deck.lastPlay ?: return hand.toList()
        return hand.toList().filter { card ->
            rank2A(card) == rank2A(Card.EIGHT_CLUBS) || rank2A(card) == rank2A(last)
        }
    }

    override fun isWinningPlay(deck: Deck, hand: Hand): Boolean = hand.size == 0

    override fun name(): String = "Crazy Eights"

    override fun url(): String = "https://en.wikipedia.org/wiki/Crazy_Eights"

    override fun noPlayTakesDiscard(): Boolean = true

    override fun setNextTurn(hand: Hand): Boolean = true
}