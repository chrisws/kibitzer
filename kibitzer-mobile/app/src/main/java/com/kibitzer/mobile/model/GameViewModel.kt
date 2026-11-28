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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.kibitzer.mobile.game.AiPlayer
import com.kibitzer.mobile.game.Card
import com.kibitzer.mobile.game.ChatBot
import com.kibitzer.mobile.game.Controller
import com.kibitzer.mobile.game.Game
import com.kibitzer.mobile.game.Hand
import com.kibitzer.mobile.game.Message
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(
  private val connection: GameConnection,
) : ViewModel() {
  private val _state = MutableStateFlow(GameState())
  val state: StateFlow<GameState> = _state.asStateFlow()

  private val gson = Gson()

  // --- Local AI mode ---
  private var controller: Controller? = null
  private var aiPlayerList: List<AiPlayer> = emptyList()
  private var chatBotList: List<ChatBot> = emptyList()
  private var aiSessionIds: List<Int> = emptyList()
  private var humanSessionId: Int = -1

  init {
    connection.setOnStateUpdate { json -> handleStateUpdate(json) }
    connection.setOnChatUpdate { json -> handleChatUpdate(json) }
    connection.setOnHandshake { json -> handleHandshake(json) }
    connection.setOnConnectionState { connected ->
      _state.value =
        _state.value.copy(
          connectionState = if (connected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED,
        )
    }
    connection.setOnScanResult { address ->
      val current = _state.value
      if (address !in current.discoveredGames.map { it.address }) {
        _state.value =
          current.copy(
            discoveredGames = current.discoveredGames + DiscoveredGame(address),
          )
      }
    }
  }

  fun connect() {
    _state.value =
      _state.value.copy(
        connectionState = ConnectionState.SCANNING,
        discoveredGames = emptyList(),
      )
    connection.startScan()
  }

  fun stopScanning() {
    connection.stopScan()
    _state.value = _state.value.copy(connectionState = ConnectionState.DISCONNECTED)
  }

  fun joinGame(address: String) {
    _state.value = _state.value.copy(connectionState = ConnectionState.CONNECTING)
    connection.sendCommand("join:$address")
  }

  fun hostGame() {
    connection.disconnect()
    _state.value =
      _state.value.copy(
        connectionState = ConnectionState.CONNECTED,
        discoveredGames = emptyList(),
      )
  }

  fun startAiGame(
    game: Game,
    numAiPlayers: Int = 1,
    difficulty: AiPlayer.Difficulty = AiPlayer.Difficulty.MEDIUM,
    nickname: String = "You",
  ) {
    val ctrl = Controller()
    val aiPlayers = (1..numAiPlayers).map { AiPlayer(difficulty = difficulty) }
    val bots = (1..numAiPlayers).map { ChatBot() }

    controller = ctrl
    aiPlayerList = aiPlayers
    chatBotList = bots

    // Create sessions
    humanSessionId = ctrl.createSession()
    aiSessionIds = (1..numAiPlayers).map { ctrl.createSession() }

    // Set nicknames
    ctrl.handle("nicn:You", humanSessionId)
    aiSessionIds.forEachIndexed { i, sid ->
      ctrl.handle("nicn:${aiPlayers[i].name}", sid)
    }

    // Set game
    ctrl.setGame(game)

    // Join all players
    ctrl.handle("join:0", humanSessionId)
    ctrl.handle("nicn:$nickname", humanSessionId)
    aiSessionIds.forEachIndexed { i, sid ->
      ctrl.handle("join:${i + 1}", sid)
      ctrl.handle("nicn:${aiPlayers[i].name}", sid)
    }

    // Shuffle and deal
    ctrl.handle("shuf:", humanSessionId)
    val dealMsg = ctrl.handle("deal:", humanSessionId)
    Log.d("GameVM", "startAiGame: dealMsg.id=${dealMsg.id}, dealMsg.data=${dealMsg.data}")

    // Build player list with hand sizes
    val playerList =
      mutableListOf(
        PlayerInfo(humanSessionId, nickname, true, false, 0, 0),
      )
    aiSessionIds.forEachIndexed { i, sid ->
      val ctrlPlayer = ctrl.findSession(sid)
      playerList.add(
        PlayerInfo(sid, aiPlayers[i].name, true, true, ctrlPlayer?.hand?.size ?: 0, 0),
      )
    }

    // Update state
    val gameName = game.name.replace("_", " ")
    val greeting = "I'm your game companion! Ask me anything about $gameName. The rules are ${game.description}."
    _state.value =
      _state.value.copy(
        gameMode = GameMode.AI,
        game = game,
        mySessionId = humanSessionId,
        connectionState = ConnectionState.CONNECTED,
        screen = Screen.GAME,
        players = playerList,
        chatLog = listOf("${aiPlayers[0].name}: $greeting"),
      )

    // Sync state from controller
    syncStateFromController(dealMsg)
    Log.d(
      "GameVM",
      "startAiGame: AFTER sync - discardPile.size=${_state.value.discardPile.size}, myHand.size=${_state.value.myHand.size}",
    )

    // If it's an AI's turn, let it play
    if (aiSessionIds.contains(_state.value.currentTurn) && !_state.value.isGameOver) {
      viewModelScope.launch { aiTurn() }
    }
  }

  fun stopAiGame() {
    controller = null
    aiPlayerList = emptyList()
    chatBotList = emptyList()
    aiSessionIds = emptyList()
    humanSessionId = -1
    _state.value =
      _state.value.copy(
        gameMode = GameMode.BLE,
        screen = Screen.HOME,
        myHand = emptyList(),
        discardPile = emptyList(),
        currentTurn = -1,
        message = "",
        isGameOver = false,
        chatLog = emptyList(),
        players = emptyList(),
      )
  }

  fun disconnect() {
    connection.disconnect()
    _state.value =
      _state.value.copy(
        connectionState = ConnectionState.DISCONNECTED,
        discoveredGames = emptyList(),
      )
  }

  fun join(slot: Int) {
    connection.sendCommand("join:$slot")
  }

  fun setNickname(name: String) {
    connection.sendCommand("nicn:$name")
  }

  fun setGame(game: Game) {
    connection.sendCommand("game:${game.name}")
    _state.value = _state.value.copy(game = game)
  }

  fun shuffle() {
    if (_state.value.gameMode == GameMode.AI) {
      val ctrl = controller ?: return
      ctrl.handle("shuf:", humanSessionId)
    } else {
      connection.sendCommand("shuf:")
    }
  }

  fun deal() {
    if (_state.value.gameMode == GameMode.AI) {
      val ctrl = controller ?: return
      val msg = ctrl.handle("deal:", humanSessionId)
      syncStateFromController(msg)
      if (aiSessionIds.contains(_state.value.currentTurn) && !_state.value.isGameOver) {
        viewModelScope.launch { aiTurn() }
      }
    } else {
      connection.sendCommand("deal:")
    }
  }

  fun pickup(count: Int = 1) {
    if (_state.value.gameMode == GameMode.AI) {
      val ctrl = controller ?: return
      val msg = ctrl.handle("picu:$count", humanSessionId)
      if (msg.id == "message") {
        aiSay(msg.data as? String ?: "Something went wrong.")
        return
      }
      syncStateFromController(msg)
      aiSay("Picked up $count card${if (count > 1) "s" else ""}.")
      if (aiSessionIds.contains(_state.value.currentTurn) && !_state.value.isGameOver) {
        viewModelScope.launch { aiTurn() }
      }
    } else {
      connection.sendCommand("picu:$count")
    }
  }

  fun putdown(cards: List<Card>): Boolean {
    val hand = Hand()
    cards.forEach { hand.add(it) }
    if (_state.value.gameMode == GameMode.AI) {
      val ctrl = controller ?: return false
      Log.d(
        "Play",
        "putdown: hand=${hand.toJson()}, mySession=$humanSessionId, currentTurn=${_state.value.currentTurn}, myTurn=${_state.value.myTurn}",
      )
      val msg = ctrl.handle("putd:${hand.toJson()}", humanSessionId)
      Log.d("Play", "putdown: response id='${msg.id}', data='${msg.data}'")
      if (msg.id == "message") {
        val room = ctrl.rooms[0]
        val myHand = ctrl.findSession(humanSessionId)?.hand ?: Hand()
        val reason = room.rules.invalidReason(room.deck, hand)
        val validCards = room.rules.findValidCards(room.deck, myHand)
        val hint =
          if (validCards.isNotEmpty()) {
            val names = validCards.joinToString(", ") { it.code }
            "$reason — try: $names"
          } else {
            "$reason — no valid plays, pick up a card."
          }
        _state.value = _state.value.copy(message = hint)
        aiSay(hint)
        return false
      }
      _state.value = _state.value.copy(message = "")
      syncStateFromController(msg)
      if (_state.value.message.isNotBlank()) {
        _state.value = _state.value.copy(chatLog = _state.value.chatLog + _state.value.message)
      }
      if (aiSessionIds.contains(_state.value.currentTurn) && !_state.value.isGameOver) {
        viewModelScope.launch { aiTurn() }
      }
      return true
    } else {
      connection.sendCommand("putd:${hand.toJson()}")
      return true
    }
  }

  fun skip() {
    if (_state.value.gameMode == GameMode.AI) {
      val ctrl = controller ?: return
      val msg = ctrl.handle("skip:", humanSessionId)
      if (msg.id == "cards") {
        aiSay("You skipped your turn.")
      }
      syncStateFromController(msg)
      if (aiSessionIds.contains(_state.value.currentTurn) && !_state.value.isGameOver) {
        viewModelScope.launch { aiTurn() }
      }
    } else {
      connection.sendCommand("skip:")
    }
  }

  fun chat(text: String) {
    if (_state.value.gameMode == GameMode.AI) {
      _state.value = _state.value.copy(chatLog = _state.value.chatLog + "You: $text")
      if (chatBotList.isNotEmpty() && aiPlayerList.isNotEmpty()) {
        val response = chatBotList[0].respond(text)
        aiSay(response)
      }
    } else {
      connection.sendCommand("chat:$text")
    }
  }

  private fun aiSay(text: String) {
    if (aiPlayerList.isEmpty()) return
    val name = aiPlayerList[0].name
    _state.value = _state.value.copy(chatLog = _state.value.chatLog + "$name: $text")
  }

  fun bid(count: Int) {
    if (_state.value.gameMode == GameMode.AI) {
      val ctrl = controller ?: return
      val msg = ctrl.handle("bid:$count", humanSessionId)
      syncStateFromController(msg)
      if (aiSessionIds.contains(_state.value.currentTurn) && !_state.value.isGameOver) {
        viewModelScope.launch { aiTurn() }
      }
    } else {
      connection.sendCommand("bid:$count")
    }
  }

  fun cheat(targetId: Int) {
    if (_state.value.gameMode == GameMode.AI) {
      val ctrl = controller ?: return
      val msg = ctrl.handle("cheat:$targetId", humanSessionId)
      syncStateFromController(msg)
      if (aiSessionIds.contains(_state.value.currentTurn) && !_state.value.isGameOver) {
        viewModelScope.launch { aiTurn() }
      }
    } else {
      connection.sendCommand("cheat:$targetId")
    }
  }

  fun ask(data: String) {
    if (_state.value.gameMode == GameMode.AI) {
      val ctrl = controller ?: return
      val msg = ctrl.handle("ask:$data", humanSessionId)
      syncStateFromController(msg)
      if (aiSessionIds.contains(_state.value.currentTurn) && !_state.value.isGameOver) {
        viewModelScope.launch { aiTurn() }
      }
    } else {
      connection.sendCommand("ask:$data")
    }
  }

  fun navigateTo(screen: Screen) {
    _state.value = _state.value.copy(screen = screen)
  }

  fun clearMessage() {
    _state.value = _state.value.copy(message = "")
  }

  fun onCheatPhaseChange(phase: CheatPhase) {
    _state.value = _state.value.copy(cheatPhase = phase)
  }

  // --- Internal handlers ---

  private var isAiTurnActive = false

  private suspend fun aiTurn(depth: Int = 0) {
    if (depth > 20) {
      Log.w("GameVM", "aiTurn: max depth reached, stopping")
      return
    }
    if (isAiTurnActive) {
      Log.w("GameVM", "aiTurn: already active, skipping")
      return
    }
    isAiTurnActive = true
    try {
      delay(1500)
    val ctrl = controller ?: return
    val currentTurn = _state.value.currentTurn
    val aiIndex = aiSessionIds.indexOf(currentTurn)
    if (aiIndex == -1) return

    val ai = aiPlayerList[aiIndex]
    var command = ai.decide(ctrl, currentTurn)
    Log.d("GameVM", "aiTurn: depth=$depth, turn=$currentTurn, command=$command")
    var msg = ctrl.handle(command, currentTurn)

    // If play was rejected, fall back to pickup
    if (msg.id == "message" && command.startsWith("putd")) {
      Log.d("GameVM", "aiTurn: play rejected, falling back to pickup")
      command = "picu:1"
      msg = ctrl.handle(command, currentTurn)
    }

    // If pickup also failed, skip the turn to avoid getting stuck
    if (msg.id == "message") {
      Log.d("GameVM", "aiTurn: all actions failed, skipping turn")
      aiSay("${ai.name} skipped their turn.")
      msg = ctrl.handle("skip:", currentTurn)
    }

    syncStateFromController(msg)

    // Narrate what happened
    if (msg.id == "cards") {
      aiSay(_state.value.message.ifBlank { "${ai.name} played." })
    }

    // If another AI needs to play and turn actually changed, recurse
    val newTurn = _state.value.currentTurn
    if (newTurn != currentTurn && aiSessionIds.contains(newTurn) && !_state.value.isGameOver) {
      aiTurn(depth + 1)
    } else if (newTurn == humanSessionId && !_state.value.isGameOver) {
      aiSay("Your turn!")
    }
    } finally {
      isAiTurnActive = false
    }
  }

  private fun syncStateFromController(msg: Message) {
    Log.d("GameVM", "syncStateFromController: msg.id=${msg.id}, data=${msg.data}")
    if (msg.id != "cards") {
      Log.d("GameVM", "syncStateFromController: SKIP - id is '${msg.id}', not 'cards'")
      return
    }
    val data = msg.data as? String ?: return
    val obj: JsonObject? =
      try {
        gson.fromJson(data, JsonObject::class.java)
      } catch (e: JsonSyntaxException) {
        Log.e("GameVM", "syncStateFromController: malformed JSON: ${e.message}")
        null
      }
    if (obj == null) return
    Log.d("GameVM", "syncStateFromController: parsed obj=$obj")

    // Always read the human player's hand from the controller,
    // because the "hand" field in the message reflects the
    // acting player (which may be an AI), not the human.
    val newHand =
      controller?.findSession(humanSessionId)?.hand?.toList()
        ?: parseCardList(obj, "hand")
    val newPile = parseCardList(obj, "pile")
    Log.d("GameVM", "syncState: newHand.size=${newHand.size}, newPile.size=${newPile.size}")
    val turn = obj.get("turn")?.asInt ?: -1
    val rawMessage = obj.get("message")?.asString ?: ""
    val message = if (rawMessage == "cards dealt") "" else rawMessage
    val faceDown = obj.get("faceDown")?.asBoolean ?: false
    val gameName = obj.get("game")?.asString
    val game = gameName?.let { Game.fromName(it) } ?: _state.value.game

    // Update player hand sizes from controller
    val ctrl = controller
    val updatedPlayers =
      _state.value.players.map { p ->
        if (ctrl != null) {
          val ctrlPlayer = ctrl.findSession(p.sessionId)
          if (ctrlPlayer != null) {
            p.copy(handSize = ctrlPlayer.hand.size)
          } else {
            p
          }
        } else {
          p
        }
      }

    val isOver = turn == -1
    val winner =
      if (isOver) {
        // Extract winner name from "(Name) won the round! Score:..."
        val raw = message.substringBefore(" won the round!").ifBlank { "" }
        raw.removeSurrounding("(", ")")
      } else {
        ""
      }
    _state.value =
      _state.value.copy(
        myHand = newHand,
        discardPile = newPile,
        currentTurn = turn,
        message = message,
        faceDown = faceDown,
        game = game,
        players = updatedPlayers,
        isGameOver = isOver,
        winner = winner,
      )
  }

  private fun handleStateUpdate(json: String) {
    try {
      val msg = parseMessage(json)
      if (msg == null) return
      val data = msg.data as? String ?: return
      val obj = gson.fromJson(data, JsonObject::class.java) ?: return

      val newHand = parseCardList(obj, "hand")
      val newPile = parseCardList(obj, "pile")
      val turn = obj.get("turn")?.asInt ?: -1
      val message = obj.get("message")?.asString ?: ""
      val faceDown = obj.get("faceDown")?.asBoolean ?: false
      val gameName = obj.get("game")?.asString
      val sessionId = obj.get("sessionId")?.asInt ?: _state.value.mySessionId

      val game = gameName?.let { Game.fromName(it) } ?: _state.value.game

      _state.value =
        _state.value.copy(
          myHand = newHand,
          discardPile = newPile,
          currentTurn = turn,
          message = message,
          faceDown = faceDown,
          game = game,
          mySessionId = sessionId,
          isGameOver = turn == -1,
        )
    } catch (e: Exception) {
      _state.value = _state.value.copy(message = "Parse error: ${e.message}")
    }
  }

  private fun handleChatUpdate(json: String) {
    try {
      val msg = parseMessage(json) ?: return
      val data = msg.data as? String ?: return
      val obj = gson.fromJson(data, JsonObject::class.java) ?: return
      val text = obj.get("message")?.asString ?: data
      _state.value =
        _state.value.copy(
          chatLog = _state.value.chatLog + text,
        )
    } catch (e: Exception) {
      // Ignore chat parse errors
    }
  }

  private fun handleHandshake(json: String) {
    try {
      val obj = gson.fromJson(json, JsonObject::class.java) ?: return
      val sessionId = obj.get("sessionId")?.asInt ?: -1
      val gameName = obj.get("game")?.asString
      val game = gameName?.let { Game.fromName(it) } ?: Game.WARLORDS

      _state.value =
        _state.value.copy(
          mySessionId = sessionId,
          game = game,
          connectionState = ConnectionState.CONNECTED,
        )
    } catch (e: Exception) {
      // Ignore handshake parse errors
    }
  }

  private fun parseMessage(json: String): Message? {
    return try {
      val obj = gson.fromJson(json, JsonObject::class.java) ?: return null
      val id = obj.get("id")?.asString ?: return null
      val data = obj.get("data")?.asString ?: ""
      Message(id, data)
    } catch (e: Exception) {
      null
    }
  }

  private fun parseCardList(
    obj: JsonObject,
    key: String,
  ): List<Card> {
    val arr = obj.get(key)
    Log.d("GameVM", "parseCardList: key='$key', arr=$arr")
    if (arr == null) {
      Log.d("GameVM", "parseCardList: key='$key' not found, returning empty")
      return emptyList()
    }
    if (!arr.isJsonArray) {
      Log.d("GameVM", "parseCardList: key='$key' is not a JsonArray, returning empty")
      return emptyList()
    }
    val result =
      arr.asJsonArray.mapNotNull { elem ->
        try {
          val name =
            if (elem.isJsonPrimitive) {
              elem.asString
            } else {
              elem.asJsonObject.get("name")?.asString
            }
          if (name != null) Card.fromName(name) else null
        } catch (e: Exception) {
          Log.d("GameVM", "parseCardList: failed to parse elem=$elem, error=${e.message}")
          null
        }
      }
    Log.d("GameVM", "parseCardList: key='$key' -> ${result.size} cards = $result")
    return result
  }

  override fun onCleared() {
    connection.disconnect()
  }
}
