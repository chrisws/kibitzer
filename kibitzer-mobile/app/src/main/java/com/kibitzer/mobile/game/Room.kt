package com.kibitzer.mobile.game

class Room(val maxSlots: Int = 6) {
    val deck = Deck()
    val slots = MutableList(maxSlots) { -1 }
    var rules: Rules = WarlordsRules()
    var turn: Int = -1
    var cheatState: CheatState = CheatState()
    var activeSuit: Char? = null

    var saveDeck: Deck? = null
    var saveHand: Hand? = null
    var saveSessionId: Int = -1

    fun saveState(player: Player) {
        saveDeck = deck.copy()
        saveHand = Hand(player.hand)
        saveSessionId = player.sessionId
    }

    fun restoreState(players: List<Player>) {
        saveDeck?.let { saved ->
            deck.pack.clear()
            deck.pack.addAll(saved.pack)
            deck.discard.clear()
            deck.discard.addAll(saved.discard)
            deck.lastPlay = saved.lastPlay
            deck.lastSize = saved.lastSize
        }
        saveHand?.let { savedHand ->
            val player = players.find { it.sessionId == saveSessionId }
            player?.hand?.let { h ->
                h.clear()
                h.addAll(savedHand)
            }
        }
        saveDeck = null
        saveHand = null
        saveSessionId = -1
    }
}