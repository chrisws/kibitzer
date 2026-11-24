package com.kibitzer.mobile.game

class WarlordsRules : Rules() {

    override fun canPlay(deck: Deck, hand: Hand): Boolean {
        if (hand.size == 0) return false
        if (hand.hasEqual(Card.TWO_CLUBS, ::rank32)) return true
        if (deck.discardSize == 0) return true
        val last = deck.lastPlay ?: return true
        if (rank32(last) == rank32(Card.TWO_CLUBS)) return true
        return hand.hasHigher(last, deck.lastSize, ::rank32)
    }

    override fun canRevoke(): Boolean = false

    override fun clearDiscard(hand: Hand): Boolean {
        return hand.hasEqual(Card.THREE_CLUBS, ::rank32)
            || hand.hasEqual(Card.TEN_CLUBS, ::rank32)
            || hand.size == 4
    }

    override fun faceDown(): Boolean = false

    override fun getRank(): CardRank = ::rank32

    override fun handSize(players: Int): Int = 52 / players

    override fun isValidPlay(deck: Deck, hand: Hand): Boolean {
        if (hand.size == 0) return false
        if (!hand.isEqual(hand.peek(), ::rank32)) return false
        if (hand.hasEqual(Card.TWO_CLUBS, ::rank32)) return true
        if (deck.discardSize == 0) return true
        val last = deck.lastPlay ?: return true
        if (rank32(last) == rank32(Card.TWO_CLUBS)) return true
        return hand.hasHigher(last, 1, ::rank32) && hand.size == deck.lastSize
    }

    override fun isWinningPlay(deck: Deck, hand: Hand): Boolean = hand.size == 0

    override fun name(): String = "Warlords and Scumbags"

    override fun url(): String = "https://en.wikipedia.org/wiki/President_(card_game)"

    override fun noPlayTakesDiscard(): Boolean = true

    override fun setNextTurn(hand: Hand): Boolean = !clearDiscard(hand)
}