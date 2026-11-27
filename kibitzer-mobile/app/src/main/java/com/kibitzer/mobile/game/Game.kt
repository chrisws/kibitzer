package com.kibitzer.mobile.game

enum class Game(
    val description: String,
    val minPlayers: Int = 2,
    val maxPlayers: Int = 6
) {
    WARLORDS("Classic sequence game. Play matching ranks from 3\u2663 up to 2\u2663 (J, Q, K, A are valid). Four of a kind clears the pile.", 2, 6),
    FREE("Free-for-all. Play any card higher than the last one played. Pick up if you can't.", 2, 6),
    CRAZY_EIGHTS("Match the rank or suit of the last card. 8s let you change suit. First to empty wins.", 2, 6),
    CHEAT("Bid how many of the current rank you hold. Play them truthfully or get caught and pick up.", 2, 6),
    OLD_MAID("Find and discard pairs of matching rank. Last player holding a card loses.", 2, 6),
    GO_FISH("Ask opponents for cards to complete sets of four. First to empty their hand wins.", 2, 6);

    companion object {
        fun fromName(name: String): Game? =
            entries.find { it.name.equals(name, ignoreCase = true) }
    }
}