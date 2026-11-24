# Game Logic — Handlers & Controller Integration

> Part of the Kibitzer Mobile game-logic documentation.
> See `game-logic.md` for core types, `game-logic-rules.md` for rule classes,
> and `game-logic-ai.md` for the AI opponent.

## Game-Specific Controller Extensions

Old Maid and Go Fish use a fundamentally different interaction model from
deck/discard games. Players act on their *hand* (finding pairs, asking
opponents for ranks) rather than playing cards to a shared discard pile.
The `Rules` overrides for these games are minimal stubs — the actual turn
logic lives in game-specific handler classes.

### `GameHandler` Interface

```kotlin
// game/GameHandler.kt
package com.kibitzer.mobile.game

/**
 * Encapsulates the full turn logic for a game mode.
 * Deck/discard games (Warlords, Free, Crazy Eights, Cheat) use the
 * default Controller flow. Hand-based games (Old Maid, Go Fish)
 * provide custom handlers.
 */
interface GameHandler {
    fun onTurnStart(room: Room, player: Player, players: List<Player>): Message
    fun onPlay(room: Room, player: Player, players: List<Player>, payload: String): Message
    fun onPickup(room: Room, player: Player, players: List<Player>, payload: String): Message
    fun onSkip(room: Room, player: Player, players: List<Player>): Message
    fun onAsk(room: Room, player: Player, players: List<Player>, payload: String): Message
    fun isGameOver(room: Room, players: List<Player>): Boolean
    fun getWinner(room: Room, players: List<Player>): Player?

    /**
     * Returns true if the turn should advance to the next player after
     * the handler's action. Returns false for "go-again" scenarios
     * (e.g., Go Fish hit, Crazy Eights 8-play).
     * Default: true (advance turn).
     */
    fun shouldAdvanceTurn(room: Room, player: Player, players: List<Player>): Boolean = true
}
```

### `OldMaidHandler`

```kotlin
// game/OldMaidHandler.kt
package com.kibitzer.mobile.game

class OldMaidHandler : GameHandler {

    // The Old Maid card (the unmatched one)
    companion object {
        val OLD_MAID = Card.TWO_SPADES  // the unmatched card (2C, 2D, 2H removed from deck)
    }

    override fun onTurnStart(room: Room, player: Player, players: List<Player>): Message {
        // Player looks through their hand for pairs
        // Pairs are automatically discarded (no command needed)
        val pairs = findPairs(player.hand)
        pairs.forEach { (a, b) ->
            player.hand.remove(Hand().apply { add(a); add(b) })
        }
        if (player.hand.size == 0) {
            // Player eliminated (no Old Maid)
            return Message("cards", buildEliminatedJson(player))
        }
        // If only Old Maid left, player loses
        if (player.hand.size == 1 && player.hand.has(OLD_MAID)) {
            return Message("cards", buildLoserJson(player))
        }
        // Otherwise, player draws 1 card from a random opponent
        val opponent = randomOpponent(room, player, players)
            ?: return Message("cards", buildTurnJson(room, player))
        val drawn = opponent.hand.pop()
        player.hand.add(drawn)
        // Check for new pairs
        val newPairs = findPairs(player.hand)
        newPairs.forEach { (a, b) ->
            player.hand.remove(Hand().apply { add(a); add(b) })
        }
        return Message("cards", buildTurnJson(room, player))
    }

    override fun onPlay(room: Room, player: Player, players: List<Player>, payload: String): Message {
        // "Play" in Old Maid = draw from opponent (handled in onTurnStart)
        return Message("message", "use your turn to draw")
    }

    override fun onPickup(room: Room, player: Player, players: List<Player>, payload: String): Message =
        onTurnStart(room, player, players)

    override fun onSkip(room: Room, player: Player, players: List<Player>): Message =
        onTurnStart(room, player, players)

    override fun onAsk(room: Room, player: Player, players: List<Player>, payload: String): Message =
        Message("message", "ask is not used in Old Maid")

    override fun isGameOver(room: Room, players: List<Player>): Boolean {
        // Game over when only one player remains with cards
        val active = room.slots.count { it != -1 }
        return active <= 1
    }

    override fun getWinner(room: Room, players: List<Player>): Player? {
        // Last player without the Old Maid wins
        val activePlayers = room.slots
            .mapNotNull { sid -> sid.takeIf { it != -1 } }
            .mapNotNull { sid -> players.find { it.sessionId == sid } }
        return activePlayers.firstOrNull { !it.hand.has(OLD_MAID) }
    }

    private fun findPairs(hand: Hand): List<Pair<Card, Card>> {
        val pairs = mutableListOf<Pair<Card, Card>>()
        val seen = mutableMapOf<Int, Card>()
        for (card in hand.toList()) {
            val rank = rank2A(card)
            val prev = seen[rank]
            if (prev != null) {
                pairs.add(prev to card)
                seen.remove(rank)
            } else {
                seen[rank] = card
            }
        }
        return pairs
    }
}
```

### `GoFishHandler`

```kotlin
// game/GoFishHandler.kt
package com.kibitzer.mobile.game

class GoFishHandler : GameHandler {

    override fun onTurnStart(room: Room, player: Player, players: List<Player>): Message {
        // Player must ask an opponent for a rank they hold
        return Message("message", "ask an opponent for a card rank")
    }

    override fun onPlay(room: Room, player: Player, players: List<Player>, payload: String): Message {
        // Go Fish uses the dedicated `ask:` command, not `putd:`
        return Message("message", "use the ask command: ask:<slot>:<rank>")
    }

    override fun onAsk(room: Room, player: Player, players: List<Player>, payload: String): Message {
        // payload format: "<targetSlot>:<rankChar>" e.g. "1:Q"
        val parts = payload.split(":")
        if (parts.size != 2) return Message("message", "invalid ask format")
        val targetSlot = parts[0].toIntOrNull() ?: return Message("message", "invalid slot")
        val rankChar = parts[1].firstOrNull() ?: return Message("message", "invalid rank")

        // Validate: player must hold at least one card of this rank
        val playerRank = rankCharToRank(rankChar)
        if (playerRank < 0) return Message("message", "invalid rank")
        if (player.hand.toList().none { rank2A(it) == playerRank }) {
            return Message("message", "you don't hold that rank")
        }

        val target = players.find { it.slot == targetSlot }
            ?: return Message("message", "invalid target")

        // Check if target has any cards of that rank
        val targetCards = target.hand.toList().filter { rank2A(it) == playerRank }
        if (targetCards.isEmpty()) {
            // "Go fish!" — draw from deck
            val drawn = room.deck.pickup(player.hand, 1)
            discardCompletedSets(player)
            lastResult = "go_fish"
            return Message("cards", buildGoFishJson(player, drawn))
        } else {
            // "Hit" — give all cards of that rank
            targetCards.forEach { target.hand.remove(Hand().apply { add(it) }) }
            targetCards.forEach { player.hand.add(it) }
            discardCompletedSets(player)
            lastResult = "hit"
            return Message("cards", buildHitJson(player, target, targetCards))
        }
    }

    override fun onPickup(room: Room, player: Player, players: List<Player>, payload: String): Message {
        // Draw from deck (used when deck is the only option)
        val count = payload.toIntOrNull() ?: 1
        val drawn = room.deck.pickup(player.hand, count)
        discardCompletedSets(player)
        return Message("cards", buildPickupJson(player, drawn))
    }

    override fun onSkip(room: Room, player: Player, players: List<Player>): Message =
        Message("message", "you must ask or draw")

    override fun isGameOver(room: Room, players: List<Player>): Boolean {
        // Game over when a player empties their hand
        return players.any { it.hand.size == 0 }
    }

    override fun getWinner(room: Room, players: List<Player>): Player? {
        return players.firstOrNull { it.hand.size == 0 }
    }

    /**
     * Go Fish: the turn always advances after a player's ask.
     * (Go-again on hit is a common variation but not the base rule.)
     * We track the last result in a transient field for UI feedback.
     */
    private var lastResult: String = ""  // "hit" or "go_fish"

    override fun shouldAdvanceTurn(room: Room, player: Player, players: List<Player>): Boolean {
        // Base rule: turn always advances
        return true
    }

    private fun discardCompletedSets(player: Player) {
        val byRank = mutableMapOf<Int, MutableList<Card>>()
        for (card in player.hand.toList()) {
            byRank.getOrPut(rank2A(card)) { mutableListOf() }.add(card)
        }
        for ((rank, cards) in byRank) {
            if (cards.size == 4) {
                player.hand.remove(Hand().apply { cards.forEach { add(it) } })
            }
        }
    }
}
```

### Controller Integration

The `Controller.handle()` delegates to the active `GameHandler` when the
current game requires one (set via `setGame()`):

```kotlin
// In Controller.handle():
fun handle(command: String, sessionId: Int): Message {
    val player = findSession(sessionId) ?: return Message("message", "unknown session")
    val prefix = command.take(5)
    val payload = command.drop(5)
    val room = rooms[player.room]

    // Delegate to game handler if active
    val players = room.players
    gameHandler?.let { handler ->
        when (Command.fromPrefix(prefix)) {
            Command.PUTDOWN  -> {
                val msg = handler.onPlay(room, player, players, payload)
                if (handler.shouldAdvanceTurn(room, player, players)) {
                    setNextTurn(player.room, StringBuilder())
                    val next = findPlayerBySlot(room, room.turn, players)
                    if (next != null) handler.onTurnStart(room, next, players)
                }
                return msg
            }
            Command.PICKUP   -> {
                val msg = handler.onPickup(room, player, players, payload)
                if (handler.shouldAdvanceTurn(room, player, players)) {
                    setNextTurn(player.room, StringBuilder())
                    val next = findPlayerBySlot(room, room.turn, players)
                    if (next != null) handler.onTurnStart(room, next, players)
                }
                return msg
            }
            Command.SKIP     -> {
                val msg = handler.onSkip(room, player, players)
                setNextTurn(player.room, StringBuilder())
                val next = findPlayerBySlot(room, room.turn, players)
                if (next != null) handler.onTurnStart(room, next, players)
                return msg
            }
            Command.ASK      -> {
                val msg = handler.onAsk(room, player, players, payload)
                if (handler.shouldAdvanceTurn(room, player, players)) {
                    setNextTurn(player.room, StringBuilder())
                    val next = findPlayerBySlot(room, room.turn, players)
                    if (next != null) handler.onTurnStart(room, next, players)
                }
                return msg
            }
            else             -> { /* fall through to default handling */ }
        }
    }

    // Default deck/discard flow (Warlords, Free, Crazy Eights, Cheat)
    return when (Command.fromPrefix(prefix)) {
        // ... existing handlers ...
    }
}
```

This keeps the `Controller` thin — game-specific logic lives in the handler
classes, and the `Rules` class continues to provide validation for all games.

## Helper Functions

Utility functions used by the handlers and AI. Defined in `game/Helpers.kt`:

```kotlin
// game/Helpers.kt
package com.kibitzer.mobile.game

/** Convert a rank character to its numeric value (matching rank2A). */
fun rankCharToRank(ch: Char): Int = when (ch) {
    '2' -> 0; '3' -> 1; '4' -> 2; '5' -> 3; '6' -> 4
    '7' -> 5; '8' -> 6; '9' -> 7; 'X' -> 8; 'J' -> 9
    'Q' -> 10; 'K' -> 11; 'A' -> 12
    else -> -1
}

/** Convert a numeric rank (0-12) back to its character representation. */
fun rankToChar(rank: Int): Char = when (rank) {
    0 -> '2'; 1 -> '3'; 2 -> '4'; 3 -> '5'; 4 -> '6'
    5 -> '7'; 6 -> '8'; 7 -> '9'; 8 -> 'X'; 9 -> 'J'
    10 -> 'Q'; 11 -> 'K'; 12 -> 'A'
    else -> '?'
}

/** Find a Player by their slot index in a Room. */
fun findPlayerBySlot(room: Room, slot: Int, players: List<Player>): Player? {
    if (slot !in room.slots.indices) return null
    val sid = room.slots[slot]
    if (sid == -1) return null
    return players.find { it.sessionId == sid }
}

/** Find a Player by their sessionId within a Room's slots. */
fun findPlayerById(room: Room, sessionId: Int, players: List<Player>): Player? {
    if (sessionId !in room.slots) return null
    return players.find { it.sessionId == sessionId }
}

/** Pick a random opponent (not the given player) from a Room. */
fun randomOpponent(room: Room, player: Player, players: List<Player>): Player? {
    val opponents = room.slots
        .mapIndexed { i, sid -> i to sid }
        .filter { (_, sid) -> sid != -1 && sid != player.sessionId }
        .map { (_, sid) -> players.find { it.sessionId == sid } }
        .filterNotNull()
    return opponents.randomOrNull()
}

// --- JSON builders (used by handlers) ---

fun buildTurnJson(room: Room, player: Player): String {
    val hand = player.hand.toJson()
    val turn = room.turn
    return """{"hand":$hand,"turn":$turn}"""
}

fun buildEliminatedJson(player: Player): String {
    return """{"sessionId":${player.sessionId},"eliminated":true}"""
}

fun buildLoserJson(player: Player): String {
    return """{"sessionId":${player.sessionId},"loser":true,"card":"${OldMaidHandler.OLD_MAID.name}"}"""
}

fun buildGoFishJson(player: Player, drawn: Hand): String {
    return """{"sessionId":${player.sessionId},"result":"go_fish","drawn":${drawn.toJson()}}"""
}

fun buildHitJson(player: Player, target: Player, cards: List<Card>): String {
    val cardJson = cards.joinToString(", ", prefix = "[", postfix = "]") { "\"${it.name}\"" }
    return """{"sessionId":${player.sessionId},"result":"hit","from":${target.sessionId},"cards":$cardJson}"""
}

fun buildPickupJson(player: Player, drawn: Hand): String {
    return """{"sessionId":${player.sessionId},"drawn":${drawn.toJson()}}"""
}

fun buildExitJson(sessionId: Int, player: Player): String {
    return """{"sessionId":$sessionId,"nic":"${player.nickname}"}"""
}
```