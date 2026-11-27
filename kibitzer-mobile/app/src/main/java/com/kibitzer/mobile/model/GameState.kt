/*
 * This file is part of kibitzer
 *
 * Copyright (C) 2026 Chris Warren-Smith. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kibitzer.mobile.model

import com.kibitzer.mobile.game.Card
import com.kibitzer.mobile.game.Game

data class PlayerInfo(
  val sessionId: Int = -1,
  val nickname: String = "",
  val active: Boolean = false,
  val isAi: Boolean = false,
  val handSize: Int = 0,
  val points: Int = 0,
)

data class DiscoveredGame(
  val address: String,
  val name: String = "",
)

enum class CheatPhase {
  BIDDING,
  PLAYING,
  CALLED,
  RESOLVED,
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
  ABOUT,
  CHAT,
}

enum class GameMode {
  BLE,
  AI,
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
  val discoveredGames: List<DiscoveredGame> = emptyList(),
) {
  val myTurn: Boolean
    get() = currentTurn == mySessionId

  val myPlayer: PlayerInfo?
    get() = players.find { it.sessionId == mySessionId }
}
