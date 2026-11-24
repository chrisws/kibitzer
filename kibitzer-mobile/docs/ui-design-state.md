# UI Design — State Management

> Part of the Kibitzer Mobile UI documentation.
> See `ui-design.md` for screens and components,
> and `ui-design-help.md` for help/rules screens.

## State Management

### PlayerInfo

```kotlin
data class PlayerInfo(
    val sessionId: Int,
    val nic: String,
    val active: Boolean,
    val isAi: Boolean = false
)
```

### GameState

```kotlin
data class GameState(
    val screen: Screen = Screen.HOME,
    val connection: ConnectionState = ConnectionState.DISCONNECTED,
    val sessionId: Int = -1,
    val players: List<PlayerInfo> = emptyList(),
    val myHand: List<Card> = emptyList(),
    val selectedCards: Set<Card> = emptySet(),
    val pile: List<Card> = emptyList(),
    val deckCount: Int = 52,
    val currentTurn: Int = -1,
    val turnSeconds: Int = 30,
    val gameType: Game = Game.WARLORDS,
    val gameName: String = "",
    val chatMessages: List<String> = emptyList(),  // capped at 50
    val faceDown: Boolean = false,
    val isMyTurn: Boolean = false,
    val gameOver: Boolean = false,
    val scores: Map<String, Int> = emptyMap(),
    val winner: String? = null,

    // --- Game-specific state ---

    // Cheat: current bidding/calling state
    val cheatPhase: CheatPhase = CheatPhase.IDLE,
    val cheatBid: Int = 0,
    val cheatBidder: String = "",
    val cheatCalled: Boolean = false,

    // Go Fish: pending ask state
    val goFishAskTarget: String = "",  // nickname of player being asked
    val goFishAskRank: String = "",    // rank being asked for
    val goFishResult: String = "",     // "hit", "go_fish", or ""

    // Crazy Eights: active suit (set after 8 is played)
    val activeSuit: String = ""  // "C", "D", "H", "S", or ""
)

enum class CheatPhase { IDLE, BIDDING, PLAYING, RESOLVING }
```

### GameViewModel

```kotlin
class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var connection: GameConnection? = null
    private var controller: Controller? = null  // host only
    private var turnTimer: Job? = null

    // --- Actions ---

    fun createGame() { /* create BleConnection(host), collect flows, update state */ }
    fun joinGame() { /* create BleConnection(client), collect flows, update state */ }
    fun setNickname(name: String) { connection?.send("nicn:$name") }
    fun startGame() {
        // Host shuffles then deals ALL players (Controller handles deal-all)
        connection?.send("shuf:")
        connection?.send("deal:")
    }
    fun selectCard(card: Card) { /* toggle in selectedCards */ }
    fun playCards() { connection?.send("putd:${selectedCards.toJson()}") }
    fun pickupCards(count: Int = 1) { connection?.send("picu:$count") }
    fun skipTurn() { connection?.send("skip:") }
    fun sendChat(text: String) { connection?.send("chat:$text") }
    fun playAgain() { connection?.send("shuf:") }
    fun leaveGame() { connection?.disconnect(); connection = null; _state.value = GameState() }

    // --- Connection flows (collected in createGame/joinGame) ---

    private fun collectConnectionFlows(conn: GameConnection) {
        viewModelScope.launch {
            conn.onMessage.collect { message ->
                onMessageReceived(message)
            }
        }
        viewModelScope.launch {
            conn.onConnectionChange.collect { state ->
                _state.update { it.copy(connection = state) }
            }
        }
    }

    fun onMessageReceived(message: Message) {
        // Parse message, update _state
        // Handle turn timer
        // Check for game over
    }

    // --- Turn timer ---

    private fun startTurnTimer() {
        turnTimer?.cancel()
        turnTimer = viewModelScope.launch {
            var seconds = 30
            while (seconds > 0 && _state.value.isMyTurn) {
                delay(1000)
                seconds--
                _state.update { it.copy(turnSeconds = seconds) }
            }
            if (seconds == 0 && _state.value.isMyTurn) {
                skipTurn()
            }
        }
    }
}
```

### Connection State

```kotlin
enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}
```