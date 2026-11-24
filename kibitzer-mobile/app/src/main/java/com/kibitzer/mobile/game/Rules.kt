package com.kibitzer.mobile.game

abstract class Rules {
    abstract fun canPlay(deck: Deck, hand: Hand): Boolean
    abstract fun canRevoke(): Boolean
    abstract fun clearDiscard(hand: Hand): Boolean
    abstract fun faceDown(): Boolean
    abstract fun getRank(): CardRank
    abstract fun handSize(players: Int): Int
    abstract fun isValidPlay(deck: Deck, hand: Hand): Boolean
    abstract fun isWinningPlay(deck: Deck, hand: Hand): Boolean
    abstract fun name(): String
    abstract fun url(): String
    abstract fun noPlayTakesDiscard(): Boolean
    abstract fun setNextTurn(hand: Hand): Boolean
}