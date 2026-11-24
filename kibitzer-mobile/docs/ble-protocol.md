# BLE GATT Protocol Design

## Service and Characteristics

### Service Label

```
Label:    0000-KIB-0000-1000-8000-00805F9B34FB  (Kibitzer Game Service)
```

This is a human-readable service label for discovery. The actual 128-bit
UUID (used in GATT operations) is defined in `GattConstants.kt`:

```kotlin
object GattConstants {
    val SERVICE_UUID   = UUID.fromString("6f5a3b2e-7c1d-4e8a-9b3f-2d4e6a8c0f1b")
    val CHAR_COMMAND   = UUID.fromString("6f5a3b2e-7c1d-4e8a-9b3f-2d4e6a8c0f2c")
    val CHAR_STATE     = UUID.fromString("6f5a3b2e-7c1d-4e8a-9b3f-2d4e6a8c0f3d")
    val CHAR_CHAT      = UUID.fromString("6f5a3b2e-7c1d-4e8a-9b3f-2d4e6a8c0f4e")
    val CHAR_HANDSHAKE = UUID.fromString("6f5a3b2e-7c1d-4e8a-9b3f-2d4e6a8c0f5f")
    val CHAR_PLAYERS   = UUID.fromString("6f5a3b2e-7c1d-4e8a-9b3f-2d4e6a8c0f6a")

    const val TARGET_MTU = 512
    const val MAX_PLAYERS = 6
}
```

### Characteristic Properties

| Characteristic | Properties | Direction |
|---|---|---|
| `CHAR_COMMAND` | Write, Write Response | Client → Host |
| `CHAR_STATE` | Notify | Host → Client |
| `CHAR_CHAT` | Notify | Host → Client |
| `CHAR_HANDSHAKE` | Read, Write, Notify | Both |
| `CHAR_PLAYERS` | Notify | Host → Client |

## Connection Lifecycle

### Host (GATT Server)

```
1. App starts → user taps "Create Game"
2. Host begins BLE advertising:
   - Name: "Kibitzer-<gameId>"  (gameId = 4 random hex chars)
   - Service UUID: SERVICE_UUID
   - Advertising interval: 100ms
3. Host waits for connections (up to MAX_PLAYERS - 1 = 5 clients)
4. On each connection:
   a. Assign sessionId (incrementing int, starting at 1)
   b. Send handshake: {"type":"welcome","sessionId":N,"maxPlayers":6}
   c. Add to Controller as lurker
   d. Notify all clients of updated player list
5. When game starts (all players dealt):
   a. Stop advertising
   b. Begin game loop
6. On client disconnect:
   a. Remove from Controller
   b. Notify remaining clients
   c. Skip disconnected player's turn
```

### Client (GATT Client)

```
1. App starts → user taps "Join Game"
2. Client scans for SERVICE_UUID (timeout: 10s)
3. On discovering host:
   a. Connect
   b. Discover services
   c. Set MTU to 512
   d. Subscribe to CHAR_STATE, CHAR_CHAT, CHAR_PLAYERS notifications
   e. Read CHAR_HANDSHAKE → receive sessionId
   f. Send handshake ack: {"type":"hello","sessionId":N,"nic":"PlayerN"}
4. Client is now in the lobby
5. On disconnect:
   a. Show "Connection lost" overlay
   b. Auto-retry scan+connect every 3s (max 5 attempts)
   c. After 5 failures, show "Host lost" screen
```

## Message Chunking Protocol

BLE MTU is typically 23 bytes (7 payload) by default, negotiable up to 517 (512 payload).
A full game state JSON can be 500-2000 bytes, so chunking is essential.

### Frame Format

Every chunk is a fixed-header frame:

```
┌────────┬────────┬────────┬─────────────────────────────────┐
│ magic  │ seq    │ total  │ payload (variable)              │
│ 2 bytes│ 1 byte │ 1 byte │ up to (MTU - 5) bytes           │
└────────┴────────┴────────┴─────────────────────────────────┘
```

| Field | Size | Description |
|---|---|---|
| `magic` | 2 bytes | `0x4B 0x42` ("KB") — identifies Kibitzer frames |
| `seq` | 1 byte | Chunk sequence number (0-based), max 255 chunks |
| `total` | 1 byte | Total number of chunks in this message |
| `payload` | 1..508 bytes | Message data (UTF-8 JSON fragment) |

### Chunker Implementation

```kotlin
class Chunker(private val maxChunkSize: Int = 508) {

    private val MAGIC = byteArrayOf(0x4B, 0x42)

    fun split(message: String): List<ByteArray> {
        val payload = message.toByteArray(Charsets.UTF_8)
        val totalChunks = (payload.size + maxChunkSize - 1) / maxChunkSize
        val chunks = mutableListOf<ByteArray>()

        for (i in 0 until totalChunks) {
            val start = i * maxChunkSize
            val end = minOf(start + maxChunkSize, payload.size)
            val chunkPayload = payload.copyOfRange(start, end)

            val chunk = ByteArray(4 + chunkPayload.size)
            System.arraycopy(MAGIC, 0, chunk, 0, 2)
            chunk[2] = i.toByte()
            chunk[3] = totalChunks.toByte()
            System.arraycopy(chunkPayload, 0, chunk, 4, chunkPayload.size)
            chunks.add(chunk)
        }
        return chunks
    }

    fun reassemble(chunks: List<ByteArray>): String {
        val total = (chunks.first()[3].toInt() and 0xFF)
        require(chunks.size == total) { "Expected $total chunks, got ${chunks.size}" }

        val sorted = chunks.sortedBy { it[2].toInt() and 0xFF }
        val payload = sorted.flatMap { it.copyOfRange(4, it.size) }.toByteArray()
        return String(payload, Charsets.UTF_8)
    }
}
```

With MTU 512: max payload per chunk = 508 bytes. Max message size = 255 × 508 = 129,540 bytes (more than enough).

### Reassembly Rules

- Receiver buffers chunks by `(seq, total)` 
- When all `total` chunks received → concatenate payload → decode UTF-8 → parse JSON
- Timeout: discard incomplete messages after 2 seconds
- Out-of-order chunks: buffer and wait (BLE GATT notifications are ordered per connection)

## Handshake Protocol

On connection, before any game commands:

```
Client → Host (CHAR_HANDSHAKE write):
  {"type":"hello","nic":"Alice","appVersion":"1.0.0"}

Host → Client (CHAR_HANDSHAKE notify):
  {"type":"welcome","sessionId":3,"gameId":"a3f2","maxPlayers":6,
   "players":[{"sessionId":1,"nic":"Bob","active":true},
              {"sessionId":2,"nic":"Carol","active":true}]}
```

The `sessionId` is the same concept as kibitzer's WebSocket session ID.
It's used in all subsequent commands for identity.

## Advertising

### Host Advertising Data

```
Local Name:  "Kibitzer-a3f2"
Service UUID: 0000KIB0-0000-1000-8000-00805F9B34FB
Manufacturer Data: 
  [0x4B, 0x42, 0x01]  // "KB" + protocol version
```

### Scan Filter (Client)

```kotlin
ScanFilter.Builder()
    .setServiceUuid(ParcelUuid(GattConstants.SERVICE_UUID))
    .build()
```

## Connection Limits and Behaviour

| Parameter | Value | Rationale |
|---|---|---|
| Max clients | 5 | 6 total players (1 host + 5 clients) |
| Advertising timeout | 120s | Stop advertising if no one joins |
| Scan timeout (client) | 10s | Give up scanning after 10s |
| Reconnect interval | 3s | Retry every 3 seconds |
| Max reconnect attempts | 5 | Then show error |
| Connection interval | Adaptive (15ms–2s) | Per game state, see Power Considerations |
| Command response timeout | 5s | If host doesn't respond, show error |

## BLE Reliability and Robustness

BLE connections are inherently unreliable — radio interference, device
movement, OS power management, and Android's aggressive background
restrictions can all cause silent disconnections. The following measures
ensure the game degrades gracefully and recovers automatically.

### GATT Server Callback Thread Marshalling

`BluetoothGattServer` callbacks (`onConnectionStateChange`,
`onCharacteristicWriteRequest`, `onCharacteristicReadRequest`, etc.)
arrive on a **background thread**. All UI updates and state mutations must
be marshalled to the main thread:

```kotlin
class BleHostService(private val context: Context) : BluetoothGattServerCallback {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val controller = GameController()

    override fun onConnectionStateChange(device: BluetoothDevice,
                                         status: Int, newState: Int) {
        // This runs on a background thread — marshal to main
        mainHandler.post {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> onClientConnected(device)
                BluetoothProfile.STATE_DISCONNECTED -> onClientDisconnected(device)
            }
        }
    }

    override fun onCharacteristicWriteRequest(device: BluetoothDevice,
                                               requestId: Int,
                                               characteristic: BluetoothGattCharacteristic,
                                               offset: Int, value: ByteArray) {
        mainHandler.post {
            handleCommand(device, characteristic, value)
        }
    }
}
```

**Rules:**
- Never touch `Controller`, `Hand`, or UI state from GATT callback threads.
- All `gattServer.notifyCharacteristicValue()` calls must also be on the
  main thread (or at least on a consistent single thread to avoid races).
- Use a single `Handler` instance for all marshalling — do not create
  per-callback handlers.

### Multi-Client Connection Enforcement

The host must handle the case where more than `MAX_PLAYERS - 1` clients
attempt to connect:

```kotlin
private fun onClientConnected(device: BluetoothDevice) {
    if (connectedClients.size >= GattConstants.MAX_PLAYERS - 1) {
        // Reject: send a "full" notification then disconnect
        val msg = """{"type":"error","code":"MAX_PLAYERS","msg":"Game is full"}"""
        device.notifyCharacteristicValue(CHAR_HANDSHAKE, msg.toByteArray())
        gattServer.cancelConnection(device)
        return
    }
    // ... normal connection setup
}
```

**Client-side handling:**
- On receiving `{"type":"error","code":"MAX_PLAYERS"}`, show a toast:
  "Game is full — try another."
- Do **not** enter the reconnect loop for this error code.
- Return to the "Join Game" scan screen.

### Runtime Permissions (API 31+)

Android 12 (API 31) replaced `BLUETOOTH_ADMIN` with two runtime
permissions:

| Permission | When needed | Rationale |
|---|---|---|
| `BLUETOOTH_SCAN` | Client: scanning for host | Location not required if `usesPermissionFlags="neverForLocation"` |
| `BLUETOOTH_CONNECT` | Both: connecting, reading, writing, notifications | Required for GATT operations |

**Manifest:**

```xml
<uses-permission android:name="android.permission.BLUETOOTH"
                 android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"
                 android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
                 android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

**Runtime flow (API 31+):**

```kotlin
fun requestBlePermissions(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val perms = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        if (perms.any { ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED }) {
            activity.requestPermissions(perms, REQ_BLE_PERMS)
        }
    }
}
```

- Request permissions **before** starting a scan or initiating a connection.
- If denied, show an explanation dialog and disable BLE features in the UI.
- `BLUETOOTH_SCAN` with `neverForLocation` flag means no location
  permission is needed (we filter by UUID, not proximity).

### Connection Health Check and Re-Subscription

BLE GATT notifications can silently stop without a disconnect event
(e.g., after a brief radio dropout that the OS doesn't report as a
disconnection). Implement a heartbeat mechanism:

```kotlin
class ConnectionMonitor(private val gatt: BluetoothGatt) {

    private val handler = Handler(Looper.getMainLooper())
    private var lastStateUpdate = SystemClock.elapsedRealtime()
    private var heartbeatInterval = 5_000L  // 5 seconds

    fun start() {
        handler.postDelayed(heartbeatRunnable, heartbeatInterval)
    }

    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            val elapsed = SystemClock.elapsedRealtime() - lastStateUpdate
            if (elapsed > heartbeatInterval * 2) {
                // No state update received — connection may be stale
                onStaleConnection()
            }
            handler.postDelayed(this, heartbeatInterval)
        }
    }

    fun onStateReceived() {
        lastStateUpdate = SystemClock.elapsedRealtime()
    }

    private fun onStaleConnection() {
        // Attempt re-subscription to notifications
        gatt.setCharacteristicNotification(CHAR_STATE, true)
        gatt.setCharacteristicNotification(CHAR_CHAT, true)
        gatt.setCharacteristicNotification(CHAR_PLAYERS, true)
        // If still no response after next interval, force reconnect
    }
}
```

**Host-side heartbeat:**
- The host sends a lightweight `{"id":"ping"}` notification every 10s
  to each connected client.
- If no `{"id":"pong"}` write is received within 20s, the host considers
  the client stale and removes them from the game (skip their turn).
- Clients respond to `ping` with a `pong` write to `CHAR_COMMAND`.

### GATT Server Reliability Measures

| Measure | Implementation |
|---|---|
| Connection state monitoring | `onConnectionStateChange` → update `connectedClients` set, notify UI |
| Automatic re-subscription | On stale detection, re-issue `setCharacteristicNotification` for all notify chars |
| Supervision timeout | Set to 6s (active) / 10s (idle). On timeout, OS fires disconnect — handle in `onConnectionStateChange` |
| Graceful degradation | If >2 clients disconnect in 30s, reduce `MAX_PLAYERS` to 4 and notify remaining players |
| Heartbeat/ping | 10s interval ping from host; 20s timeout for pong; stale clients removed |
| Write response verification | Use `WRITE_RESPONSE` for `CHAR_COMMAND` — if no response in 5s, retry once, then flag client |

**Graceful degradation logic:**

```kotlin
private fun onClientDisconnected(device: BluetoothDevice) {
    disconnectedRecently.add(SystemClock.elapsedRealtime() to device)
    // Prune entries older than 30s
    disconnectedRecently.removeAll { it.first < SystemClock.elapsedRealtime() - 30_000 }

    if (disconnectedRecently.size > 2) {
        // Unstable environment — reduce max players
        effectiveMaxPlayers = 4
        notifyAllPlayers("""{"id":"players","data":{"maxPlayers":4,"reason":"unstable"}}""")
    }
}
```

### BLE Reconnection Strategy

When a client loses connection, use **exponential backoff** with jitter:

```
Attempt 1: wait 1s  + random(0-500ms)
Attempt 2: wait 2s  + random(0-500ms)
Attempt 3: wait 4s  + random(0-500ms)
Attempt 4: wait 8s  + random(0-500ms)
Attempt 5: wait 16s + random(0-500ms)
→ Give up, show "Host lost" screen
```

**State restoration on reconnect:**

1. Client reconnects and completes handshake.
2. Host detects the `sessionId` was previously assigned (stored in
   `disconnectedSessions: Map<sessionId, PlayerState>`).
3. Host sends full redacted state for that player's perspective:
   ```json
   {"id":"state","data":{"hand":["AH","KD"],"table":[...],"turn":3}}
   ```
4. Client restores UI from the full state.
5. If the game has progressed (other players took turns), the client
   sees the updated table and their turn (if applicable).

**Host-side session persistence:**
- Keep `disconnectedSessions` for up to 120s after disconnect.
- If a client reconnects within 120s, restore their seat.
- After 120s, the seat is released and the player is removed from the game.

**Client-side state cache:**
- Cache the last known full state in memory (not disk — BLE reconnect
  is expected within seconds).
- On reconnect, compare cached `turn` number with server's current turn.
  If they differ, the server's full state takes precedence.

## Power Considerations

BLE LE is already low-power (~10-30mA active), but a card game is mostly
*waiting* — a turn can take 10-30 seconds. The radio doesn't need to poll at
maximum rate during idle periods. The following strategies minimise battery
drain without affecting gameplay responsiveness.

### Adaptive Connection Parameters

Negotiate BLE connection parameters per game state. The host initiates
`BluetoothGatt.requestConnectionPriority()` and
`BluetoothLeRemoteRpa` parameter updates:

| Game state | Connection interval | Slave latency | Supervision timeout |
|---|---|---|---|
| Your turn (active play) | 15ms | 0 | 6s |
| Waiting for other players | 100ms | 4 | 6s |
| Lobby (connected, not dealt) | 1000ms | 8 | 10s |
| Game over / idle | 2000ms | 8 | 10s |

**How it works:**
- When it's your turn, the client requests `CONNECTION_PRIORITY_HIGH`
  (short interval, no slave latency) for responsive card play.
- When it's someone else's turn, the client requests
  `CONNECTION_PRIORITY_BALANCED` (longer interval, slave latency 4).
- In the lobby or after game over, request `CONNECTION_PRIORITY_LOW`
  (1-2s interval, slave latency 8).
- The host applies the same logic for its own radio (it's a slave to
  each connected client in BLE terms).

**Expected savings:** 5-10x reduction in radio wake-ups during wait periods,
which is ~80% of game time.

### Slave Latency

Allow clients to skip connection events when they have nothing to send:

- **Your turn:** slave latency = 0 (must respond immediately)
- **Not your turn:** slave latency = 4-8 (can skip up to 8 connection events)
- **Lobby:** slave latency = 8

The host still sends notifications (turn changes, chat), which wake the client
regardless of slave latency. Slave latency only affects the client's *polling*.

### Data Minimisation (Delta Updates)

Instead of sending full redacted state after every action, use a two-tier
update strategy:

| Event | Message type | Size (approx) |
|---|---|---|
| Turn change | `{"id":"turn","data":{"sid":3}}` | ~30 bytes |
| Card played | `{"id":"play","data":{"sid":3,"cards":["QH"]}}` | ~45 bytes |
| Chat message | `{"id":"chat","data":{"sid":3,"msg":"hi"}}` | ~40 bytes |
| Player joined/left | `{"id":"players","data":[...]}` | ~100 bytes |
| Full state (join/reconnect only) | `{"id":"cards","data":{...}}` | 500-2000 bytes |

Full state is sent **only** on:
- Initial join (handshake)
- Reconnect after disconnect
- Explicit `state:` command (debug)

All other updates are small deltas. This reduces average message size from
~1000 bytes to ~40 bytes, cutting chunked transfers from 2-4 chunks to 1.

### Scan Optimisation (Client)

- Use `ScanSettings.SCAN_MODE_LOW_POWER` — the 10s scan window doesn't need
  aggressive scanning.
- Filter by service UUID (already specified) to ignore unrelated advertisements.
- Stop scanning immediately on first valid discovery + connect.
- During reconnect, use `SCAN_MODE_BALANCED` (slightly faster re-discovery).

### Screen and App Lifecycle

- **Auto-dim:** Reduce screen brightness to minimum during "waiting for turn"
  state. Restore on "your turn" notification.
- **Doze mode:** Allow the app to enter Android doze when backgrounded.
  The BLE GATT connection persists through doze (it's a hardware-level
  connection). Notifications still arrive and wake the app.
- **Wake lock:** Hold a partial wake lock only during active turn processing
  (card play, state update). Release immediately after.
- **No always-on scanning:** Scanning only occurs during "Join Game" (10s)
  or reconnect (3s × 5 attempts). Never in steady state.

### Expected Battery Life

| Scenario | Est. drain | 4000mAh battery |
|---|---|---|
| Active play (your turn, 30s rounds) | ~25mA avg | ~5 hours |
| Mostly waiting (6 players, 30s turns) | ~12mA avg | ~10 hours |
| Lobby + game over (idle) | ~5mA avg | ~25 hours |

A typical 6-player game with 30-second turns should last **6-8 hours** on a
full charge, well within a normal gaming session.

## Multi-Game Support

The host can only run one game at a time (single room, like kibitzer's default).
If the user wants a different game, they must end the current one first.
This keeps the BLE topology simple (star, single host).

Future: Could support multiple rooms by adding a room ID to the handshake,
but this adds complexity to the GATT service (multiple notification channels).
Not needed for v1.
