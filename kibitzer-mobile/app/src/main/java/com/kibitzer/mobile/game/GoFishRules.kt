package com.kibitzer.mobile.game

class GoFishRules : Rules() {

    override fun canPlay(deck: Deck, hand: Hand): Boolean {
        if (hand.size == 0) return false
        return true
    }

    override fun canRevoke(): Boolean = false

    override fun clearDiscard(hand: Hand): Boolean = false

    override fun faceDown(): Boolean = true

    override fun getRank(): CardRank = ::rank2A

    override fun handSize(players: Int): Int = if (players <= 3) 7 else 5

    override fun isValidPlay(deck: Deck, hand: Hand): Boolean = true

    override fun isWinningPlay(deck: Deck, hand: Hand): Boolean = hand.size == 0

    override fun name(): String = "Go Fish"

    override fun url(): String = "https://en.wikipedia.org/wiki/Go_Fish"

    override fun noPlayTakesDiscard(): Boolean = false

    override fun setNextTurn(hand: Hand): Boolean = true
}