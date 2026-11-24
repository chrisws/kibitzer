# System Architecture

## High-Level Overview

```
┌─────────────────────────────────────────────────────────┐
│                    HOST PHONE                            │
│                                                         │
│  ┌───────────┐    ┌──────────────┐    ┌─────────────┐  │
│  │  UI Layer │◄──►│  Game Engine │◄──►│  BLE Host   │  │
│  │ (Compose) │    │ (Controller) │    │ (GATT Srv)  │  │
│  └───────────┘    └──────────────┘    └──────┬──────┘  │
│       │                  │                   │          │
│       ▼                  ▼                   │          │
│  ViewModel         Rules / Cards             │          │
│  StateFlow         Deck / Hand               │          │
└──────────────────────────────────────────────┼──────────┘
                                               │
                     BLE GATT (star topology)   │
                                               │
        ┌──────────────────────────────────────┼──────────┐
        │                  │                   │          │
   ┌────┴─────┐     ┌─────┴────┐       ┌─────┴────┐      │
   │ Client 1 │     │ Client 2 │       │ Client N │      │
   │ (Phone)  │     │ (Phone)  │       │ (Phone)  │      │
   └──────────┘     └──────────┘       └──────────┘      │
```

## Components

### 1. Game Engine (transport-agnostic)

Pure Kotlin classes with no Android or BLE dependencies. Direct port of the C++
modules in `../server/`, which serve as the authoritative reference.

| Class | Source (C++) | Responsibility |
|---|---|---|
| `Card` | `cards.h` enum | 52-card enum + name mapping |
| `Hand` | `cards.h/cpp` | Ordered list of cards, add/remove/sort/has |
| `Deck` | `cards.h/cpp` | Pack + discard pile, deal/pickup/putdown/shuffle |
| `Rules` | `rules.h/cpp` | Abstract game rules interface |
| `WarlordsRules` | `rules.cpp` (struct `Warlords`) | Warlords and Scumbags implementation |
| `FreeRules` | `rules.cpp` (struct `RulesFree`) | Free-play implementation |
| `CrazyEightsRules` | *(new — no C++ reference)* | Crazy Eights (wild 8s, direction) |
| `CheatRules` | *(new — no C++ reference)* | Cheat (bidding, calling, stealing) |
| `OldMaidRules` | *(new — no C++ reference)* | Old Maid (pair matching, elimination) |
| `GoFishRules` | *(new — no C++ reference)* | Go Fish (asking, hitting, going fish) |
| `Player` | `controller.h` | Player state: hand, nickname, slot, room, points |
| `Room` | `controller.h` | Room state: deck, slots, turn, rules, save state |
| `Controller` | `controller.cpp` | Game orchestration: sessions, turns, redaction, game selection |
| `AiPlayer` | *(new — distilled expert system)* | Rule-based AI opponent for all 6 games |

**Key design principle**: The `Controller` exposes a `handle(command: String): Message` method.
It knows nothing about BLE, WebSocket, or any transport. The transport layer calls it
and distributes the response.

### 2. BLE Transport Layer

| Class | Responsibility |
|---|---|
| `GattConstants` | Service/characteristic UUIDs, MTU size |
| `BleHost` | GATT Server: advertises, accepts connections, relays messages |
| `BleClient` | GATT Client: scans, connects, sends commands, receives state |
| `Chunker` | Splits/reassembles messages that exceed BLE MTU |
| `BleService` | Android `BluetoothLeScanner` / `BluetoothGatt` wrappers |

**Topology**: Star. One host, up to 5 clients (6 players total).
The host runs the `Controller`. Clients send commands and receive redacted state.

### 3. Connection Abstraction

The `GameViewModel` does not directly reference `BleClient` or `BleHost`.
Instead, it depends on a `GameConnection` interface:

```kotlin
interface GameConnection {
    fun send(command: String)
    fun onMessage: Flow<Message>
    fun onConnectionChange: Flow<ConnectionState>
    fun disconnect()
}
```

Implementations:
- `BleConnection` — wraps `BleClient`/`BleHost` for multiplayer
- `LocalConnection` — in-process loopback for AI/single-player mode
- `FakeConnection` — for unit tests

This keeps the ViewModel testable and decoupled from transport.

### 4. Foreground Service (Host)

The host runs a `GameService` (foreground service) while a game is active:

```kotlin
class GameService : Service() {
    override fun onStartCommand(...): Int {
        startForeground(NOTIF_ID, buildNotification())
        return START_STICKY
    }
}
```

- Started when the host creates a game, stopped when the game ends or host disconnects.
- Shows a persistent notification: "Kibitzer — Game in progress (3/6 players)".
- Prevents Android from killing the host app when the screen turns off.
- Declared in `AndroidManifest.xml` with `android:foregroundServiceType="connectedDevice"`.

### 5. UI Layer (Jetpack Compose)

| Screen | Purpose |
|---|---|
| `HomeScreen` | Create game (host) or join game (client) |
| `LobbyScreen` | See connected players, set nickname, start game |
| `GameScreen` | Main gameplay: hand, pile, deck, players, chat |
| `GameOverScreen` | Round results, scores, play again |

**State management**: Single `GameViewModel` with `StateFlow<GameState>`.
UI observes state and renders. User actions dispatch commands through the BLE layer.

### 6. Protocol Layer

| Class | Responsibility |
|---|---|
| `Command` | Enum of command types (DEAL, PUTDOWN, PICKUP, etc.) |
| `Message` | Parsed message with type + data payload |

## Data Flow

### Client sends a command

```
User taps "Play" on GameScreen
  → GameViewModel.playSelectedCards()
    → BleClient.sendCommand("putd:[\"QH\",\"QD\"]")
      → Chunker.split(payload)
        → BluetoothGatt.writeCharacteristic(COMMAND, chunk)
          → [BLE] → BleHost.onCharacteristicWrite()
            → Chunker.reassemble(chunks)
              → Controller.handle("putd:[\"QH\",\"QD\"]", sessionId)
                → returns Message
                  → BleHost.broadcast(message, excludeSessionId)
                    → [BLE notify] → each client's BleClient.onNotification()
                      → GameViewModel.updateState(parsedMessage)
                        → Compose re-renders
```

### Host sends state update

```
Controller produces Message (e.g., after a play)
  → BleHost.broadcast(message)
    → for each connected client:
      → redacted = Controller.redact(clientSessionId, message)
      → Chunker.split(redacted.toJson())
      → BluetoothGatt.notifyCharacteristic(STATE, chunk)
```

## Threading Model

| Thread | What runs |
|---|---|
| Main (UI) | Compose rendering, ViewModel state updates |
| GATT Server (host) | `BluetoothLeServer` callbacks arrive on a **binder thread** (not main). `BleHost` must dispatch to main via `Handler(Looper.getMainLooper())` before calling `Controller.handle()`. |
| GATT Client | `BluetoothGatt` callbacks arrive on a binder thread. `BleClient` dispatches to main via `Handler` before emitting to `Flow`. |
| Game logic | Synchronous on **main thread** (fast, no I/O). All `Controller.handle()` calls happen on main. |
| Host turn timer | Coroutine on main scope: tracks turn start, enforces 30s timeout, auto-skip |

**Key rule**: All `Controller` access is serialized on the main thread. BLE
callbacks arrive on binder threads and are dispatched to main before touching
game state. This eliminates the need for locks or synchronization.

The host turn timer is a lightweight coroutine (`viewModelScope.launch`) that
`delay(1000)` in a loop, checking elapsed time against the 30s limit. When the
limit is reached with no valid command received, the host calls
`Controller.handle("skip:", turnSessionId)` and broadcasts the result.

### Controller Ownership

The `Controller` instance is owned by `GameService` (the foreground service),
**not** by `GameViewModel`. This ensures:
- The game engine survives configuration changes (rotation, etc.)
- The `ViewModel` can be recreated without losing game state
- The service lifecycle matches the game lifecycle

`GameViewModel` accesses the `Controller` via a `GameEngine` singleton or
by binding to the `GameService`:

```kotlin
// GameService holds the Controller
class GameService : Service() {
    val controller = Controller()
    val bleHost = BleHost(controller)
    // ...
}

// GameViewModel binds to GameService
class GameViewModel : ViewModel() {
    private var service: GameService? = null
    // access controller via service?.controller
}
```

## Error Handling

| Scenario | Handling |
|---|---|
| Client disconnects mid-game | Host marks player inactive, skips their turn, notifies others |
| BLE connection lost | Client shows "reconnecting" overlay, retries scan+connect |
| Invalid command from client | Controller returns error message, client shows toast |
| MTU too small for message | Chunker handles transparently |
| Host app killed | All clients detect disconnect, show "host lost" screen |

## Security

- BLE pairing (Just Works) for connection auth
- App-level session token exchanged on connect (random 128-bit)
- No encryption beyond BLE link-layer (sufficient for local card game)
- No internet access required or used