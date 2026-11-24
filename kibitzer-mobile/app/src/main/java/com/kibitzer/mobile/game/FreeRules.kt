package com.kibitzer.mobile.game

class FreeRules : Rules() {

    override fun canPlay(deck: Deck, hand: Hand): Boolean {
        if (hand.size == 0) return false
        if (deck.discardSize == 0) return true
        val last = deck.lastPlay ?: return true
        return hand.hasHigher(last, 1, ::rank2A)
    }

    override fun canRevoke(): Boolean = true

    override fun clearDiscard(hand: Hand): Boolean = false

    override fun faceDown(): Boolean = false

    override fun getRank(): CardRank = ::rank2A

    override fun handSize(players: Int): Int = 7

    override fun isValidPlay(deck: Deck, hand: Hand): Boolean = true

    override fun isWinningPlay(deck: Deck, hand: Hand): Boolean = false

    override fun name(): String = "Rules Free"

    override fun url(): String = "https://en.wikipedia.org/wiki/Card_game"

    override fun noPlayTakesDiscard(): Boolean = false

    override fun setNextTurn(hand: Hand): Boolean = true
}