# Game Rules — Per-Game Implementations

Per-game `Rules` subclasses and game-specific state. See
[game-logic.md](game-logic.md) for the core types (Card, Hand, Deck,
`Rules` abstract class, Player, Room, Controller).

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
> described in the [Game-Specific Controller Extensions](game-logic-handlers.md#game-specific-controller-extensions) section.
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