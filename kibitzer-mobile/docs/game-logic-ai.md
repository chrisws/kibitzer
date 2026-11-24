# Game Logic — AI Opponent

> Part of the Kibitzer Mobile game-logic documentation.
> See `game-logic.md` for core types, `game-logic-rules.md` for rule classes,
> and `game-logic-handlers.md` for handler/controller integration.

## AiPlayer (Rule-Based Expert System)

A distilled, rule-based AI opponent. Not an LLM — a hand-coded set of
heuristics that mimics expert play. Used for:
- **Single-player mode** in the app (fill empty slots with AI)
- **Automated test games** (AI vs AI, no physical devices needed)
- **CI integration tests** (deterministic game completion)

### Design Principles

- **Deterministic**: Given the same game state + seed, the AI makes the
  same decision. This makes tests reproducible.
- **Transport-agnostic**: Lives in `game/`, no Android or BLE deps.
- **Stateless per call**: `decide()` takes the current state and returns a
  command. No internal memory between turns (the game state *is* the memory).
- **Game-aware**: Delegates to game-specific strategy functions.
- **Bounded**: Always terminates. If no valid play exists, picks up. If in
  a bidding game and no valid bid, bids 0.

### Interface

```kotlin
// game/AiPlayer.kt
package com.kibitzer.mobile.game

class AiPlayer(
    private val seed: Long = 0L,
    private val difficulty: Difficulty = Difficulty.MEDIUM
) {
    enum class Difficulty { EASY, MEDIUM, HARD }

    private val rng = Random(seed)

    /**
     * Given the current game state, decide what command to send.
     * Returns a command string (e.g., "putd:[\"5C\",\"5D\"]") or null
     * if the AI should skip/wait.
     */
    fun decide(controller: Controller, sessionId: Int): String {
        val player = controller.findSession(sessionId)
            ?: return "skip:"
        val room = controller.rooms[player.room]
        val rules = room.rules

        return when (rules) {
            is WarlordsRules    -> warlordsMove(controller, player, room)
            is FreeRules        -> freeMove(controller, player, room)
            is CrazyEightsRules -> crazyEightsMove(controller, player, room)
            is CheatRules       -> cheatMove(controller, player, room)
            is OldMaidRules     -> oldMaidMove(controller, player, room)
            is GoFishRules      -> goFishMove(controller, player, room)
            else                -> "skip:"
        }
    }
}
```

### Strategy: Warlords / President

```kotlin
private fun warlordsMove(controller: Controller, player: Player, room: Room): String {
    val hand = player.hand
    val deck = room.deck

    // 1. Can we clear the discard? (3, 10, or 4-of-a-kind)
    val clearPlay = findClearPlay(hand)
    if (clearPlay != null && shouldClear(clearPlay, hand.size)) {
        return "putd:${clearPlay.toJson()}"
    }

    // 2. Can we play on the discard?
    val validPlays = findAllValidPlays(hand, deck)
    if (validPlays.isNotEmpty()) {
        // Strategy: play lowest-value valid cards (save high cards)
        val play = validPlays.minBy { it.sumOf { c -> rank32(c) } }
        return "putd:${play.toJson()}"
    }

    // 3. Must pick up
    return "picu:1"
}

private fun findClearPlay(hand: Hand): Hand? {
    // Check for 3 of a kind, 10 of a kind, or 4 of a kind
    val byRank = hand.toList().groupBy { rank32(it) }
    for ((rank, cards) in byRank) {
        if (rank == rank32(Card.THREE_CLUBS) && cards.size >= 1) return Hand().apply { add(cards[0]) }
        if (rank == rank32(Card.TEN_CLUBS) && cards.size >= 1) return Hand().apply { add(cards[0]) }
        if (cards.size == 4) return Hand().apply { addAll(Hand(cards)) }
    }
    return null
}

private fun shouldClear(card: Hand, handSize: Int): Boolean {
    // Don't clear if it leaves you with the 2 (wildcard) as your only card
    // and you're not in last place
    return true  // simplified; HARD difficulty adds positional awareness
}

private fun findAllValidPlays(hand: Hand, deck: Deck): List<Hand> {
    val last = deck.lastPlay ?: return emptyList()
    val lastRank = rank2A(last)
    val validCards = hand.toList().filter { card ->
        val r = rank2A(card)
        r == lastRank || r == lastRank + 1 || r == lastRank - 1
    }
    // Each valid card is a single-card play
    return validCards.map { Hand().apply { add(it) } }
}
```

### Strategy: Free

```kotlin
private fun freeMove(controller: Controller, player: Player, room: Room): String {
    val hand = player.hand
    val deck = room.deck

    // Free: play any card higher than the last play
    // Strategy: play the lowest valid card (save high cards for endgame)
    val last = deck.lastPlay
    if (last == null) {
        // First play: play lowest card
        val lowest = hand.toList().minBy { rank2A(it) }
        return "putd:[\"${lowest.name}\"]"
    }

    val validCards = hand.toList().filter { rank2A(it) > rank2A(last) }
    if (validCards.isNotEmpty()) {
        val card = validCards.minBy { rank2A(it) }
        return "putd:[\"${card.name}\"]"
    }

    return "picu:1"
}
```

### Strategy: Crazy Eights

```kotlin
private fun crazyEightsMove(controller: Controller, player: Player, room: Room): String {
    val hand = player.hand
    val deck = room.deck

    // 1. Play an 8 (wild) only when forced or to dump it early
    val eights = hand.toList().filter { it.name[0] == '8' }
    val last = deck.lastPlay

    if (last == null) {
        // First play: play an 8 if available (sets direction), else lowest
        if (eights.isNotEmpty() && difficulty == Difficulty.HARD) {
            return "putd:[\"${eights[0].name}\"]"
        }
        val lowest = hand.toList().minBy { rank2A(it) }
        return "putd:[\"${lowest.name}\"]"
    }

    // 2. Play matching rank (lowest suit preference)
    val matching = hand.toList().filter { rank2A(it) == rank2A(last) }
    if (matching.isNotEmpty()) {
        val card = matching.minBy { it.ordinal }
        return "putd:[\"${card.name}\"]"
    }

    // 3. Play an 8 (wild)
    if (eights.isNotEmpty()) {
        return "putd:[\"${eights[0].name}\"]"
    }

    // 4. Pick up
    return "picu:1"
}
```

### Strategy: Cheat

```kotlin
private fun cheatMove(controller: Controller, player: Player, room: Room): String {
    val hand = player.hand
    val deck = room.deck
    val cheatState = room.cheatState

    when (cheatState.phase) {
        CheatState.Phase.BIDDING -> {
            // Bid the number of matching-rank cards you actually hold
            // (plus 1 on EASY to bluff occasionally)
            val matching = hand.toList().count { rank2A(it) == cheatState.currentRank }
            val bid = if (difficulty == Difficulty.EASY && rng.nextBoolean())
                matching + 1  // bluff
            else
                matching.coerceAtMost(hand.size)
            return "bid:$bid"  // Controller interprets this
        }
        CheatState.Phase.PLAYING -> {
            // Play cards matching the bid (can lie — play any cards)
            val toPlay = hand.toList().filter { rank2A(it) == cheatState.currentRank }.take(cheatState.bid)
            // If not enough matching cards, fill with random cards (lying)
            val final = if (toPlay.size >= cheatState.bid) toPlay
                        else toPlay + hand.toList().filter { rank2A(it) != cheatState.currentRank }.take(cheatState.bid - toPlay.size)
            if (final.isEmpty()) return "skip:"
            val h = Hand().apply { final.forEach { add(it) } }
            return "putd:${h.toJson()}"
        }
        CheatState.Phase.RESOLVING -> "skip:"
    }
    return "skip:"
}
```

### Strategy: Old Maid

```kotlin
private fun oldMaidMove(controller: Controller, player: Player, room: Room): String {
    // Old Maid is turn-driven: the handler auto-finds pairs and draws.
    // The AI just needs to trigger its turn.
    return "picu:1"  // triggers OldMaidHandler.onTurnStart
}
```

### Strategy: Go Fish

```kotlin
private fun goFishMove(controller: Controller, player: Player, room: Room): String {
    val hand = player.hand
    val room2 = controller.rooms[player.room]

    // Strategy: ask the player who is most likely to have the rank
    // (the player with the most visible cards of that rank, or random)
    val myRanks = hand.toList().map { rank2A(it) }.toSet()
    if (myRanks.isEmpty()) return "picu:1"

    // Pick a rank I hold
    val targetRank = myRanks.randomElement(rng)

    // Pick a target player (not me, not eliminated)
    val opponents = room2.slots
        .mapIndexed { i, sid -> i to sid }
        .filter { (_, sid) -> sid != -1 && sid != player.sessionId }
        .map { (i, _) -> i }
        .shuffled(rng)

    if (opponents.isEmpty()) return "picu:1"

    val targetSlot = opponents.first()
    val rankChar = rankToChar(targetRank)
    return "ask:$targetSlot:$rankChar"
}
```

### AI Turn Loop (Host-Side)

When the host has AI players, it runs an AI turn loop after each real player's
move:

```kotlin
// In GameViewModel (host-side):
fun processAiTurns() {
    val state = _state.value
    val aiSessions = state.players
        .filter { it.isAi }
        .map { it.sessionId }

    for (sid in aiSessions) {
        if (_state.value.currentTurn == sid) {
            val command = aiPlayer.decide(controller!!, sid)
            val response = controller!!.handle(command, sid)
            onMessageReceived(response)
            // Small delay for UX (so the user can see AI "thinking")
            delay(500)
        }
    }
}
```

The AI turn loop is triggered after:
- A real player makes a move
- The turn advances to an AI player
- Game start (if AI goes first)

A 500ms delay between AI moves makes the game feel natural rather than
instantaneous.

### Difficulty Levels

| Level | Behaviour |
|---|---|
| `EASY` | Random valid moves, occasional bluffs (Cheat), plays 8s early (Crazy Eights) |
| `MEDIUM` | Lowest-valid-card strategy, saves high cards, no bluffs |
| `HARD` | Positional awareness (saves 2s in Warlords for last place), optimal clearing, strategic 8-holding |

For v1, implement `MEDIUM` only. `EASY` and `HARD` are future enhancements.

### Testing the AI

```kotlin
// test/game/AiVsAiTest.kt
class AiVsAiTest {

    @Test fun `ai vs ai warlords completes`() {
        runAiGame(Game.WARLORDS, maxTurns = 200)
    }

    @Test fun `ai vs ai free completes`() {
        runAiGame(Game.FREE, maxTurns = 200)
    }

    @Test fun `ai vs ai crazy eights completes`() {
        runAiGame(Game.CRAZY_EIGHTS, maxTurns = 200)
    }

    @Test fun `ai vs ai cheat completes`() {
        runAiGame(Game.CHEAT, maxTurns = 200)
    }

    @Test fun `ai vs ai old maid completes`() {
        runAiGame(Game.OLD_MAID, maxTurns = 100)
    }

    @Test fun `ai vs ai go fish completes`() {
        runAiGame(Game.GO_FISH, maxTurns = 200)
    }

    private fun runAiGame(game: Game, maxTurns: Int) {
        val controller = Controller()
        val ai1 = AiPlayer(seed = 42L)
        val ai2 = AiPlayer(seed = 99L)
        val ai3 = AiPlayer(seed = 7L)

        val s1 = controller.createSession()
        val s2 = controller.createSession()
        val s3 = controller.createSession()

        controller.setGame(game)
        controller.handle("join:0", s1)
        controller.handle("join:1", s2)
        controller.handle("join:2", s3)
        controller.handle("shuf:", s1)
        controller.handle("deal:", s1)
        controller.handle("deal:", s2)
        controller.handle("deal:", s3)

        val ais = mapOf(s1 to ai1, s2 to ai2, s3 to ai3)
        var turns = 0
        var gameOver = false

        while (!gameOver && turns < maxTurns) {
            val currentTurn = controller.getCurrentTurn()
            val ai = ais[currentTurn] ?: break
            val cmd = ai.decide(controller, currentTurn)
            val msg = controller.handle(cmd, currentTurn)
            gameOver = controller.isGameOver()
            turns++
        }

        assertTrue(gameOver, "Game should end within $maxTurns turns")
        assertTrue(turns < maxTurns)
    }
}
```

### Key Differences from C++

| Aspect | C++ (kibitzer) | Kotlin (mobile) |
|---|---|---|
| Memory | Manual `unique_ptr`, `malloc` | GC-managed, no manual memory |
| Card storage | `vector<Card>` (enum ints) | `MutableList<Card>` (enum class) |
| JSON | Manual string building | `kotlinx.serialization` or manual (same style) |
| Rules | C++ virtual methods | Kotlin abstract class |
| Threading | Single-threaded (lws loop) | Main thread (same) |
| Rooms | 10 rooms | 1 room (v1), extensible |
| Max players | 6 | 6 (same) |

The logic is **identical** — same rules, same turn order, same redaction.