package com.kibitzer.mobile.model

import androidx.lifecycle.ViewModel
import com.kibitzer.mobile.game.Card
import com.kibitzer.mobile.game.Game
import com.kibitzer.mobile.game.Hand
import com.kibitzer.mobile.game.Message
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel(
    private val connection: GameConnection
) : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val gson = Gson()

    init {
        connection.setOnStateUpdate { json -> handleStateUpdate(json) }
        connection.setOnChatUpdate { json -> handleChatUpdate(json) }
        connection.setOnHandshake { json -> handleHandshake(json) }
        connection.setOnConnectionState { connected ->
            _state.value = _state.value.copy(
                connectionState = if (connected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
            )
        }
        connection.setOnScanResult { address ->
            _state.value = _state.value.copy(connectionState = ConnectionState.CONNECTING)
        }
    }

    fun connect() {
        _state.value = _state.value.copy(connectionState = ConnectionState.SCANNING)
        connection.startScan()
    }

    fun disconnect() {
        connection.disconnect()
        _state.value = _state.value.copy(connectionState = ConnectionState.DISCONNECTED)
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
        connection.sendCommand("shuf:")
    }

    fun deal() {
        connection.sendCommand("deal:")
    }

    fun pickup(count: Int = 1) {
        connection.sendCommand("picu:$count")
    }

    fun putdown(cards: List<Card>) {
        val hand = Hand()
        cards.forEach { hand.add(it) }
        connection.sendCommand("putd:${hand.toJson()}")
    }

    fun skip() {
        connection.sendCommand("skip:")
    }

    fun chat(text: String) {
        connection.sendCommand("chat:$text")
    }

    fun bid(count: Int) {
        connection.sendCommand("bid:$count")
    }

    fun cheat(targetId: Int) {
        connection.sendCommand("cheat:$targetId")
    }

    fun ask(data: String) {
        connection.sendCommand("ask:$data")
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

            _state.value = _state.value.copy(
                myHand = newHand,
                discardPile = newPile,
                currentTurn = turn,
                message = message,
                faceDown = faceDown,
                game = game,
                mySessionId = sessionId,
                isGameOver = newHand.isEmpty() && turn == -1
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
            _state.value = _state.value.copy(
                chatLog = _state.value.chatLog + text
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

            _state.value = _state.value.copy(
                mySessionId = sessionId,
                game = game,
                connectionState = ConnectionState.CONNECTED
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

    private fun parseCardList(obj: JsonObject, key: String): List<Card> {
        val arr = obj.get(key) ?: return emptyList()
        if (!arr.isJsonArray) return emptyList()
        return arr.asJsonArray.mapNotNull { elem ->
            try {
                val cardObj = elem.asJsonObject
                val name = cardObj.get("name")?.asString
                if (name != null) Card.valueOf(name) else null
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun onCleared() {
        connection.disconnect()
    }
}