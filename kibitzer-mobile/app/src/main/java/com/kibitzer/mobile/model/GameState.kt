package com.kibitzer.mobile.model

import com.kibitzer.mobile.game.Card
import com.kibitzer.mobile.game.Game

data class PlayerInfo(
    val sessionId: Int = -1,
    val nickname: String = "",
    val active: Boolean = false,
    val isAi: Boolean = false,
    val handSize: Int = 0,
    val points: Int = 0
)

data class DiscoveredGame(
    val address: String,
    val name: String = ""
)

enum class CheatPhase {
    BIDDING,
    PLAYING,
    CALLED,
    RESOLVED
}

enum class Screen {
    HOME,
    LOBBY,
    AI_LOBBY,
    GAME,
    GAME_OVER,
    HELP,
    CONNECTION_HELP,
    GAME_RULES,
    ABOUT
}

enum class GameMode {
    BLE,
    AI
}

data class GameState(
    val screen: Screen = Screen.HOME,
    val game: Game = Game.WARLORDS,
    val gameMode: GameMode = GameMode.BLE,
    val players: List<PlayerInfo> = emptyList(),
    val mySessionId: Int = -1,
    val myHand: List<Card> = emptyList(),
    val discardPile: List<Card> = emptyList(),
    val currentTurn: Int = -1,
    val message: String = "",
    val faceDown: Boolean = false,
    val isGameOver: Boolean = false,
    val winner: String = "",
    val cheatPhase: CheatPhase = CheatPhase.BIDDING,
    val activeSuit: Char? = null,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val chatLog: List<String> = emptyList(),
    val discoveredGames: List<DiscoveredGame> = emptyList()
) {
    val myTurn: Boolean
        get() = currentTurn == mySessionId

    val myPlayer: PlayerInfo?
        get() = players.find { it.sessionId == mySessionId }
}