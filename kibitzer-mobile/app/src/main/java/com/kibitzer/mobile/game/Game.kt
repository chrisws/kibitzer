package com.kibitzer.mobile.game

enum class Game {
    WARLORDS,
    FREE,
    CRAZY_EIGHTS,
    CHEAT,
    OLD_MAID,
    GO_FISH;

    companion object {
        fun fromName(name: String): Game? =
            entries.find { it.name.equals(name, ignoreCase = true) }
    }
}