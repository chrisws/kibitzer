package com.kibitzer.mobile.model

import androidx.lifecycle.ViewModel
import com.kibitzer.mobile.game.AiPlayer
import com.kibitzer.mobile.game.Card
import com.kibitzer.mobile.game.ChatBot
import com.kibitzer.mobile.game.Controller
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
            _state.value = _state.value.copy(
                connectionState = if (connected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
            )
        }
        connection.setOnScanResult { address ->
            val current = _state.value
            if (address !in current.discoveredGames.map { it.address }) {
                _state.value = current.copy(
                    discoveredGames = current.discoveredGames + DiscoveredGame(address)
                )
            }
        }
    }

    fun connect() {
        _state.value = _state.value.copy(
            connectionState = ConnectionState.SCANNING,
            discoveredGames = emptyList()
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
        _state.value = _state.value.copy(
            connectionState = ConnectionState.CONNECTED,
            discoveredGames = emptyList()
        )
    }

    fun startAiGame(
        game: Game,
        numAiPlayers: Int = 1,
        difficulty: AiPlayer.Difficulty = AiPlayer.Difficulty.MEDIUM
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
        aiSessionIds.forEachIndexed { i, sid ->
            ctrl.handle("join:${i + 1}", sid)
        }

        // Shuffle and deal
        ctrl.handle("shuf:", humanSessionId)
        val dealMsg = ctrl.handle("deal:", humanSessionId)

        // Build player list with hand sizes
        val playerList = mutableListOf(
            PlayerInfo(humanSessionId, "You", true, false, 0, 0)
        )
        aiSessionIds.forEachIndexed { i, sid ->
            val ctrlPlayer = ctrl.findSession(sid)
            playerList.add(
                PlayerInfo(sid, aiPlayers[i].name, true, true, ctrlPlayer?.hand?.size ?: 0, 0)
            )
        }

        // Update state
        _state.value = _state.value.copy(
            gameMode = GameMode.AI,
            game = game,
            mySessionId = humanSessionId,
            connectionState = ConnectionState.CONNECTED,
            screen = Screen.GAME,
            players = playerList,
            chatLog = listOf("${aiPlayers[0].name}: ${bots[0].greet()}")
        )

        // Sync state from controller
        syncStateFromController(dealMsg)

        // If it's an AI's turn, let it play
        if (aiSessionIds.contains(_state.value.currentTurn) && !_state.value.isGameOver) {
            aiTurn()
        }
    }

    fun stopAiGame() {
        controller = null
        aiPlayerList = emptyList()
        chatBotList = emptyList()
        aiSessionIds = emptyList()
        humanSessionId = -1
        _state.value = _state.value.copy(
            gameMode = GameMode.BLE,
            screen = Screen.HOME,
            myHand = emptyList(),
            discardPile = emptyList(),
            currentTurn = -1,
            message = "",
            isGameOver = false,
            chatLog = emptyList(),
            players = emptyList()
        )
    }

    fun disconnect() {
        connection.disconnect()
        _state.value = _state.value.copy(
            connectionState = ConnectionState.DISCONNECTED,
            discoveredGames = emptyList()
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
                aiTurn()
            }
        } else {
            connection.sendCommand("deal:")
        }
    }

    fun pickup(count: Int = 1) {
        if (_state.value.gameMode == GameMode.AI) {
            val ctrl = controller ?: return
            val msg = ctrl.handle("picu:$count", humanSessionId)
            syncStateFromController(msg)
            if (aiSessionIds.contains(_state.value.currentTurn) && !_state.value.isGameOver) {
                aiTurn()
            }
        } else {
            connection.sendCommand("picu:$count")
        }
    }

    fun putdown(cards: List<Card>) {
        val hand = Hand()
        cards.forEach { hand.add(it) }
        if (_state.value.gameMode == GameMode.AI) {
            val ctrl = controller ?: return
            val msg = ctrl.handle("putd:${hand.toJson()}", humanSessionId)
            syncStateFromController(msg)
            if (aiSessionIds.contains(_state.value.currentTurn) && !_state.value.isGameOver) {
                aiTurn()
            }
        } else {
            connection.sendCommand("putd:${hand.toJson()}")
        }
    }

    fun skip() {
        if (_state.value.gameMode == GameMode.AI) {
            val ctrl = controller ?: return
            val msg = ctrl.handle("skip:", humanSessionId)
            syncStateFromController(msg)
            if (aiSessionIds.contains(_state.value.currentTurn) && !_state.value.isGameOver) {
                aiTurn()
            }
        } else {
            connection.sendCommand("skip:")
        }
    }

    fun chat(text: String) {
        if (_state.value.gameMode == GameMode.AI) {
            _state.value = _state.value.copy(chatLog = _state.value.chatLog + "You: $text")
            aiSessionIds.forEachIndexed { i, sid ->
                val bot = chatBotList[i]
                val aiName = aiPlayerList[i].name
                val response = bot.respond(text)
                _state.value = _state.value.copy(
                    chatLog = _state.value.chatLog + "$aiName: $response"
                )
            }
        } else {
            connection.sendCommand("chat:$text")
        }
    }

    fun bid(count: Int) {
        if (_state.value.gameMode == GameMode.AI) {
            val ctrl = controller ?: return
            val msg = ctrl.handle("bid:$count", humanSessionId)
            syncStateFromController(msg)
            if (aiSessionIds.contains(_state.value.currentTurn) && !_state.value.isGameOver) {
                aiTurn()
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
                aiTurn()
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
                aiTurn()
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

    private fun aiTurn() {
        val ctrl = controller ?: return
        val currentTurn = _state.value.currentTurn
        val aiIndex = aiSessionIds.indexOf(currentTurn)
        if (aiIndex == -1) return

        val ai = aiPlayerList[aiIndex]
        val command = ai.decide(ctrl, currentTurn)
        val msg = ctrl.handle(command, currentTurn)
        syncStateFromController(msg)

        // If another AI needs to play, recurse
        if (aiSessionIds.contains(_state.value.currentTurn) && !_state.value.isGameOver) {
            aiTurn()
        }
    }

    private fun syncStateFromController(msg: Message) {
        if (msg.id != "cards") return
        val data = msg.data as? String ?: return
        val obj = gson.fromJson(data, JsonObject::class.java) ?: return

        val newHand = parseCardList(obj, "hand")
        val newPile = parseCardList(obj, "pile")
        val turn = obj.get("turn")?.asInt ?: -1
        val rawMessage = obj.get("message")?.asString ?: ""
        val message = if (rawMessage == "cards dealt") "" else rawMessage
        val faceDown = obj.get("faceDown")?.asBoolean ?: false
        val gameName = obj.get("game")?.asString
        val game = gameName?.let { Game.fromName(it) } ?: _state.value.game

        // Update player hand sizes from controller
        val ctrl = controller
        val updatedPlayers = _state.value.players.map { p ->
            if (ctrl != null) {
                val ctrlPlayer = ctrl.findSession(p.sessionId)
                if (ctrlPlayer != null) {
                    p.copy(handSize = ctrlPlayer.hand.size)
                } else p
            } else p
        }

        _state.value = _state.value.copy(
            myHand = newHand,
            discardPile = newPile,
            currentTurn = turn,
            message = message,
            faceDown = faceDown,
            game = game,
            players = updatedPlayers,
            isGameOver = newHand.isEmpty() && turn == -1
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
                val name = if (elem.isJsonPrimitive) {
                    elem.asString
                } else {
                    elem.asJsonObject.get("name")?.asString
                }
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