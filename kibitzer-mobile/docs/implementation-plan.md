# Implementation Plan

## Phase Overview

```
Phase 1: Game Engine + Card Games (Week 1-3)
Phase 2: Protocol Layer (Week 3)
Phase 3: BLE Transport (Week 4-5)
Phase 3b: BLE Hardening + Buffer (Week 6)
Phase 4: UI - Core Screens (Week 7-8)
Phase 5: UI - Polish + Game Flow (Week 9)
Phase 6: Testing + Bug Fixing (Week 10)
Phase 7: Beta + Release (Week 11-13)
```

> **Note:** Phase 3b is a dedicated buffer week for BLE hardware issues
> that are difficult to predict (OEM-specific GATT Server bugs, range
> problems, connection stability). BLE development is notoriously hard
> to estimate because issues only surface on physical devices.

> **AI Player:** A rule-based AI opponent (distilled expert system) is built
> alongside the game engine in Phase 1 and integrated into the UI in Phase 4.
> It enables single-player mode and automated test games without physical
> devices. See `game/AiPlayer.kt` in the game-logic doc.

---

## Phase 1: Game Engine (Week 1-3)

**Goal**: Working, tested game logic with zero Android dependencies.
Use the C++ source in `../server/` as the authoritative reference for
**Warlords** and **Free** rules. The remaining four games (Crazy Eights,
Cheat, Old Maid, Go Fish) are new implementations defined in Kotlin only.

### Tasks

| # | Task | Files | Est. |
|---|---|---|---|
| 1.1 | Create Android project skeleton (Gradle, manifest, empty Activity) | `app/build.gradle.kts`, `AndroidManifest.xml`, `MainActivity.kt` | 0.5d |
| 1.2 | Implement `Card` enum with name mapping | `game/Card.kt` | 0.5d |
| 1.3 | Implement `Hand` class (add, remove, sort, has, hasHigher, isEqual, toJson) | `game/Hand.kt` | 1d |
| 1.4 | Implement `Deck` class (shuffle, deal, pickup, putdown, takeDiscard) | `game/Deck.kt` | 1d |
| 1.5 | Implement `Rules` abstract class | `game/Rules.kt` | 0.5d |
| 1.6 | Implement `WarlordsRules` (ref: `../server/rules.cpp`, struct `Warlords`) | `game/WarlordsRules.kt` | 1d |
| 1.7 | Implement `FreeRules` (ref: `../server/rules.cpp`, struct `RulesFree`) | `game/FreeRules.kt` | 0.5d |
| 1.8 | Implement `CrazyEightsRules` (new, no C++ ref) | `game/CrazyEightsRules.kt` | 1d |
| 1.9 | Implement `CheatRules` (new, no C++ ref) | `game/CheatRules.kt` | 2d |
| 1.10 | Implement `OldMaidRules` (new, no C++ ref) | `game/OldMaidRules.kt` | 0.5d |
| 1.11 | Implement `GoFishRules` (new, no C++ ref) | `game/GoFishRules.kt` | 1d |
| 1.12 | Implement `Player` class | `game/Player.kt` | 0.5d |
| 1.13 | Implement `Room` class | `game/Room.kt` | 0.5d |
| 1.14 | Implement `Controller` (all command handlers, game selection) | `game/Controller.kt` | 2d |
| 1.15 | Unit tests: Card, Hand, Deck | `test/game/CardTest.kt`, `HandTest.kt`, `DeckTest.kt` | 1d |
| 1.16 | Unit tests: WarlordsRules (all canPlay/isValidPlay cases) | `test/game/WarlordsRulesTest.kt` | 1d |
| 1.16a | Unit tests: FreeRules (canPlay, revoke, handSize) | `test/game/FreeRulesTest.kt` | 0.5d |
| 1.17 | Unit tests: CrazyEightsRules (wildcard, direction, 8s) | `test/game/CrazyEightsRulesTest.kt` | 1d |
| 1.18 | Unit tests: CheatRules (bidding, calling, stealing) | `test/game/CheatRulesTest.kt` | 1d |
| 1.19 | Unit tests: OldMaidRules (pair matching, elimination) | `test/game/OldMaidRulesTest.kt` | 0.5d |
| 1.20 | Unit tests: GoFishRules (asking, hitting, going fish) | `test/game/GoFishRulesTest.kt` | 1d |
| 1.21 | Unit tests: Controller (full game simulation, all 6 games) | `test/game/ControllerTest.kt` | 1.5d |
| 1.22 | Implement `AiPlayer` (rule-based expert system, all 6 games) | `game/AiPlayer.kt` | 2d |
| 1.23 | Unit tests: AiPlayer (valid moves, no infinite loops, wins games) | `test/game/AiPlayerTest.kt` | 1d |
| 1.24 | Unit tests: AI vs AI full game (all 6 games, 100 games each) | `test/game/AiVsAiTest.kt` | 1d |

**Deliverable**: `Controller.handle()` works end-to-end in a unit test for all
six card games (Warlords, Free, Crazy Eights, Cheat, Old Maid, Go Fish).
A test creates sessions, joins them, shuffles, deals, plays cards, and
verifies the winner. No Android, no BLE — pure logic. The `AiPlayer` can
drive a full game autonomously against the `Controller`.

> **Timeline risk:** Phase 1 carries 24+ tasks in 3 weeks. The four new
> games (Crazy Eights, Cheat, Old Maid, Go Fish) have no C++ reference and
> require original rule design, making them the highest-risk items. If
> progress falls behind by end of Week 2, consider deferring Go Fish and
> Old Maid to Phase 2 (they are hand-based games with simpler turn logic
> and can be added incrementally).

**Milestone check**: Run `ControllerTest` — a full game of each of the 6 card
games completes without errors.

---

## Phase 2: Protocol Layer (Week 3)

**Goal**: Message serialization/deserialization matching kibitzer's JSON format.

### Tasks

| # | Task | Files | Est. |
|---|---|---|---|
| 2.1 | Define `Command` enum with prefix mapping | `protocol/Command.kt` | 0.5d |
| 2.2 | Define `Message` data class with JSON serialization | `protocol/Message.kt` | 0.5d |
| 2.3 | Define `Envelope` for `{"id":"...","data":{...}}` | `protocol/Envelope.kt` | 0.5d |
| 2.4 | Implement `Hand.fromJson()` (parse `["QH","QD"]` → `Hand`) | `game/Hand.kt` (add) | 0.5d |
| 2.5 | Implement `Controller.redact()` for per-player hand filtering | `game/Controller.kt` (add) | 1d |
| 2.6 | Unit tests: Message serialization round-trip | `test/protocol/MessageTest.kt` | 0.5d |
| 2.7 | Unit tests: Redaction (player A doesn't see player B's hand) | `test/protocol/RedactionTest.kt` | 0.5d |

**Deliverable**: `Controller.handle()` returns properly formatted JSON messages.
Redaction verified in tests.

---

## Phase 3: BLE Transport (Week 4-5)

**Goal**: Two phones can exchange commands and state over BLE.

### Tasks

| # | Task | Files | Est. |
|---|---|---|---|
| 3.1 | Define `GattConstants` (UUIDs, MTU, limits) | `ble/GattConstants.kt` | 0.5d |
| 3.2 | Implement `Chunker` (split/reassemble with 4-byte header) | `ble/Chunker.kt` | 1d |
| 3.3 | Unit tests: Chunker (split → reassemble round-trip, edge cases) | `test/ble/ChunkerTest.kt` | 0.5d |
| 3.4 | Implement `BleHost` (advertising, accept connections, relay) | `ble/BleHost.kt` | 2d |
| 3.5 | Implement `BleClient` (scan, connect, send, receive) | `ble/BleClient.kt` | 2d |
| 3.6 | Implement handshake protocol (hello/welcome) | `ble/BleHost.kt`, `ble/BleClient.kt` | 1d |
| 3.7 | Implement `BleService` (Android BLE API wrappers) | `ble/BleService.kt` | 1d |
| 3.8 | Integration test: Host + Client on same device (loopback) | `test/ble/BleLoopbackTest.kt` | 1d |
| 3.9 | Manual test: Two physical phones, connect, exchange pings | *(manual)* | 1d |
| 3.10 | Handle disconnect/reconnect logic | `ble/BleClient.kt` | 1d |
| 3.11 | BLE permissions in manifest + runtime permission request | `AndroidManifest.xml`, `MainActivity.kt` | 0.5d |
| 3.12 | Foreground service for host (keeps app alive when screen off) | `service/GameService.kt`, `AndroidManifest.xml` | 1d |

**Deliverable**: Two phones connect over BLE. Client sends `init:`, host responds
with JSON. Client sends `chat:hello`, host broadcasts to all. The host runs a
foreground service with a persistent notification so Android does not kill the
app when the screen turns off.

**Milestone check**: On two physical phones, the host sees the client connect,
and the client receives the welcome message.

---

## Phase 3b: BLE Hardening + Reliability (Week 6)

**Goal**: Harden the BLE transport against real-world failures. This is a
dedicated buffer week for hardware-specific issues that only surface on
physical devices.

### Tasks

| # | Task | Files | Est. |
|---|---|---|---|
| 3b.1 | GATT Server callback thread marshalling (Handler on main looper) | `ble/BleHost.kt` | 0.5d |
| 3b.2 | Connection health check + periodic re-subscription to notifications | `ble/ConnectionMonitor.kt` | 1d |
| 3b.3 | Heartbeat/ping mechanism (10s host→client, 20s pong timeout) | `ble/BleHost.kt`, `ble/BleClient.kt` | 1d |
| 3b.4 | Exponential backoff reconnection (1s, 2s, 4s, 8s, 16s + jitter) | `ble/BleClient.kt` | 0.5d |
| 3b.5 | State restoration on reconnect (full redacted state resend) | `ble/BleHost.kt`, `game/Controller.kt` | 1d |
| 3b.6 | Multi-client connection enforcement (reject 6th client gracefully) | `ble/BleHost.kt` | 0.5d |
| 3b.7 | Graceful degradation (reduce max players if connections unstable) | `ble/BleHost.kt` | 0.5d |
| 3b.8 | BLUETOOTH_SCAN/BLUETOOTH_CONNECT runtime permission flow (API 31+) | `MainActivity.kt`, `AndroidManifest.xml` | 0.5d |
| 3b.9 | Supervision timeout handling + stale connection detection | `ble/BleHost.kt`, `ble/BleClient.kt` | 0.5d |
| 3b.10 | Multi-device testing (3+ phones, different OEMs, API 24-34) | *(manual)* | 1d |
| 3b.11 | Battery drain profiling (2-hour continuous game, measure mA) | *(manual)* | 0.5d |
| 3b.12 | BLE range testing (10m, 20m, through walls, interference) | *(manual)* | 0.5d |

**Deliverable**: BLE connection survives screen-off, app backgrounding,
brief radio dropouts, and player disconnect/reconnect. Heartbeat detects
stale connections within 20s. Reconnection restores full game state.

**Milestone check**: Disconnect a client mid-game, reconnect within 120s,
verify full state is restored and the game continues seamlessly.

---

## Phase 4a: UI — Core Screens (Week 7)

**Goal**: Navigable app with all 4 screens, basic interactivity.
No BLE — uses mock data / local `Controller` for development.

### Tasks

| # | Task | Files | Est. |
|---|---|---|---|
| 4a.1 | Set up Compose theme (dark, Material 3) | `ui/theme/Theme.kt`, `Color.kt`, `Type.kt` | 0.5d |
| 4a.2 | Set up `NavGraph` with 4 routes | `ui/navigation/NavGraph.kt` | 0.5d |
| 4a.3 | Implement `GameViewModel` with `StateFlow<GameState>` (local Controller, no BLE) | `model/GameViewModel.kt` | 1d |
| 4a.4 | Implement `HomeScreen` (Create/Join/Help buttons) | `ui/screens/HomeScreen.kt` | 0.5d |
| 4a.5 | Implement `LobbyScreen` (nickname, player grid, start) | `ui/screens/LobbyScreen.kt` | 1d |
| 4a.6 | Implement `CardView` composable | `ui/components/CardView.kt` | 1d |
| 4a.7 | Implement `HandRow` composable | `ui/components/HandRow.kt` | 0.5d |
| 4a.8 | Implement `PileView` composable | `ui/components/PileView.kt` | 0.5d |
| 4a.9 | Implement `DeckView` composable | `ui/components/DeckView.kt` | 0.5d |
| 4a.10 | Implement `PlayerRow` + `PlayerAvatar` | `ui/components/PlayerAvatar.kt` | 0.5d |
| 4a.11 | Implement `TurnIndicator` | `ui/components/TurnIndicator.kt` | 0.5d |
| 4a.12 | Implement `ChatPanel` (collapsible) | `ui/components/ChatPanel.kt` | 1d |
| 4a.13 | Implement `ActionBar` (Play/Pickup/Chat buttons) | `ui/components/ActionBar.kt` (inline in GameScreen) | 0.5d |
| 4a.14 | Implement `GameScreen` (compose all components) | `ui/screens/GameScreen.kt` | 1d |
| 4a.15 | Implement `GameOverScreen` | `ui/screens/GameOverScreen.kt` | 0.5d |

**Deliverable**: All 4 screens navigable with mock data. Cards render,
hand selection works, turn indicator updates, chat panel expands.
No BLE required — `GameViewModel` uses a local `Controller` instance.

**Milestone check**: Navigate Home → Lobby → Game → GameOver with mock
data. Select cards, "play" them (local state update), see pile update.

---

## Phase 4b: UI — BLE Wiring + AI Mode (Week 8)

**Goal**: Connect the UI to real BLE transport and enable AI opponents.

### Tasks

| # | Task | Files | Est. |
|---|---|---|---|
| 4b.1 | Wire ViewModel actions to BLE commands (replace local Controller) | `model/GameViewModel.kt` | 1d |
| 4b.2 | Wire BLE messages to state updates (parse notifications) | `model/GameViewModel.kt` | 1d |
| 4b.3 | Turn timer — client-side (30s countdown display) | `model/GameViewModel.kt` | 0.5d |
| 4b.4 | Turn timer — host-side (enforce 30s, auto-skip + notify) | `ble/BleHost.kt`, `game/Controller.kt` | 1d |
| 4b.5 | "Play vs AI" mode (HomeScreen button, AI fills empty slots) | `ui/screens/HomeScreen.kt`, `model/GameViewModel.kt` | 1d |
| 4b.6 | AI turn loop (host-side: AI players auto-play on their turn) | `model/GameViewModel.kt`, `game/AiPlayer.kt` | 1d |
| 4b.7 | Connection state UI (spinner, "reconnecting" overlay, error toasts) | `ui/components/ConnectionOverlay.kt` | 0.5d |
| 4b.8 | Integration test: full game over BLE (2 devices) | `test/integration/BleGameTest.kt` | 1d |

**Deliverable**: Full game playable on two phones over BLE. Create game on
phone A, join on phone B, deal, play cards, chat, game over. Single-player
mode: create a game, fill remaining slots with AI opponents, play a full
game against the AI.

**Milestone check**: Complete a 2-player Warlords game on two physical
phones. Complete a 1v5 AI game on a single phone.

---

## Phase 5: UI — Polish + Game Flow (Week 9)

**Goal**: Smooth animations, edge cases handled, good UX.

### Tasks

| # | Task | Est. |
|---|---|---|
| 5.1 | Card play/pickup animations (fade + slide) | 1d |
| 5.2 | Turn indicator pulse animation | 0.5d |
| 5.3 | Player turn glow animation | 0.5d |
| 5.4 | Game over screen animation (scale + fade) | 0.5d |
| 5.5 | Handle 3-6 player games (test with 3+ phones) | 1d |
| 5.6 | Handle player disconnect mid-game (skip turn, notify) | 1d |
| 5.6a | State persistence (save/restore game to SharedPreferences on disconnect, session recovery, multi-round score persistence) | 2-3d |
| 5.7 | Handle "no valid play" → force pickup | 0.5d |
| 5.8 | Handle deck exhaustion (reshuffle discard) | 0.5d |
| 5.9 | Card exchange UI (offer/take flow with confirm dialog) | 1d |
| 5.10 | Haptic feedback on card select/play | 0.5d |
| 5.11 | Sound effects (optional: card deal, play, win) | 0.5d |
| 5.12 | Landscape mode support (optional) | 1d |
| 5.13 | "How to Play" dialog with rules | 0.5d |

**Deliverable**: Polished, playable game with 2-6 players. All edge cases handled.

---

## Phase 6: Testing + Bug Fixing (Week 10)

**Goal**: Comprehensive test coverage, stable on multiple devices.

### Tasks

| # | Task | Est. |
|---|---|---|
| 6.1 | Complete unit test suite (target: 90%+ on game/ and protocol/) | 2d |
| 6.2 | BLE integration tests (loopback + two-device) | 1d |
| 6.3 | UI tests: Compose `createComposeRule` for each screen | 1d |
| 6.4 | Test on 3+ physical devices (different Android versions) | 1d |
| 6.5 | Battery drain test (2-hour continuous game) | 0.5d |
| 6.6 | BLE range test (10m, 20m, through walls) | 0.5d |
| 6.7 | Fix all bugs found in testing | 1d |
| 6.8 | Performance profiling (frame drops, memory leaks) | 0.5d |

**Deliverable**: Stable build with comprehensive test coverage.

---

## Phase 7: Beta + Release (Week 11-13)

**Goal**: Public release.

### Tasks

| # | Task | Est. |
|---|---|---|
| 7.1 | App icon + splash screen | 0.5d |
| 7.2 | App Store / Play Store listing (description, screenshots) | 1d |
| 7.3 | Play Store technical requirements (upload signing key, `.aab` bundle, data safety form, target API compliance) | 1d |
| 7.4 | Beta testing (5-10 users, 1 week) | 5d |
| 7.5 | Fix beta feedback | 2d |
| 7.6 | Final build + release | 0.5d |
| 7.7 | Post-release monitoring | ongoing |

---

## Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| `BluetoothGattServer` unreliable on some devices (dropped notifications, stuck connections, limited multi-client support) | High | High | Connection health checks, periodic re-subscription to notifications, cap at 3 clients on problematic OEMs, early hardware testing in Phase 3 |
| BLE MTU negotiation fails on some devices | Medium | High | Fallback to 23-byte MTU, increase chunk count |
| BLE connection drops frequently | Medium | High | Robust reconnect logic, game state persistence |
| 6-player game too slow over BLE | Low | Medium | Profile early (Phase 3), optimize JSON size |
| Compose performance with 13+ cards on screen | Low | Medium | LazyRow for hand, limit animations |
| Android BLE API differences across versions | Medium | Medium | Min SDK 24, test on API 24/28/31/34 |
| Host phone dies mid-game | Low | High | Save game state to SharedPreferences, clients can reconnect |

## Dependencies

```
Phase 1 ──► Phase 2 ──► Phase 3 ──► Phase 3b ──► Phase 4 ──► Phase 5 ──► Phase 6 ──► Phase 7
                                         │              │
                                         │              ├── Can start UI mockups in parallel
                                         │              │   (Phase 4 design work during Phase 3/3b)
                                         │              │
                                         │              └── BLE hardening needs 2+ physical devices
                                         │
                                         └── BLE testing needs 2+ physical devices
```

Phases 1-2 are sequential (protocol depends on game engine).
Phase 3 can start once protocol is defined (Phase 2).
Phase 3b is a buffer week for BLE hardware issues (OEM bugs, range, stability).
Phase 4 UI work can partially overlap with Phase 3/3b (design mockups).
Phases 5-7 are sequential.