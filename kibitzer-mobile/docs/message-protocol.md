# Message Protocol

## Overview

The message protocol is a direct adaptation of kibitzer's WebSocket command protocol.
Commands are short text strings with a 5-character prefix. Responses are JSON envelopes.
The same protocol works over BLE GATT (chunked) as it did over WebSocket.

## Command Format

```
<5-char-prefix><payload>
```

| Prefix | Command | Payload | Description |
|---|---|---|---|
| `init:` | INIT | *(empty)* | Request initial game state |
| `join:` | JOIN | `<slot>` | Join game slot (0-5) |
| `nicn:` | NICKNAME | `<name>` | Set player nickname |
| `shuf:` | SHUFFLE | *(empty)* | Shuffle deck, reset game |
| `deal:` | DEAL | *(empty)* | Deal cards to this player |
| `picu:` | PICKUP | `<count>` | Pick up N cards from deck (default 1) |
| `putd:` | PUTDOWN | `<json-array>` | Play cards to discard pile |
| `exch:` | EXCHANGE | `<Y\|Q>:<toId>:<json-array>` | Exchange/offer cards |
| `skip:` | SKIP | *(empty)* | Skip current turn |
| `chat:` | CHAT | `<text>` | Send chat message |
| `game:` | GAME | `<gamename>` | Select game mode (e.g., `warlords`, `old_maid`) |
| `bid:` | BID | `<count>` | Submit Cheat bid (number of cards to play) |
| `cheat:` | CHEAT | `<targetId>` | Call "Cheat" on the previous player's play |
| `room:` | ROOM | `<room#>` | Change room (not used in v1) |

### Examples

```
init:
join:2
nicn:Alice
shuf:
deal:
picu:1
putd:["QH","QD"]
exch:Q:2:["3C","3D"]
exch:Y:2:["3C","3D"]
skip:
chat:hello everyone!
game:old_maid
bid:3
cheat:2
```

### Exchange Timeout

When a player sends an exchange offer (`exch:Q:<toId>:<cards>`), the recipient
has **15 seconds** to respond with `exch:Y:` (accept) or `exch:N:` (reject).
If no response arrives within the window:

1. The host automatically cancels the exchange.
2. The offered cards return to the offering player's hand.
3. A `message` response is broadcast: `"(Bob) did not respond — exchange cancelled"`.
4. The turn advances to the next player.

The 15-second timer is enforced host-side (same mechanism as the turn timer).
The client displays a countdown on the exchange confirm dialog.

## Response Envelope

All responses use the same JSON envelope as kibitzer:

```json
{
  "id": "<message-type>",
  "data": { ... }
}
```

### Message Types

| `id` value | When sent | `data` contents |
|---|---|---|
| `init` | Response to `init:` | `sessionId`, `players[]`, `welcome` |
| `players` | Player joined/left/nick changed | `message`, `players[]`, `turn`, optional `dealId`, `clearHandId`, `game` |
| `cards` | After deal/pickup/putdown/skip | `pile[]`, `message`, `turn`, `faceDown`, `game`, optional `hand[]`, `sessionId` |
| `exchange` | Card exchange offer | `fromId`, `from`, `toId`, `hand[]`, `message` |
| `cheat` | Cheat challenge state | `callerId`, `targetId`, `cards[]`, `result` (`"liar"` \| `"truthful"`), `message` |
| `message` | Chat or simple notification | plain text string |
| `shuffle` | Deck shuffled | `"ready"` |
| `exit` | Player disconnected | `id`, `players[]`, `name` |

### Example: `cards` response

```json
{
  "id": "cards",
  "data": {
    "pile": ["3C", "3D", "QH"],
    "message": "(Alice) played QH QD",
    "turn": 2,
    "faceDown": false,
    "game": "Warlords and Scumbags",
    "hand": ["4C", "5D", "7H", "9S", "JS", "KD", "AS"],
    "sessionId": 1
  }
}
```

### Example: `players` response

```json
{
  "id": "players",
  "data": {
    "message": "(Bob) has joined the game",
    "players": [
      {"sessionId": 1, "active": true, "nic": "Alice"},
      {"sessionId": 2, "active": true, "nic": "Bob"},
      {"sessionId": -1, "active": false, "nic": "empty 3"},
      {"sessionId": -1, "active": false, "nic": "empty 4"},
      {"sessionId": -1, "active": false, "nic": "empty 5"},
      {"sessionId": -1, "active": false, "nic": "empty 6"}
    ],
    "turn": -1
  }
}
```

## Redaction

The `Controller.redact(sessionId, message)` method works exactly as in kibitzer:
- For `cards` messages: each player only sees their own `hand` field
- Other players' hands are omitted from the JSON
- `pile`, `turn`, `message` are visible to all

This is critical for game integrity — a client must never see another player's cards.

## Kotlin Protocol Classes

```kotlin
// protocol/Command.kt
enum class Command(val prefix: String) {
    INIT("init:"),
    JOIN("join:"),
    NICKNAME("nicn:"),
    SHUFFLE("shuf:"),
    DEAL("deal:"),
    PICKUP("picu:"),
    PUTDOWN("putd:"),
    EXCHANGE("exch:"),
    SKIP("skip:"),
    CHAT("chat:"),
    GAME("game:"),
    BID("bid:"),
    ASK("ask:"),
    CHEAT("cheat:"),
    ROOM("room:");

    companion object {
        fun fromPrefix(prefix: String): Command? =
            entries.find { it.prefix == prefix }
    }
}

// protocol/Message.kt
@Serializable
data class Message(
    val id: String,
    val data: JsonElement
) {
    companion object {
        fun fromJson(json: String): Message =
            json.decodeFromString(Message.serializer())

        fun toJson(msg: Message): String =
            json.encodeToString(Message.serializer(), msg)
    }
}
```

> **Note:** A separate `Envelope` class is not needed. `Message` serves as
> both the parsed representation and the serialization target. The `data`
> field uses `JsonElement` to preserve the raw structure without requiring
> a separate data class for every message type. This keeps the protocol
> flexible while avoiding a parallel deserialization path.

## BLE Transport Mapping

| Protocol element | BLE characteristic |
|---|---|
| Commands (client → host) | `CHAR_COMMAND` (write) |
| State updates (host → client) | `CHAR_STATE` (notify) |
| Chat messages (host → client) | `CHAR_CHAT` (notify) |
| Handshake (both) | `CHAR_HANDSHAKE` (read/write/notify) |
| Player list (host → client) | `CHAR_PLAYERS` (notify) |

All payloads are UTF-8 JSON, chunked per the frame format in `ble-protocol.md`.

## Version Compatibility

During the BLE handshake, the client sends its `appVersion` in the `hello`
message. The host checks compatibility before accepting:

```
Client → Host: {"type":"hello","nic":"Alice","appVersion":"1.0.0"}
Host → Client: {"type":"welcome","sessionId":3,...}
  OR
Host → Client: {"type":"reject","reason":"version","minVersion":"1.0.0","yourVersion":"0.9.0"}
```

**Rules:**
- The host maintains a `MIN_COMPATIBLE_VERSION` constant (e.g., `"1.0.0"`).
- If the client's `appVersion` is below the minimum, the host sends a
  `reject` message and does not assign a session.
- The client displays: *"Your app version (0.9.0) is too old. Please update
  to at least 1.0.0."*
- Version comparison uses semantic versioning (major.minor.patch).
- Future: the host may also send its own version in `welcome` so clients
  can warn about missing features.

This prevents protocol mismatches when one player has an older app build.

## Error Handling

Invalid commands return a `message` type response:

```json
{"id": "message", "data": "invalid play!"}
```

The client displays this as a toast or in the chat panel.
No special error codes needed — the human-readable message is sufficient.