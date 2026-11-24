# Game Logic — Kotlin Port

Direct port of the C++ `cards`, `rules`, and `controller` modules to Kotlin.
The C++ source in `../server/` is the authoritative reference for **Warlords**
and **Free** rules and state transitions. The remaining four games
(Crazy Eights, Cheat, Old Maid, Go Fish) are new implementations with no
C++ counterpart — their rules are defined here in Kotlin only.
No Android or BLE dependencies. Pure domain logic.

> **Documentation split:** This file contains the core types. See:
> - [`game-logic-rules.md`](game-logic-rules.md) — all 6 rule classes + CheatState
> - [`game-logic-handlers.md`](game-logic-handlers.md) — GameHandler, OldMaidHandler, GoFishHandler, Controller Integration, Helpers
> - [`game-logic-ai.md`](game-logic-ai.md) — AiPlayer, strategies, AI tests, C++ differences

## Card

```kotlin
// game/Card.kt
package com.kibitzer.mobile.game

enum class Card(val name: String) {
    TWO_CLUBS("2C"),  TWO_DIAMONDS("2D"),  TWO_HEARTS("2H"),  TWO_SPADES("2S"),
    THREE_CLUBS("3C"), THREE_DIAMONDS("3D"), THREE_HEARTS("3H"), THREE_SPADES("3S"),
    FOUR_CLUBS("4C"),  FOUR_DIAMONDS("4D"),  FOUR_HEARTS("4H"),  FOUR_SPADES("4S"),
    FIVE_CLUBS("5C"),  FIVE_DIAMONDS("5D"),  FIVE_HEARTS("5H"),  FIVE_SPADES("5S"),
    SIX_CLUBS("6C"),   SIX_DIAMONDS("6D"),   SIX_HEARTS("6H"),   SIX_SPADES("6S"),
    SEVEN_CLUBS("7C"), SEVEN_DIAMONDS("7D"), SEVEN_HEARTS("7H"), SEVEN_SPADES("7S"),
    EIGHT_CLUBS("8C"), EIGHT_DIAMONDS("8D"), EIGHT_HEARTS("8H"), EIGHT_SPADES("8S"),
    NINE_CLUBS("9C"),  NINE_DIAMONDS("9D"),  NINE_HEARTS("9H"),  NINE_SPADES("9S"),
    TEN_CLUBS("XC"),   TEN_DIAMONDS("XD"),   TEN_HEARTS("XH"),   TEN_SPADES("XS"),
    JACK_CLUBS("JC"),  JACK_DIAMONDS("JD"),  JACK_HEARTS("JH"),  JACK_SPADES("JS"),
    QUEEN_CLUBS("QC"), QUEEN_DIAMONDS("QD"), QUEEN_HEARTS("QH"), QUEEN_SPADES("QS"),
    KING_CLUBS("KC"),  KING_DIAMONDS("KD"),  KING_HEARTS("KH"),  KING_SPADES("KS"),
    ACE_CLUBS("AC"),   ACE_DIAMONDS("AD"),   ACE_HEARTS("AH"),   ACE_SPADES("AS");

    companion object {
        fun fromName(name: String): Card? =
            entries.find { it.name == name }
    }
}

typealias CardRank = (Card) -> Int
```

## Game

```kotlin
// game/Game.kt
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
```

## Hand

```kotlin
// game/Hand.kt
package com.kibitzer.mobile.game

class Hand {
    private val _cards = mutableListOf<Card>()

    constructor()
    constructor(other: Hand) { _cards.addAll(other._cards) }

    val size: Int get() = _cards.size

    fun add(card: Card) { _cards.add(card) }

    fun addAll(hand: Hand) { _cards.addAll(hand._cards) }

    fun clear() { _cards.clear() }

    fun peek(): Card = _cards.last()

    fun pop(): Card = _cards.removeAt(_cards.size - 1)

    fun has(card: Card): Boolean = card in _cards

    fun has(hand: Hand): Boolean = hand._cards.all { it in _cards }

    fun hasEqual(card: Card, rank: CardRank): Boolean {
        val value = rank(card)
        return _cards.any { rank(it) == value }
    }

    fun hasHigher(card: Card, minCount: Int, rank: CardRank): Boolean {
        val value = rank(card)
        val counts = mutableMapOf<Int, Int>()
        for (c in _cards) {
            val r = rank(c)
            if (r > value) {
                counts[r] = (counts[r] ?: 0) + 1
            }
        }
        return counts.values.any { it >= minCount }
    }

    fun isEqual(card: Card, rank: CardRank): Boolean {
        val value = rank(card)
        return _cards.all { rank(it) == value }
    }

    fun remove(hand: Hand) {
        _cards.removeAll(hand._cards.toSet())
    }

    fun remove(card: Card) {
        _cards.remove(card)
    }

    fun sort(rank: CardRank) {
        _cards.sortWith(compareBy({ rank(it) }, { it.ordinal }))
    }

    fun swap(i: Int, j: Int) { _cards.swap(i, j) }

    fun get(index: Int): Card = _cards[index]

    fun toList(): List<Card> = _cards.toList()

    fun toJson(): String {
        return _cards.joinToString(", ", prefix = "[", postfix = "]") { "\"${it.name}\"" }
    }

    fun toString(): String =
        if (_cards.isEmpty()) "empty!" else _cards.joinToString(" ") { it.name }
}
```

## Deck

```kotlin
// game/Deck.kt
package com.kibitzer.mobile.game

class Deck {
    val pack = Hand()
    val discard = Hand()
    var lastPlay: Card? = null
        private set
    var lastSize: Int = 0
        private set

    val packSize: Int get() = pack.size
    val discardSize: Int get() = discard.size

    fun shuffle() {
        discard.clear()
        lastPlay = null
        lastSize = 0
        pack.clear()
        for (card in Card.entries) {
            pack.add(card)
        }
        // Fisher-Yates shuffle
        for (i in pack.size - 1 downTo 1) {
            val j = Random.nextInt(i + 1)
            pack.swap(i, j)
        }
    }

    fun deal(hand: Hand, count: Int) {
        hand.clear()
        repeat(count) {
            if (pack.size > 0) {
                hand.add(pack.pop())
            }
        }
    }

    fun pickup(hand: Hand, count: Int): Hand {
        val result = Hand()
        repeat(count) {
            if (pack.size > 0) {
                val card = pack.pop()
                result.add(card)
                hand.add(card)
            }
        }
        return result
    }

    fun putdown(hand: Hand) {
        discard.addAll(hand)
        lastPlay = discard.peek()
        lastSize = hand.size
    }

    fun clearDiscard() {
        discard.clear()
        lastPlay = null
        lastSize = 0
    }

    fun takeDiscard(hand: Hand) {
        hand.addAll(discard)
        discard.clear()
        lastPlay = null
        lastSize = 0
    }

    fun getPackJson(): String = pack.toJson()
    fun getDiscardJson(): String = discard.toJson()

    fun copy(): Deck {
        val d = Deck()
        d.pack.addAll(pack)
        d.discard.addAll(discard)
        d.lastPlay = lastPlay
        d.lastSize = lastSize
        return d
    }
}
```

## Rules (Abstract)

```kotlin
// game/Rules.kt
package com.kibitzer.mobile.game

abstract class Rules {
    abstract fun canPlay(deck: Deck, hand: Hand): Boolean
    abstract fun canRevoke(): Boolean
    abstract fun clearDiscard(hand: Hand): Boolean
    abstract fun faceDown(): Boolean
    abstract fun getRank(): CardRank
    abstract fun handSize(players: Int): Int
    abstract fun isValidPlay(deck: Deck, hand: Hand): Boolean
    abstract fun isWinningPlay(deck: Deck, hand: Hand): Boolean
    abstract fun name(): String
    abstract fun url(): String
    abstract fun noPlayTakesDiscard(): Boolean
    abstract fun setNextTurn(hand: Hand): Boolean
}
```

> Concrete rule implementations (WarlordsRules, FreeRules, CrazyEightsRules,
> CheatRules, OldMaidRules, GoFishRules) are in
> [`game-logic-rules.md`](game-logic-rules.md).

## Multi-Round Scoring

| Game | 1st | 2nd | 3rd | 4th | 5th | 6th |
|---|---|---|---|---|---|---|
| Warlords | 6 | 5 | 4 | 3 | 2 | 1 |
| Free | 1 (per card remaining, opponent scores) | | | | | |
| Crazy Eights | 6 | 5 | 4 | 3 | 2 | 1 |
| Cheat | 6 | 5 | 4 | 3 | 2 | 1 |
| Old Maid | 1 (winner only; loser gets 0) | | | | | |
| Go Fish | 6 | 5 | 4 | 3 | 2 | 1 |

**Free** scoring is inverted: each player scores 1 point per card remaining
in *other* players' hands when they go out first. This rewards efficient play.

**Old Maid** is elimination-based: the last player without the Old Maid gets
1 point; all others get 0.

The `Controller` awards points in `awardPoints(room: Room)` called when
`isWinningPlay()` returns true. The `GameOverScreen` displays cumulative
scores across all rounds.

## Player

```kotlin
// game/Player.kt
package com.kibitzer.mobile.game

enum class PlayerState { LURK, JOINED, DEALT }

class Player(val sessionId: Int) {
    val hand = Hand()
    var nickname: String = "#$sessionId"
        set(value) { field = value.replace("\"", "") }
    var slot: Int = -1
    var points: Int = 0
    var room: Int = 0
    var state: PlayerState = PlayerState.LURK
    var isAi: Boolean = false  // true if this slot is filled by AiPlayer

    fun name(): String = "($nickname)"

    fun toJson(): String {
        val active = state != PlayerState.LURK && slot != -1
        return """{"sessionId":$sessionId,"active":$active,"nic":"$nickname","ai":$isAi}"""
    }
}
```

## Room

```kotlin
// game/Room.kt
package com.kibitzer.mobile.game

class Room(val maxSlots: Int = 6) {
    val deck = Deck()
    val slots = MutableList(maxSlots) { -1 }  // sessionId per slot
    var rules: Rules = WarlordsRules()
    var turn: Int = -1  // sessionId of current turn
    var cheatState: CheatState = CheatState()  // Cheat game bidding state

    // Crazy Eights: the current suit (set when an 8 is played).
    // null = no suit constraint (start of game or after 8 played).
    // Per Wikipedia: "the player who plays an 8 declares the suit that
    // must be played next." There is no clockwise/counter-clockwise
    // direction — only suit changes.
    var activeSuit: Char? = null  // 'C', 'D', 'H', 'S'

    // Save state for undo/revocation (Free game)
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
```

## Controller

```kotlin
// game/Controller.kt
package com.kibitzer.mobile.game

class Controller(private val numRooms: Int = 1) {

    private val players = mutableListOf<Player>()
    val rooms: List<Room> = List(numRooms) { Room() }
    private var nextId = 1
    private var gameHandler: GameHandler? = null
    private var currentGame: Game = Game.WARLORDS

    // --- Public accessors (used by AiPlayer, tests) ---

    fun findSession(sessionId: Int): Player? =
        players.find { it.sessionId == sessionId }

    fun getCurrentTurn(): Int = rooms[0].turn

    fun isGameOver(): Boolean {
        val room = rooms[0]
        return room.slots.any { sid ->
            sid != -1 && findSession(sid)?.hand?.size == 0
        }
    }

    fun setGame(game: Game) {
        currentGame = game
        rooms[0].rules = when (game) {
            Game.WARLORDS    -> WarlordsRules()
            Game.FREE        -> FreeRules()
            Game.CRAZY_EIGHTS -> CrazyEightsRules()
            Game.CHEAT       -> CheatRules()
            Game.OLD_MAID    -> OldMaidRules()
            Game.GO_FISH     -> GoFishRules()
        }
        rooms[0].cheatState = CheatState()
        rooms[0].activeSuit = null
        gameHandler = when (game) {
            Game.OLD_MAID -> OldMaidHandler()
            Game.GO_FISH  -> GoFishHandler()
            else          -> null
        }
    }

    // --- Session management ---

    fun createSession(): Int {
        val id = nextId++
        players.add(Player(id))
        return id
    }

    fun destroySession(sessionId: Int): Message? {
        val player = findSession(sessionId) ?: return null
        val room = rooms[player.room]
        if (player.slot in room.slots.indices) {
            room.slots[player.slot] = -1
        }
        players.remove(player)
        return Message("exit", buildExitJson(sessionId, player))
    }

    // --- Command handling (main entry point) ---

    fun handle(command: String, sessionId: Int): Message {
        val player = findSession(sessionId) ?: return Message("message", "unknown session")
        val prefix = command.take(5)
        val payload = command.drop(5)

        return when (Command.fromPrefix(prefix)) {
            Command.INIT     -> init(player)
            Command.JOIN     -> join(player, payload)
            Command.NICKNAME -> nic(player, payload)
            Command.GAME     -> setGameCommand(player, payload)
            Command.SHUFFLE  -> shuffle(player)
            Command.DEAL     -> deal(player)
            Command.PICKUP   -> pickup(player, payload)
            Command.PUTDOWN  -> putdown(player, payload)
            Command.EXCHANGE -> exchange(player, payload)
            Command.BID      -> bid(player, payload)
            Command.CHEAT    -> cheat(player, payload)
            Command.SKIP     -> skip(player)
            Command.CHAT     -> chat(player, payload)
            Command.ROOM     -> Message("message", "rooms not supported in v1")
            null             -> Message("message", "unknown command: $prefix")
        }
    }

    // --- Redaction ---

    /**
     * Redacts a message so a given player only sees their own hand.
     * For "cards" messages: replaces the `hand` field with the requesting
     * player's actual hand. Other players' hands are omitted entirely.
     * For "cheat" messages: the `cards[]` field is visible to all
     * (it's the revealed discard pile, not a private hand).
     * All other message types pass through unmodified.
     */
    fun redact(sessionId: Int, message: Message): Message {
        if (message.id == "cards") {
            val player = findSession(sessionId) ?: return message
            val handJson = player.hand.toJson()
            // Rebuild the data object with only this player's hand
            val data = message.data as? JsonObject ?: return message
            val redacted = data.toMutableMap()
            redacted["hand"] = JsonPrimitive(handJson)
            return Message("cards", JsonObject(redacted))
        }
        // "players", "message", "exchange", "cheat", "shuffle", "exit"
        // are all public — no redaction needed
        return message
    }

    // --- Internal methods (mirror C++ Controller) ---

    private fun init(player: Player): Message { /* ... */ }
    private fun join(player: Player, slot: String): Message { /* ... */ }
    private fun nic(player: Player, name: String): Message { /* ... */ }
    private fun setGameCommand(player: Player, name: String): Message {
        val game = Game.fromName(name)
            ?: return Message("message", "unknown game: $name")
        setGame(game)
        return Message("players", """{"game":"${game.name}"}""")
    }
    private fun shuffle(player: Player): Message { /* ... */ }

    /**
     * "Deal all" — the host deals cards to ALL players in the room.
     * Only the host (first player / slot 0) can issue this command.
     * Each player receives `rules.handSize(numPlayers)` cards.
     * For Old Maid: all 49 cards are dealt out (no stock pile).
     * For Go Fish: 5 cards each (7 for 2-3 players), rest is stock.
     */
    private fun deal(player: Player): Message {
        val room = rooms[player.room]
        val activePlayers = room.slots
            .mapNotNull { sid -> sid.takeIf { it != -1 } }
            .mapNotNull { sid -> findSession(sid) }
            .filter { it.state == PlayerState.JOINED }

        if (activePlayers.isEmpty()) {
            return Message("message", "no players to deal")
        }

        val numPlayers = activePlayers.size
        val cardsPerPlayer = room.rules.handSize(numPlayers)

        // Old Maid: deal ALL cards (no stock)
        if (room.rules is OldMaidRules) {
            room.deck.shuffle()
            // Deal all 49 cards out
            var idx = 0
            while (room.deck.packSize > 0) {
                val p = activePlayers[idx % numPlayers]
                p.hand.add(room.deck.pack.pop())
                idx++
            }
            // Discard initial pairs
            activePlayers.forEach { p ->
                val pairs = findPairsInHand(p.hand)
                pairs.forEach { (a, b) -> p.hand.remove(Hand().apply { add(a); add(b) }) }
            }
        } else {
            room.deck.shuffle()
            activePlayers.forEach { p ->
                room.deck.deal(p.hand, cardsPerPlayer)
            }
        }

        // Set turn to first player
        room.turn = activePlayers.first().sessionId
        for (p in activePlayers) {
            p.state = PlayerState.DEALT
        }

        return Message("cards", """{"message":"cards dealt","turn":${room.turn}}""")
    }

    private fun findPairsInHand(hand: Hand): List<Pair<Card, Card>> {
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
    private fun pickup(player: Player, count: String): Message { /* ... */ }
    private fun putdown(player: Player, handJson: String): Message { /* ... */ }
    private fun exchange(player: Player, data: String): Message { /* ... */ }
    private fun bid(player: Player, count: String): Message { /* ... */ }
    private fun cheat(player: Player, targetId: String): Message { /* ... */ }
    private fun skip(player: Player): Message { /* ... */ }
    private fun chat(player: Player, text: String): Message { /* ... */ }

    private fun nextTurn(room: Int): Int { /* ... */ }
    private fun canPlay(sessionId: Int, msg: StringBuilder): Boolean { /* ... */ }
    private fun setNextTurn(room: Int, msg: StringBuilder) { /* ... */ }
    private fun isTurn(player: Player): Boolean =
        rooms[player.room].turn == -1 || player.sessionId == rooms[player.room].turn
}
```

> Game-specific handler delegation (OldMaidHandler, GoFishHandler) and
> helper functions are documented in
> [`game-logic-handlers.md`](game-logic-handlers.md).