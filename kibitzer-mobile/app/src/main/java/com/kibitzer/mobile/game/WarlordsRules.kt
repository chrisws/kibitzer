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
        if (hand.size == 0) {
            android.util.Log.d("Play", "Warlords.isValidPlay: REJECT - empty hand")
            return false
        }
        if (!hand.isEqual(hand.peek(), ::rank32)) {
            android.util.Log.d("Play", "Warlords.isValidPlay: REJECT - cards not all same rank: $hand")
            return false
        }
        if (hand.hasEqual(Card.TWO_CLUBS, ::rank32)) {
            android.util.Log.d("Play", "Warlords.isValidPlay: ACCEPT - playing 2s")
            return true
        }
        if (deck.discardSize == 0) {
            android.util.Log.d("Play", "Warlords.isValidPlay: ACCEPT - empty discard")
            return true
        }
        val last = deck.lastPlay ?: return true
        if (rank32(last) == rank32(Card.TWO_CLUBS)) {
            android.util.Log.d("Play", "Warlords.isValidPlay: ACCEPT - last was 2s")
            return true
        }
        val rankOk = hand.hasHigher(last, 1, ::rank32)
        val sizeOk = hand.size == deck.lastSize
        android.util.Log.d("Play", "Warlords.isValidPlay: checking rank: hand=$hand > last=$last? $rankOk, size: ${hand.size}==${deck.lastSize}? $sizeOk")
        return rankOk && sizeOk
    }

    override fun findValidCards(deck: Deck, hand: Hand): List<Card> {
        if (deck.discardSize == 0) return hand.toList()
        val last = deck.lastPlay ?: return hand.toList()
        val lastRank = rank32(last)
        if (lastRank == rank32(Card.TWO_CLUBS)) return hand.toList()
        return hand.toList().filter { card ->
            val r = rank32(card)
            r == rank32(Card.TWO_CLUBS) || r == lastRank || r == lastRank + 1 || r == lastRank - 1
        }
    }

    override fun isWinningPlay(deck: Deck, hand: Hand): Boolean = hand.size == 0

    override fun name(): String = "Warlords and Scumbags"

    override fun url(): String = "https://en.wikipedia.org/wiki/President_(card_game)"

    override fun noPlayTakesDiscard(): Boolean = true

    override fun setNextTurn(hand: Hand): Boolean = true
}