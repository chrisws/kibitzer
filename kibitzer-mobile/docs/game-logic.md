# Game Logic — Kotlin Port

Direct port of the C++ `cards`, `rules`, and `controller` modules to Kotlin.
The C++ source in `../server/` is the authoritative reference for **Warlords**
and **Free** rules and state transitions. The remaining four games
(Crazy Eights, Cheat, Old Maid, Go Fish) are new implementations with no
C++ counterpart — their rules are defined here in Kotlin only.
No Android or BLE dependencies. Pure domain logic.

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

## WarlordsRules

```kotlin
// game/WarlordsRules.kt
package com.kibitzer.mobile.game

// Rank: 3=0, 4=1, ..., 10=7, J=8, Q=9, K=10, A=11, 2=12
// (2 is the wildcard, played last in the sequence)
fun rank32(card: Card): Int = when (card.name[0]) {
    '3' -> 0; '4' -> 1; '5' -> 2; '6' -> 3; '7' -> 4
    '8' -> 5; '9' -> 6; 'X' -> 7; 'J' -> 8; 'Q' -> 9
    'K' -> 10; 'A' -> 11; '2' -> 12
    else -> -1
}

class WarlordsRules : Rules() {

    override fun canPlay(deck: Deck, hand: Hand): Boolean {
        if (hand.size == 0) return false
        if (hand.hasEqual(Card.TWO_CLUBS, ::rank32)) return true
        if (deck.discardSize == 0) return true
        val last = deck.lastPlay ?: return true
        if (rank32(last) == rank32(Card.TWO_CLUBS)) return true
        return hand.hasHigher(last, deck.lastSize, ::rank32)
    }

    override fun canRevoke() = false

    override fun clearDiscard(hand: Hand): Boolean {
        return hand.hasEqual(Card.THREE_CLUBS, ::rank32)
            || hand.hasEqual(Card.TEN_CLUBS, ::rank32)
            || hand.size == 4
    }

    override fun faceDown() = false

    override fun getRank() = ::rank32

    override fun handSize(players: Int) = 52 / players

    override fun isValidPlay(deck: Deck, hand: Hand): Boolean {
        if (hand.size == 0) return false
        if (!hand.isEqual(hand.peek(), ::rank32)) return false
        if (hand.hasEqual(Card.TWO_CLUBS, ::rank32)) return true
        if (deck.discardSize == 0) return true
        val last = deck.lastPlay ?: return true
        if (rank32(last) == rank32(Card.TWO_CLUBS)) return true
        return hand.hasHigher(last, 1, ::rank32) && hand.size == deck.lastSize
    }

    override fun isWinningPlay(deck: Deck, hand: Hand): Boolean = hand.size == 0

    override fun name() = "Warlords and Scumbags"

    override fun url() = "https://en.wikipedia.org/wiki/President_(card_game)"

    override fun noPlayTakesDiscard() = true

    override fun setNextTurn(hand: Hand): Boolean = !clearDiscard(hand)
}
```

## FreeRules

```kotlin
// game/FreeRules.kt
package com.kibitzer.mobile.game

// Standard rank: 2=0, 3=1, ..., 10=8, J=9, Q=10, K=11, A=12
fun rank2A(card: Card): Int = when (card.name[0]) {
    '2' -> 0; '3' -> 1; '4' -> 2; '5' -> 3; '6' -> 4
    '7' -> 5; '8' -> 6; '9' -> 7; 'X' -> 8; 'J' -> 9
    'Q' -> 10; 'K' -> 11; 'A' -> 12
    else -> -1
}

class FreeRules : Rules() {

    override fun canPlay(deck: Deck, hand: Hand): Boolean {
        if (hand.size == 0) return false
        if (deck.discardSize == 0) return true
        val last = deck.lastPlay ?: return true
        return hand.hasHigher(last, 1, ::rank2A)
    }

    override fun canRevoke() = true
    override fun clearDiscard(hand: Hand) = false
    override fun faceDown() = false
    override fun getRank() = ::rank2A
    override fun handSize(players: Int) = 7
    override fun isValidPlay(deck: Deck, hand: Hand) = true
    override fun isWinningPlay(deck: Deck, hand: Hand) = false
    override fun name() = "Rules Free"
    override fun url() = "https://en.wikipedia.org/wiki/Card_game"
    override fun noPlayTakesDiscard() = false
    override fun setNextTurn(hand: Hand) = true
}
```

## CrazyEightsRules

```kotlin
// game/CrazyEightsRules.kt
package com.kibitzer.mobile.game

// Standard rank: 2=0, 3=1, ..., 10=8, J=9, Q=10, K=11, A=12
// 8s are wild — can be played on anything

class CrazyEightsRules : Rules() {

    // Per Wikipedia: 8s change the SUIT (not direction).
    // The player playing an 8 declares the next suit.
    // 5 cards dealt (7 for 2 players). Draw from stock until playable.
    // 8s score 50, court cards 10, others face value.

    override fun canPlay(deck: Deck, hand: Hand): Boolean {
        if (hand.size == 0) return false
        if (deck.discardSize == 0) return true
        val last = deck.lastPlay ?: return true
        // 8s are always playable
        if (hand.hasEqual(Card.EIGHT_CLUBS, ::rank2A)) return true
        // Same rank as top of discard
        return hand.hasEqual(last, ::rank2A)
    }

    override fun canRevoke() = false

    override fun clearDiscard(hand: Hand): Boolean = false

    override fun faceDown() = false

    override fun getRank() = ::rank2A

    // 5 cards for 3+ players, 7 for 2 players (per Wikipedia)
    override fun handSize(players: Int) = if (players <= 2) 7 else 5

    override fun isValidPlay(deck: Deck, hand: Hand): Boolean {
        if (hand.size == 0) return false
        if (hand.size != 1) return false  // Crazy Eights: one card at a time
        val card = hand.peek()
        if (deck.discardSize == 0) return true
        val last = deck.lastPlay ?: return true
        // 8s are always valid
        if (rank2A(card) == rank2A(Card.EIGHT_CLUBS)) return true
        return rank2A(card) == rank2A(last)
    }

    override fun isWinningPlay(deck: Deck, hand: Hand) = hand.size == 0

    override fun name() = "Crazy Eights"

    override fun url() = "https://en.wikipedia.org/wiki/Crazy_Eights"

    override fun noPlayTakesDiscard() = true

    override fun setNextTurn(hand: Hand) = true
}
```

## CheatRules

```kotlin
// game/CheatRules.kt
package com.kibitzer.mobile.game

// Cheat (also known as "Cheat" or "Bullshit"):
// Players bid how many cards they can play, then play them.
// Opponents can call "cheat" if they believe the bid was a lie.
// If called and the bid was valid, the caller picks up the discard.
// If called and the bid was a lie, the bidder picks up the discard.

// Bidding state lives in Room, not in Rules (Rules is stateless)
data class CheatState(
    val phase: Phase = Phase.BIDDING,
    val bid: Int = 0,
    val bidSessionId: Int = -1,
    val called: Boolean = false,
    val calledBy: Int = -1,
    val playedCards: List<Card> = emptyList(),
    // Sequential rank: 1=A, 2=2, ..., 13=K. Wraps after K.
    val currentRank: Int = 1
) {
    enum class Phase { BIDDING, PLAYING, RESOLVING }

    fun withBid(sessionId: Int, count: Int) =
        copy(phase = Phase.PLAYING, bid = count, bidSessionId = sessionId)

    fun withPlayedCards(cards: List<Card>) =
        copy(playedCards = cards)

    fun withCalled(sessionId: Int) =
        copy(called = true, calledBy = sessionId, phase = Phase.RESOLVING)

    // Advance to next turn: reset bid/call/played, increment rank (A→2→...→K→A)
    fun nextTurn() = copy(
        phase = Phase.BIDDING,
        bid = 0,
        bidSessionId = -1,
        called = false,
        calledBy = -1,
        playedCards = emptyList(),
        currentRank = if (currentRank >= 13) 1 else currentRank + 1
    )

    fun reset() = CheatState()

    // Check the cards the bidder actually played (not their remaining hand)
    fun isBidValid(played: List<Card>): Boolean {
        val matching = played.count { rank2A(it) == currentRank }
        return matching >= bid
    }
}

class CheatRules : Rules() {

    // Players can always play (lying is the point).
    // Bidding/calling flow is handled by Controller using Room.cheatState.
    // The current rank is tracked in CheatState.currentRank (sequential A→K).

    override fun canPlay(deck: Deck, hand: Hand): Boolean {
        // In Cheat you can always play — lying is the point.
        // Truth is verified at challenge time, not at play time.
        return hand.size > 0
    }

    override fun canRevoke() = false

    override fun clearDiscard(hand: Hand): Boolean = false

    override fun faceDown() = true  // Cards are played face-down

    override fun getRank() = ::rank2A

    // All cards are dealt out (no stock). 52 cards for ≤4 players,
    // 104 (two decks) for 5+ players.
    override fun handSize(players: Int) = (if (players <= 4) 52 else 104) / players

    override fun isValidPlay(deck: Deck, hand: Hand): Boolean {
        // In Cheat, you can always play (lying is the point).
        // Truth is verified at challenge time via CheatState.isBidValid.
        return hand.size > 0
    }

    override fun isWinningPlay(deck: Deck, hand: Hand) = hand.size == 0

    override fun name() = "Cheat"

    override fun url() = "https://en.wikipedia.org/wiki/Cheat_(card_game)"

    override fun noPlayTakesDiscard() = true

    override fun setNextTurn(hand: Hand) = true
}
```

### Cheat Game Flow

The Cheat game follows a four-phase cycle each turn:

1. **Bid** — The current player declares how many cards they will play
   (0–hand size). The bid is stored in `Room.cheatState`.
2. **Play** — The bidder plays the declared number of cards face-down
   to the discard pile. They claim the cards are of the current rank
   (tracked in `cheatState.currentRank`), but may lie.
3. **Resolve** — Any opponent may call "cheat" before the next turn
   begins. If called:
   - **Bid was valid** (bidder had enough matching cards): the caller
     picks up the entire discard pile.
   - **Bid was a lie** (bidder didn't have enough matching cards): the
     bidder picks up the entire discard pile.
4. **Penalty / Next Turn** — After resolution, `cheatState` advances to
   the next turn: the rank increments (A→2→3→...→K→A) and play passes
   to the next player.

The `Controller` manages this flow via `Room.cheatState`:

```kotlin
// In Controller.handle() for Cheat:
Command.BID -> {
    val count = payload.toIntOrNull() ?: return Message("message", "invalid bid")
    if (count < 0 || count > player.hand.size)
        return Message("message", "invalid bid")
    room.cheatState = room.cheatState.withBid(player.sessionId, count)
    Message("message", "bid $count (rank ${room.cheatState.currentRank})")
}
Command.PUTDOWN -> {
    // Bidder plays cards face-down to discard; store them for later resolution
    val cards = parseCards(payload)
    if (cards.size != room.cheatState.bid)
        return Message("message", "must play exactly ${room.cheatState.bid} cards")
    player.hand.remove(Hand().apply { addAll(cards) })
    room.deck.discard(cards)
    room.cheatState = room.cheatState.withPlayedCards(cards)
    Message("message", "played ${cards.size} cards face-down")
}
Command.CHEAT -> {
    if (!room.cheatState.called) {
        room.cheatState = room.cheatState.withCalled(player.sessionId)
        // Resolve: check the cards the bidder actually played
        val bidder = findPlayerById(room, room.cheatState.bidSessionId)
        val valid = room.cheatState.isBidValid(room.cheatState.playedCards)
        val loser = if (valid) player else bidder
        room.deck.pickup(loser.hand, room.deck.discardSize)
        room.cheatState = room.cheatState.nextTurn()
        Message("message", "cheat resolved — ${loser.name} picks up")
    } else {
        Message("message", "already called")
    }
}
```

## OldMaidRules

```kotlin
// game/OldMaidRules.kt
package com.kibitzer.mobile.game

// Old Maid:
// 24 pairs + 1 Old Maid (unmatched) = 49 cards
// Players draw from hand, look for pairs, discard pairs.
// Player left with the Old Maid loses.

class OldMaidRules : Rules() {

    // 49-card deck: 24 pairs (48 cards) + 1 Old Maid
    // Construction: take a full 52-card deck, remove 2C, 2D, 2H.
    // The remaining 2S is the Old Maid (only one 2 in the deck).
    // Pairs: 3C/3D, 3H/3S, 4C/4D, 4H/4S, ... AC/AD, AH/AS (24 pairs)
    // Old Maid: 2S (unmatched)

    companion object {
        val OLD_MAID = Card.TWO_SPADES
        val DECK_CARDS: List<Card> = Card.entries.filter {
            it != Card.TWO_CLUBS && it != Card.TWO_DIAMONDS && it != Card.TWO_HEARTS
        }  // 49 cards
    }

    override fun canPlay(deck: Deck, hand: Hand): Boolean {
        // In Old Maid, you "play" by finding pairs in your hand
        // You can always attempt to find a pair
        return hand.size > 1
    }

    override fun canRevoke() = false

    override fun clearDiscard(hand: Hand): Boolean = false

    override fun faceDown() = true  // Cards are face down

    override fun getRank() = ::rank2A

    override fun handSize(players: Int) = 49 / players

    override fun isValidPlay(deck: Deck, hand: Hand): Boolean {
        // A valid "play" is finding a pair in hand
        // This is checked by the Controller, not the rules
        return true
    }

    override fun isWinningPlay(deck: Deck, hand: Hand): Boolean {
        // You win if you have no Old Maid
        // You lose if you have the Old Maid
        return hand.size == 0
    }

    override fun name() = "Old Maid"

    override fun url() = "https://en.wikipedia.org/wiki/Old_Maid_(card_game)"

    override fun noPlayTakesDiscard() = false

    override fun setNextTurn(hand: Hand) = true
}
```

## GoFishRules

```kotlin
// game/GoFishRules.kt
package com.kibitzer.mobile.game

// Go Fish:
// Players ask opponents for a specific rank.
// If the opponent has it, they give all cards of that rank ("hit").
// If not, they say "Go fish!" and the asker draws from the deck.
// Complete sets of 4 are discarded. First to empty hand wins.

class GoFishRules : Rules() {

    override fun canPlay(deck: Deck, hand: Hand): Boolean {
        if (hand.size == 0) return false
        // In Go Fish, you "play" by asking for a rank
        // You can always ask
        return true
    }

    override fun canRevoke() = false

    override fun clearDiscard(hand: Hand): Boolean = false

    override fun faceDown() = true  // Cards are face down

    override fun getRank() = ::rank2A

    override fun handSize(players: Int) = if (players <= 3) 7 else 5

    override fun isValidPlay(deck: Deck, hand: Hand): Boolean {
        // A valid "play" is asking for a rank you hold
        // This is checked by the Controller
        return true
    }

    override fun isWinningPlay(deck: Deck, hand: Hand) = hand.size == 0

    override fun name() = "Go Fish"

    override fun url() = "https://en.wikipedia.org/wiki/Go_Fish"

    override fun noPlayTakesDiscard() = false

    override fun setNextTurn(hand: Hand) = true
}
```

> **Note:** Old Maid and Go Fish use a fundamentally different interaction
> model from the deck/discard games. In these games, players act on their
> *hand* (finding pairs, asking opponents for ranks) rather than playing
> cards to a shared discard pile. The `Rules` overrides above are minimal
> stubs — the actual game logic is handled by the `GameHandler` classes
> described in the [Game-Specific Controller Extensions](#game-specific-controller-extensions) section.
> The `faceDown() = true` flag tells the UI to render cards face-down.
>
> **Old Maid interaction model:** Pair-finding is **automatic**. When a
> player's turn begins, the `OldMaidHandler` scans their hand, discards all
> matching pairs, and draws one card from a random opponent. The player's
> only UI action is tapping "My Turn" (or the turn auto-advances in AI mode).
> There is no "tap a card to reveal it" mechanic — the hand is logically
> face-down for other players, but the *owner* always sees their full hand
> in the UI. Pairs are resolved server-side before the state is sent.
>
> **Go Fish interaction model:** The player issues an **ask** command
> (`ask:<targetSlot>:<rank>`) specifying which opponent to ask and which
> rank to request. This is a dedicated command (not a repurposed `putd:`)
> to keep the protocol unambiguous. The handler resolves the ask
> server-side: if the target holds the rank, all matching cards transfer
> to the asker ("hit"); otherwise the asker draws from the deck ("go fish").

## Multi-Round Scoring

Points are awarded at the end of each round (when a player empties their
hand). Scores persist across rounds via `Player.points`. The "Play Again"
button on `GameOverScreen` triggers a new round with the same players and
game type.

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
    private fun bid(player: Player, count: String): Message {
        val room = rooms[player.room]
        val n = count.toIntOrNull() ?: return Message("message", "invalid bid")
        if (n < 0 || n > player.hand.size) {
            return Message("message", "invalid bid: $n")
        }
        room.cheatState = room.cheatState.withBid(player.sessionId, n)
        return Message("players", """{"message":"(${player.nickname}) bid $n"}""")
    }

    /**
     * Cheat "call" — any player may call, not just the current turn player.
     * This is the non-turn-player exception: the call is valid as long as
     * the cheat state is in PLAYING phase (bid has been made, not yet resolved).
     * Per Wikipedia: "once the next player has placed cards, it is too late
     * to call out any previous players."
     */
    private fun cheat(player: Player, targetId: String): Message {
        val room = rooms[player.room]
        val state = room.cheatState
        if (state.phase != CheatState.Phase.PLAYING) {
            return Message("message", "nothing to call")
        }
        if (state.called) {
            return Message("message", "already called")
        }
        // Resolve the call
        state.let {
            room.cheatState = it.withCalled(player.sessionId)
        }
        val bidder = findSession(room.cheatState.bidSessionId)
            ?: return Message("message", "bidder not found")
        val valid = room.cheatState.isBidValid(bidder.hand)
        val loser = if (valid) player else bidder
        // Loser picks up the entire discard pile
        room.deck.takeDiscard(loser.hand)
        room.cheatState = room.cheatState.nextTurn()
        val result = if (valid) "truthful" else "liar"
        return Message("cheat", """{"callerId":${player.sessionId},"targetId":${bidder.sessionId},"result":"$result","message":"(${player.nickname}) called cheat — ${loser.nickname} picks up"}""")
    }

    private fun skip(player: Player): Message { /* ... */ }
    private fun chat(player: Player, text: String): Message { /* ... */ }

    private fun nextTurn(room: Int): Int { /* ... */ }
    private fun canPlay(sessionId: Int, msg: StringBuilder): Boolean { /* ... */ }
    private fun setNextTurn(room: Int, msg: StringBuilder) { /* ... */ }
    private fun isTurn(player: Player): Boolean =
        rooms[player.room].turn == -1 || player.sessionId == rooms[player.room].turn
}
```

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
Only the language idioms change.