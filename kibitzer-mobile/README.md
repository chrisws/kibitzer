# Kibitzer Mobile

![made with hali](.hali/badge.svg)
![unit tests](.hali/test_badge.svg)

Android card game playable over Bluetooth Low Energy (BLE).
Ported from the [kibitzer](../) web-based card game.

## Overview

A turn-based multiplayer card game for 2-6 players, supporting six games:
**Warlords** (President), **Free**, **Crazy Eights**, **Cheat**, **Old Maid**,
and **Go Fish**.
One phone acts as the **host** (GATT Server), running the game logic.
Other phones connect as **clients** (GATT Clients) over BLE.

A **rule-based AI opponent** (distilled expert system) is available for
single-player mode and automated testing. The AI is not an LLM — it is a
hand-coded set of per-game heuristics that makes valid, strategic moves.

## Architecture

- **Game logic**: Kotlin port of the C++ `cards`, `rules`, `controller` modules
  (Warlords and Free ported from `../server/`; Crazy Eights, Cheat, Old Maid,
  and Go Fish designed from standard rules)
- **Transport**: BLE GATT with custom service, chunked JSON messages
- **UI**: Jetpack Compose, touch-based card interactions
- **No internet required** — fully offline, peer-to-peer

## Documentation

| Document | Description |
|---|---|
| [docs/architecture.md](docs/architecture.md) | System architecture, components, data flow |
| [docs/ble-protocol.md](docs/ble-protocol.md) | BLE GATT service design, chunking, connection mgmt |
| [docs/message-protocol.md](docs/message-protocol.md) | Command/response protocol, JSON envelope format |
| [docs/game-logic.md](docs/game-logic.md) | Core types: Card, Hand, Deck, Rules, Player, Room, Controller |
| [docs/game-logic-rules.md](docs/game-logic-rules.md) | All 6 rule classes + CheatState |
| [docs/game-logic-handlers.md](docs/game-logic-handlers.md) | GameHandler, OldMaidHandler, GoFishHandler, Helpers |
| [docs/game-logic-ai.md](docs/game-logic-ai.md) | AiPlayer, strategies, AI tests, C++ differences |
| [docs/ui-design.md](docs/ui-design.md) | Screens, layouts, components, animations, accessibility |
| [docs/ui-design-state.md](docs/ui-design-state.md) | PlayerInfo, GameState, GameViewModel, ConnectionState |
| [docs/ui-design-help.md](docs/ui-design-help.md) | HelpScreen, game rules content, AboutScreen, navigation |
| [docs/implementation-plan.md](docs/implementation-plan.md) | Phased build plan with milestones |
| [docs/testing.md](docs/testing.md) | Unit, integration, and BLE testing strategy |

## Project Structure

```
kibitzer-mobile/
├── README.md
├── docs/
│   ├── architecture.md
│   ├── ble-protocol.md
│   ├── message-protocol.md
│   ├── game-logic.md
│   ├── ui-design.md
│   ├── implementation-plan.md
│   └── testing.md
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/kibitzer/mobile/
│       │   │   ├── KibitzerApp.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── ui/
│       │   │   │   ├── theme/
│       │   │   │   ├── screens/
│       │   │   │   │   ├── HomeScreen.kt
│       │   │   │   │   ├── LobbyScreen.kt
│       │   │   │   │   ├── GameScreen.kt
│       │   │   │   │   └── GameOverScreen.kt
│       │   │   │   ├── components/
│       │   │   │   │   ├── CardView.kt
│       │   │   │   │   ├── HandRow.kt
│       │   │   │   │   ├── PileView.kt
│       │   │   │   │   ├── DeckView.kt
│       │   │   │   │   ├── PlayerAvatar.kt
│       │   │   │   │   ├── TurnIndicator.kt
│       │   │   │   │   └── ChatPanel.kt
│       │   │   │   └── navigation/
│       │   │   │       └── NavGraph.kt
│       │   │   ├── game/
│       │   │   │   ├── Card.kt
│       │   │   │   ├── Hand.kt
│       │   │   │   ├── Deck.kt
│       │   │   │   ├── Rules.kt
│       │   │   │   ├── WarlordsRules.kt
│       │   │   │   ├── FreeRules.kt
│       │   │   │   ├── CrazyEightsRules.kt
│       │   │   │   ├── CheatRules.kt
│       │   │   │   ├── OldMaidRules.kt
│       │   │   │   ├── GoFishRules.kt
│       │   │   │   ├── Player.kt
│       │   │   │   ├── Room.kt
│       │   │   │   ├── Controller.kt
│       │   │   │   ├── AiPlayer.kt
│       │   │   │   ├── GameHandler.kt
│       │   │   │   ├── OldMaidHandler.kt
│       │   │   │   ├── GoFishHandler.kt
│       │   │   │   ├── CheatState.kt
│       │   │   │   ├── Game.kt
│       │   │   │   └── Helpers.kt
│       │   │   ├── ble/
│       │   │   │   ├── BleService.kt
│       │   │   │   ├── BleHost.kt
│       │   │   │   ├── BleClient.kt
│       │   │   │   ├── Chunker.kt
│       │   │   │   ├── GattConstants.kt
│       │   │   │   └── ConnectionMonitor.kt
│       │   │   ├── model/
│       │   │   │   ├── GameViewModel.kt
│       │   │   │   ├── ConnectionState.kt
│       │   │   │   ├── GameState.kt
│       │   │   │   └── GameConnection.kt
│       │   │   ├── service/
│       │   │   │   └── GameService.kt
│       │   │   └── protocol/
│       │   │       ├── Command.kt
│       │   │       └── Message.kt
│       │   └── res/
│       │       ├── drawable/
│       │       ├── layout/
│       │       └── values/
│       └── test/
│           └── java/com/kibitzer/mobile/
│               ├── game/
│               ├── protocol/
│               └── ble/
└── build.gradle.kts
```

## Tech Stack

- **Language**: Kotlin 2.0+
- **UI**: Jetpack Compose + Material 3
- **BLE**: Android `BluetoothLeScanner` / `BluetoothGatt`
- **State**: ViewModel + StateFlow
- **Serialization**: kotlinx.serialization (JSON)
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **R8/ProGuard**: Enabled for release builds; keep rules for `kotlinx.serialization`
  and BLE callback classes (see `app/proguard-rules.pro`)

## License

Same as kibitzer (see ../LICENSE).