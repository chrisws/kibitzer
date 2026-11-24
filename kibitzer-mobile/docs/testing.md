# Testing Strategy

## Test Pyramid

```
         ┌──────────┐
         │  Manual  │  ← 2-phone BLE games, battery, range
         ├──────────┤
         │ UI Tests │  ← Compose createComposeRule
         ├──────────┤
         │Integrat. │  ← BLE loopback, Controller+Protocol
         ├──────────┤
         │  Unit    │  ← Game logic, protocol, chunker
         └──────────┘
```

## Unit Tests

### Game Logic (`test/game/`)

| Test class | What it covers |
|---|---|
| `CardTest` | Name mapping, `fromName()` round-trip, all 52 cards |
| `HandTest` | add/remove/sort/has/hasHigher/isEqual/pop/peek/toJson |
| `DeckTest` | shuffle (52 cards, no dups), deal, pickup, putdown, takeDiscard, clearDiscard |
| `WarlordsRulesTest` | All `canPlay`/`isValidPlay`/`clearDiscard`/`isWinningPlay` cases |
| `FreeRulesTest` | Same coverage for free rules |
| `ControllerTest` | Full game: 3 players, join, shuffle, deal, play, pickup, skip, exchange, game over |

### Key test cases for WarlordsRules

```kotlin
@Test fun `can play 2 on anything`() {
    val deck = Deck().apply { putdown(Hand().apply { add(Card.QUEEN_CLUBS) }) }
    val hand = Hand().apply { add(Card.TWO_CLUBS) }
    assertTrue(WarlordsRules().canPlay(deck, hand))
}

@Test fun `cannot play 10 on ace`() {
    val deck = Deck().apply { putdown(Hand().apply { add(Card.ACE_CLUBS) }) }
    val hand = Hand().apply { add(Card.TEN_CLUBS) }
    assertFalse(WarlordsRules().canPlay(deck, hand))
}

@Test fun `cannot play same rank`() {
    val deck = Deck().apply { putdown(Hand().apply { add(Card.QUEEN_CLUBS) }) }
    val hand = Hand().apply { add(Card.QUEEN_HEARTS) }
    assertFalse(WarlordsRules().canPlay(deck, hand))
}

@Test fun `3s and 10s clear discard`() {
    val rules = WarlordsRules()
    assertTrue(rules.clearDiscard(Hand().apply { add(Card.THREE_CLUBS) }))
    assertTrue(rules.clearDiscard(Hand().apply { add(Card.TEN_CLUBS) }))
    assertTrue(rules.clearDiscard(Hand().apply {
        add(Card.FIVE_CLUBS); add(Card.FIVE_HEARTS)
        add(Card.FIVE_DIAMONDS); add(Card.FIVE_SPADES)
    }))
}

@Test fun `must play same number of cards as last play`() {
    val deck = Deck().apply {
        putdown(Hand().apply { add(Card.FIVE_CLUBS); add(Card.FIVE_HEARTS) })
    }
    val hand = Hand().apply { add(Card.SIX_CLUBS) }  // only 1 card
    assertFalse(WarlordsRules().isValidPlay(deck, hand))
}
```

### Protocol Tests (`test/protocol/`)

| Test class | What it covers |
|---|---|
| `MessageTest` | JSON serialize/deserialize round-trip |
| `CommandTest` | Prefix parsing, all 11 command types |
| `RedactionTest` | Player A's response doesn't contain Player B's hand |

### AI Player Tests (`test/game/`)

| Test class | What it covers |
|---|---|
| `AiPlayerTest` | Valid moves for each game, no infinite loops, respects rules |
| `AiVsAiTest` | Full AI vs AI games (all 6 games, 100 games each, seeded RNG) |

```kotlin
@Test fun `ai always makes valid moves in warlords`() {
    val controller = Controller()
    val ai = AiPlayer(seed = 42L)
    val s1 = controller.createSession()
    val s2 = controller.createSession()
    controller.setGame(Game.WARLORDS)
    controller.handle("join:0", s1)
    controller.handle("join:1", s2)
    controller.handle("shuf:", s1)
    controller.handle("deal:", s1)
    controller.handle("deal:", s2)

    repeat(100) {
        val turn = controller.getCurrentTurn()
        val cmd = ai.decide(controller, turn)
        val msg = controller.handle(cmd, turn)
        // Should never get "invalid play" error
        assertNotEquals("invalid play!", msg.data)
    }
}

@Test fun `ai vs ai completes all 6 games`() {
    for (game in Game.entries) {
        val controller = Controller()
        val ais = (1..3).map { AiPlayer(seed = it.toLong()) }
        val sessions = (1..3).map { controller.createSession() }
        controller.setGame(game)
        sessions.forEach { s -> controller.handle("join:${s-1}", s) }
        controller.handle("shuf:", sessions[0])
        sessions.forEach { s -> controller.handle("deal:", s) }

        var turns = 0
        while (!controller.isGameOver() && turns < 200) {
            val turn = controller.getCurrentTurn()
            val ai = ais[sessions.indexOf(turn)]
            val cmd = ai.decide(controller, turn)
            controller.handle(cmd, turn)
            turns++
        }
        assertTrue(controller.isGameOver(), "$game should end within 200 turns")
    }
}
```

### BLE Tests (`test/ble/`)

| Test class | What it covers |
|---|---|
| `ChunkerTest` | Split/reassemble round-trip, empty message, single chunk, max chunks, corrupted magic |

```kotlin
@Test fun `chunker round trip`() {
    val chunker = Chunker(maxChunkSize = 508)
    val original = """{"id":"cards","data":{"hand":["2C","3D","QH"],"pile":["4C"]}}"""
    val chunks = chunker.split(original)
    val reassembled = chunker.reassemble(chunks)
    assertEquals(original, reassembled)
}

@Test fun `chunker handles single byte`() {
    val chunker = Chunker(maxChunkSize = 508)
    val chunks = chunker.split("a")
    assertEquals(1, chunks.size)
    assertEquals("a", chunker.reassemble(chunks))
}
```

## Integration Tests

### BLE Loopback (`test/ble/BleLoopbackTest.kt`)

Simulates host + client on the same device (no physical BLE):

```kotlin
@Test fun `full game over loopback`() {
    val controller = Controller()
    val host = BleHost(controller)
    val client = BleClient()

    // Connect
    val sessionId = host.acceptClient(client)

    // Init
    client.send("init:")
    val initMsg = client.receive()
    assertEquals("init", initMsg.id)

    // Join, shuffle, deal
    client.send("join:0")
    client.send("shuf:")
    client.send("deal:")

    // Verify hand received
    val cardsMsg = client.receive()
    assertEquals("cards", cardsMsg.id)
    // hand should have 26 cards (52/2 players)
}
```

### Host Turn Timer (`test/ble/HostTurnTimerTest.kt`)

Verifies the host enforces the 30-second turn limit:

```kotlin
@Test fun `host auto-skips after 30s timeout`() {
    val controller = Controller()
    val host = BleHost(controller)
    val client = BleClient()

    val sessionId = host.acceptClient(client)
    client.send("init:")
    client.receive()
    client.send("join:0")
    client.send("shuf:")
    client.send("deal:")
    client.receive()

    // Start turn timer (host-side)
    host.startTurnTimer(sessionId, timeoutSeconds = 30)

    // Simulate 30s passing without any command
    advanceTimeBy(31_000)  // kotlinx-coroutines test scheduler

    // Host should have auto-skipped
    val msg = client.receive()
    assertEquals("cards", msg.id)
    // Turn should have advanced past this player
}

@Test fun `host timer resets on valid play`() {
    val controller = Controller()
    val host = BleHost(controller)
    val client = BleClient()

    val sessionId = host.acceptClient(client)
    client.send("init:")
    client.receive()
    client.send("join:0")
    client.send("shuf:")
    client.send("deal:")
    client.receive()

    host.startTurnTimer(sessionId, timeoutSeconds = 30)

    // Player plays a card at 15s (before timeout)
    advanceTimeBy(15_000)
    client.send("putd:[\"5C\"]")
    client.receive()

    // Timer should have reset — no skip at 30s
    advanceTimeBy(15_000)
    // No skip message should have been sent
    assertNull(client.tryReceive())
}
```

### Controller + Protocol Integration

Full game simulation without BLE:

```kotlin
@Test fun `three player warlords game`() {
    val controller = Controller()
    val s1 = controller.createSession()
    val s2 = controller.createSession()
    val s3 = controller.createSession()

    // All join
    controller.handle("join:0", s1)
    controller.handle("join:1", s2)
    controller.handle("join:2", s3)

    // Shuffle + deal
    controller.handle("shuf:", s1)
    controller.handle("deal:", s1)
    controller.handle("deal:", s2)
    controller.handle("deal:", s3)

    // Play until someone wins (max 100 turns to prevent infinite loop)
    var turns = 0
    var winner = -1
    while (winner == -1 && turns < 100) {
        // Each player tries to play, picks up if can't
        for (s in listOf(s1, s2, s3)) {
            val msg = controller.handle("putd:[\"${getValidCard(controller, s)}\"]", s)
            if (msg.id == "cards" && isGameOver(msg)) {
                winner = s
                break
            }
        }
        turns++
    }
    assertTrue(winner != -1, "Game should end within 100 turns")
}
```

## UI Tests (Compose)

```kotlin
@Composable
fun GameScreen_test() {
    val viewModel = GameViewModel()
    // Set up fake state
    viewModel.state.value = GameState(
        screen = Screen.GAME,
        myHand = listOf(Card.FIVE_CLUBS, Card.QUEEN_HEARTS),
        isMyTurn = true,
        // ...
    )
    GameScreen(viewModel = viewModel)
}

@Test fun `select card on tap`() {
    composeTestRule.setContent { GameScreen_test() }
    composeTestRule.onNodeWithContentDescription("Five of Clubs").performClick()
    // Verify card is selected (lifted up)
}

@Test fun `play button enabled when cards selected`() {
    composeTestRule.setContent { GameScreen_test() }
    composeTestRule.onNodeWithContentDescription("Five of Clubs").performClick()
    composeTestRule.onNodeWithText("Play").assertIsEnabled()
}
```

## Manual Test Checklist

| # | Test | Devices | Pass |
|---|---|---|---|
| 1 | Create + join (2 phones) | 2 | ☐ |
| 2 | Full 2-player game | 2 | ☐ |
| 3 | Full 3-player game | 3 | ☐ |
| 4 | Full 6-player game | 6 | ☐ |
| 5 | Chat during game | 2 | ☐ |
| 6 | Card exchange (offer/take) | 2 | ☐ |
| 7 | Player disconnects mid-game | 3 | ☐ |
| 8 | Turn timeout auto-skip | 2 | ☐ |
| 9 | Deck exhaustion + reshuffle | 2 | ☐ |
| 10 | Play again after game over | 2 | ☐ |
| 11 | BLE range (10m) | 2 | ☐ |
| 12 | BLE through wall | 2 | ☐ |
| 13 | 2-hour battery test | 2 | ☐ |
| 14 | Android 5.0 (API 21) | 1 | ☐ |
| 15 | Android 14 (API 34) | 1 | ☐ |
| 16 | Host screen off mid-game (foreground service keeps alive) | 2 | ☐ |
| 17 | Single-player vs AI (3 AI opponents, full game) | 1 | ☐ |
| 18 | AI turn delay feels natural (500ms between moves) | 1 | ☐ |

## CI/CD

- **Unit + Integration tests**: Run on every commit (GitHub Actions / GitLab CI)
- **UI tests**: Run on emulator (API 29 + API 34)
- **BLE tests**: Manual only (no BLE in CI)
- **Build**: `assembleDebug` + `assembleRelease` on every commit
- **Lint**: `ktlint` + Android Lint on every commit