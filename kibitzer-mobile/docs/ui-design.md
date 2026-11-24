# UI Design — Jetpack Compose

> **Documentation split:** This file contains screens and components. See:
> - [`ui-design-state.md`](ui-design-state.md) — PlayerInfo, GameState, GameViewModel, ConnectionState
> - [`ui-design-help.md`](ui-design-help.md) — HelpScreen, ConnectionHelp, GameRulesScreen, AboutScreen, Navigation

## Design Principles

- **Portrait orientation** (phone held in one or two hands)
- **Touch-first**: tap to select, swipe to play — no drag-and-drop
- **Minimal chrome**: cards are the focus, UI elements are subtle
- **Dark theme** by default (card games look good on dark backgrounds)
- **Material 3** components with custom card styling

## Screen Flow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  HomeScreen  │────►│ LobbyScreen  │────►│  GameScreen  │────►│ GameOverScreen│
│              │     │              │     │              │     │              │
│ Create/Join  │     │ Players,     │     │ Hand, Pile,  │     │ Scores,      │
│              │     │ Nickname,    │     │ Deck, Chat   │     │ Play Again   │
└──────────────┘     │ Start        │     └──────────────┘     └──────────────┘
                     └──────────────┘            │
                                                 │ (round ends)
                                                 ▼
                                          GameOverScreen
```

Navigation: `NavHost` with 4 routes: `home`, `lobby`, `game`, `gameover`.

---

## HomeScreen

```
┌─────────────────────────┐
│                         │
│      🃏 KIBITZER        │
│                         │
│  ┌───────────────────┐  │
│  │   Create Game     │  │  ← host: BLE + AI players
│  └───────────────────┘  │
│                         │
│  ┌───────────────────┐  │
│  │    Join Game      │  │  ← scans for BLE host
│  └───────────────────┘  │
│                         │
│  ┌───────────────────┐  │
│  │     How to Play   │  │  ← shows rules
│  └───────────────────┘  │
│                         │
└─────────────────────────┘
```

- **Create Game**: Starts BLE advertising (optional) and transitions to
  LobbyScreen. The host can fill any empty slot with an AI player (🤖 button)
  or wait for BLE clients to join. Supports any mix: 6 AI, 1 BLE + 5 AI,
  3 BLE + 3 AI, etc. If no BLE clients join, the game is fully local.
- **Join Game**: Starts BLE scan, shows spinner, transitions to LobbyScreen on connect
- **How to Play**: Dialog with Warlords rules summary

### Compose Sketch

```kotlin
@Composable
fun HomeScreen(
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onHelp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🃏 KIBITZER", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(48.dp))
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
            Text("Create Game")
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onJoin, modifier = Modifier.fillMaxWidth()) {
            Text("Join Game")
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onHelp) {
            Text("How to Play")
        }
    }
}
```

---

## LobbyScreen

```
┌─────────────────────────┐
│  Kibitzer [a3f2]        │
│─────────────────────────│
│                         │
│  Nickname: [Alice    ]  │
│                         │
│  Game: [Warlords    ▼]  │  ← game selector (host only)
│                         │
│  Players:               │
│  ┌────┐ ┌────┐ ┌────┐  │
│  │ 🧑 │ │ 🧑 │ │ 🤖 │  │
│  │Ali │ │Bob │ │AI-3│  │
│  └────┘ └────┘ └────┘  │
│  ┌────┐ ┌────┐ ┌────┐  │
│  │ 🤖 │ │ 🤖 │ │ +  │  │  ← tap "+" to add AI
│  │AI-4│ │AI-5│ │    │  │
│  └────┘ └────┘ └────┘  │
│                         │
│  ┌───────────────────┐  │
│  │    Start Game     │  │  ← enabled when 2+ players
│  └───────────────────┘  │
│                         │
│  [Leave]                │
└─────────────────────────┘
```

- Nickname input (max 12 chars, sent as `nicn:` command)
- **Game selector** (dropdown/`ExposedDropdownMenu`): host chooses from
  Warlords, Free, Crazy Eights, Cheat, Old Maid, Go Fish.
  Clients see the selected game as read-only text.
  Selection is stored in `GameState.gameType` and sent to the
  `Controller` via `setGame()` before dealing.
- Player grid (2×3): avatars with names.
  - **BLE players**: shown with 🧑 icon + nickname
  - **AI players**: shown with 🤖 icon + "AI-N" name (host can rename)
  - **Empty slots**: shown dimmed with a "+" button (host only).
    Tapping "+" fills the slot with an AI player.
  - **Remove AI**: tapping an AI player's avatar (host only) shows a
    confirm dialog: "Remove AI-3?" → frees the slot.
- Start button: enabled when ≥2 players joined (any mix of AI + BLE),
  sends `shuf:` then `deal:`
- Leave button: disconnects BLE (if connected), returns to Home

---

## GameScreen (Main Gameplay)

This is the most complex screen. Layout is a vertical stack:

```
┌─────────────────────────┐
│ Turn: (Bob)    ⏱ 28s   │  ← TurnIndicator (top bar)
├─────────────────────────┤
│                         │
│      ┌─────────┐       │
│      │  PILE   │       │  ← PileView (top card visible)
│      │  QH     │       │     with count badge
│      │ (12)    │       │
│      └─────────┘       │
│                         │
│      ┌─────────┐       │
│      │  DECK   │       │  ← DeckView (card back)
│      │  🂠     │       │     tappable to pick up
│      │ (34)    │       │
│      └─────────┘       │
│                         │
├─────────────────────────┤
│                         │
│  ┌──┐┌──┐┌──┐┌──┐┌──┐  │
│  │4C││5D││7H││9S││JS│  │  ← HandRow (fanned cards)
│  └──┘└──┘└──┘└──┘└──┘  │     tap to select/deselect
│                         │
├─────────────────────────┤
│ ┌────┐┌────┐┌────┐     │
│ │ 🧑 ││ 🧑 ││ 🧑 │     │  ← Player row (small avatars)
│ │Ali ││Bob*││Car │     │     * = current turn (glowing)
│ └────┘└────┘└────┘     │
├─────────────────────────┤
│ [Play] [Pick] [Chat]    │  ← Action bar
├─────────────────────────┤
│ > hello!                │  ← ChatPanel (collapsible)
│ > Bob played QH QD      │
│ > Alice picked up 1     │
│ [type message...] [Send]│
└─────────────────────────┘
```

### Components

#### TurnIndicator (top bar)

```kotlin
@Composable
fun TurnIndicator(
    currentPlayer: String,
    isMyTurn: Boolean,
    secondsRemaining: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isMyTurn) Color(0xFF2E7D32) else Color(0xFF424242))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (isMyTurn) "Your turn!" else "Turn: $currentPlayer",
            color = Color.White,
            fontSize = 16.sp
        )
        Text(
            text = "⏱ ${secondsRemaining}s",
            color = Color.White,
            fontSize = 14.sp
        )
    }
}
```

- Green background when it's your turn, grey otherwise
- Countdown timer (30s, same as kibitzer)
- Auto-skip when timer hits 0

#### PileView

```kotlin
@Composable
fun PileView(
    topCard: Card?,
    count: Int,
    onDragToHand: () -> Unit  // not used, tap instead
) {
    Box(
        modifier = Modifier
            .size(120.dp, 168.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B5E20))
            .border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(12.dp))
    ) {
        if (topCard != null) {
            CardImage(card = topCard, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFC107))
            ) {
                Text("$count", color = Color.Black, fontSize = 14.sp,
                     modifier = Modifier.align(Alignment.Center))
            }
        } else {
            Text("Empty", color = Color.White.copy(alpha = 0.5),
                 modifier = Modifier.align(Alignment.Center))
        }
    }
}
```

#### DeckView

```kotlin
@Composable
fun DeckView(
    count: Int,
    isMyTurn: Boolean,
    onPickup: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .size(120.dp, 168.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D47A1))
            .border(2.dp, if (isMyTurn) Color(0xFF2196F3) else Color(0xFF1565C0),
                     RoundedCornerShape(12.dp))
            .clickable { onPickup(1) }
    ) {
        Text("🂠", fontSize = 48.sp, modifier = Modifier.align(Alignment.Center))
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF2196F3))
        ) {
            Text("$count", color = Color.White, fontSize = 14.sp,
                 modifier = Modifier.align(Alignment.Center))
        }
    }
}
```

- Glowing border when it's your turn
- Tap = pick up 1 card
- Long-press = pick up multiple (shows count picker)

#### HandRow

```kotlin
@Composable
fun HandRow(
    cards: List<Card>,
    selected: Set<Card>,
    faceDown: Boolean,
    onCardClick: (Card) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(cards, key = { it.name }) { card ->
            CardView(
                card = card,
                isSelected = card in selected,
                faceDown = faceDown,
                onClick = { onCardClick(card) },
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .offset(y = if (card in selected) (-12).dp else 0.dp)
            )
        }
    }
}
```

- Cards fan out horizontally
- Selected cards lift up 12dp
- Tap to toggle selection
- Face-down mode: show card backs (for games that hide hands)

#### CardView

```kotlin
@Composable
fun CardView(
    card: Card,
    isSelected: Boolean,
    faceDown: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp, 100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (faceDown) Color(0xFF1565C0)
                else when (card.name[1]) {
                    'H', 'D' -> Color(0xFFC62828)
                    else -> Color(0xFF212121)
                }
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFFFFC107)
                        else Color(0xFF757575),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        if (!faceDown) {
            Column(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(card.name[0].toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold,
                     color = if (card.name[1] in "HD") Color(0xFFEF5350) else Color.White)
                Spacer(Modifier.weight(1f))
                Text(suitSymbol(card.name[1]), fontSize = 24.sp,
                     color = if (card.name[1] in "HD") Color(0xFFEF5350) else Color.White)
            }
        } else {
            Text("🂠", fontSize = 32.sp, modifier = Modifier.align(Alignment.Center))
        }
    }
}

fun suitSymbol(suit: Char): String = when (suit) {
    'H' -> "♥"; 'D' -> "♦"; 'C' -> "♣"; 'S' -> "♠"; else -> "?"
}
```

#### PlayerRow

```kotlin
@Composable
fun PlayerRow(
    players: List<PlayerInfo>,
    mySessionId: Int,
    turnSessionId: Int,
    onPlayerPoke: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        players.forEach { player ->
            PlayerAvatar(
                name = player.nic,
                isMe = player.sessionId == mySessionId,
                isTurn = player.sessionId == turnSessionId,
                isActive = player.active,
                onClick = { onPlayerPoke(player.sessionId) },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
```

- Small circular avatars (48dp)
- Current turn player has a glowing **heartbeat** border
- **Opacity levels**: `me` = 1.0, `other active` = 0.5, `inactive` = 0.1
- **Player poke**: tapping another player's avatar sends a `skip:` command
- "Me" has a subtle highlight ring

#### ChatPanel

```kotlin
@Composable
fun ChatPanel(
    messages: List<String>,
    onSend: (String) -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Chat", fontSize = 14.sp)
            Text(if (expanded) "▼" else "▲")
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                messages.forEach { msg ->
                    Text("> $msg", fontSize = 12.sp, modifier = Modifier.padding(4.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    singleLine = true
                )
                IconButton(onClick = { onSend(chatInput); chatInput = "" }) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}
```

- Collapsible (tap header to expand/collapse)
- Shows last 50 messages (capped via circular buffer)
- Auto-scrolls to bottom on new message

#### Action Bar

```kotlin
@Composable
fun ActionBar(
    isMyTurn: Boolean,
    hasSelection: Boolean,
    handEmpty: Boolean,
    onPlay: () -> Unit,
    onPickup: () -> Unit,
    onDeal: () -> Unit,
    onChat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        if (handEmpty) {
            Button(onClick = onDeal, enabled = isMyTurn, modifier = Modifier.weight(1f)) {
                Text("Deal")
            }
        } else {
            Button(onClick = onPlay, enabled = isMyTurn && hasSelection, modifier = Modifier.weight(1f)) {
                Text("Play")
            }
        }
        Button(onClick = onPickup, enabled = isMyTurn, modifier = Modifier.weight(1f)) {
            Text("Pick Up")
        }
        OutlinedButton(onClick = onChat, modifier = Modifier.weight(1f)) {
            Text("Chat")
        }
    }
}
```

- **Play**: enabled when it's your turn AND cards are selected
- **Deal**: shown when hand is empty (replaces Play button); sends `deal:`
- **Pick Up**: enabled when it's your turn
- **Chat**: always enabled

---

## GameOverScreen

```
┌─────────────────────────┐
│                         │
│      🏆 GAME OVER       │
│                         │
│  (Alice) won!           │
│                         │
│  Scores:                │
│  ┌────┐ ┌────┐         │
│  │Ali │ │Bob │         │
│  │ 3  │ │ 1  │         │
│  └────┘ └────┘         │
│  ┌────┐ ┌────┐         │
│  │Car │ │Dan │         │
│  │ 0  │ │ 2  │         │
│  └────┘ └────┘         │
│                         │
│  ┌───────────────────┐  │
│  │   Play Again      │  │
│  └───────────────────┘  │
│  ┌───────────────────┐  │
│  │     Leave Game    │  │
│  └───────────────────┘  │
└─────────────────────────┘
```

- Winner announcement
- Score grid (all players)
- Play Again: shuffles and re-deals
- Leave: disconnects, returns to Home

---

## Animations

| Element | Animation | Trigger |
|---|---|---|
| Card selection | Lift up 12dp + gold border | Tap |
| Turn indicator | **Heartbeat** (scale 1.0→1.03→1.0) | Every 1.3s when my turn |
| Card play | Fade out from hand → appear on pile | Play action |
| Card pickup | Fade in from deck → appear in hand | Pickup action |
| Player turn glow | **Heartbeat** border (blue→white→blue) | Turn change |
| Game over | Scale up + fade in | Round end |
| Chat expand | Slide down (200dp) | Toggle |

All animations use `animateDpAsState` / `animateColorAsState` from Compose.

> **Host-side turn timer:** The client-side countdown is display-only.
> The **host** enforces the 30-second limit: if no `putd:`/`picu:`/`skip:`
> command arrives within the window, the host automatically issues a
> `skip:` for that player and broadcasts the turn advance. The client
> timer is a UX aid; the host timer is authoritative.

## Accessibility

- All cards have `contentDescription` ("Queen of Hearts", etc.)
- Turn indicator announced via `LiveRegion`
- Chat messages announced
- Minimum touch target: 48dp
- Color contrast: WCAG AA minimum