# UI Design — Help & Rules Screens

> Part of the Kibitzer Mobile UI documentation.
> See `ui-design.md` for screens and components,
> and `ui-design-state.md` for state management.

## HelpScreen

Accessible from the HomeScreen "Help" button.
Also accessible via a `?` icon in the top-right of LobbyScreen and
GameScreen (opens as a full-screen overlay, not a dialog — rules can be
long).

Navigation: `NavHost` gains a `help` route. Per-game rules use
`help/{game}` sub-routes.

```
┌─────────────────────────┐
│  ←  Help               │
├─────────────────────────┤
│                         │
│  ┌───────────────────┐  │
│  │ 📡  Connection     │  │  ← BLE setup & troubleshooting
│  └───────────────────┘  │
│                         │
│  ┌───────────────────┐  │
│  │ 🃏  Warlords       │  │
│  │ 🃏  Free           │  │
│  │ 🃏  Crazy Eights   │  │
│  │ 🃏  Cheat          │  │
│  │ 🃏  Old Maid       │  │
│  │ 🃏  Go Fish        │  │
│  └───────────────────┘  │
│                         │
│  ┌───────────────────┐  │
│  │ ℹ️  About          │  │
│  └───────────────────┘  │
│                         │
└─────────────────────────┘
```

### Compose Sketch

```kotlin
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onConnectionHelp: () -> Unit,
    onGameRules: (Game) -> Unit,
    onAbout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Help", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(48.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                HelpCard(icon = "📡", title = "Connection", subtitle = "BLE setup & troubleshooting",
                         onClick = onConnectionHelp)
            }
            item {
                Text("Games", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                     modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            }
            items(Game.entries) { game ->
                HelpCard(icon = "🃏", title = game.displayName, subtitle = game.tagline,
                         onClick = { onGameRules(game) })
            }
            item {
                HelpCard(icon = "ℹ️", title = "About", subtitle = "Version & credits",
                         onClick = onAbout)
            }
        }
    }
}

@Composable
fun HelpCard(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 28.sp)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

---

## Connection Help Screen (`help/connection`)

User-level BLE guidance. No technical GATT details — just what the user
needs to know to connect and troubleshoot.

```
┌─────────────────────────┐
│  ←  Connection Help     │
├─────────────────────────┤
│                         │
│  **Getting Started**    │
│  • Both phones need     │
│    Bluetooth enabled    │
│  • Grant "Nearby        │
│    devices" permission   │
│    when prompted        │
│  • Stay within ~10 m    │
│                         │
│  **Creating a Game**    │
│  1. Tap "Create Game"   │
│  2. Your phone          │
│     advertises as        │
│     "Kibitzer-XXXX"     │
│  3. Friends tap         │
│     "Join Game" and      │
│     select your phone    │
│  4. Add AI players to    │
│     fill empty slots     │
│  5. Tap "Start Game"     │
│                         │
│  **Joining a Game**     │
│  1. Tap "Join Game"     │
│  2. Wait for the scan    │
│     (up to 10 s)         │
│  3. Tap the host's       │
│     phone name           │
│  4. You'll appear in     │
│     the lobby            │
│                         │
│  **Troubleshooting**    │
│  • Can't see host?       │
│    → Check Bluetooth is   │
│      on, host is          │
│      advertising          │
│  • Connection drops?      │
│    → Move closer,         │
│      avoid Wi-Fi 5 GHz    │
│      interference         │
│  • Slow responses?        │
│    → Close other BLE       │
│      apps, restart        │
│      Bluetooth            │
│  • "Permission denied"?   │
│    → Settings → Apps →    │
│      Kibitzer →           │
│      Permissions →        │
│      Nearby devices →     │
│      Allow                │
│                         │
└─────────────────────────┘
```

### Compose Sketch

```kotlin
@Composable
fun ConnectionHelpScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        HelpTopBar(title = "Connection Help", onBack = onBack)

        HelpSection("Getting Started") {
            HelpBullet("Both phones need Bluetooth enabled")
            HelpBullet("Grant \"Nearby devices\" permission when prompted")
            HelpBullet("Stay within ~10 m of each other")
        }

        HelpSection("Creating a Game") {
            HelpStep(1, "Tap \"Create Game\"")
            HelpStep(2, "Your phone advertises as \"Kibitzer-XXXX\"")
            HelpStep(3, "Friends tap \"Join Game\" and select your phone")
            HelpStep(4, "Add AI players to fill empty slots")
            HelpStep(5, "Tap \"Start Game\"")
        }

        HelpSection("Joining a Game") {
            HelpStep(1, "Tap \"Join Game\"")
            HelpStep(2, "Wait for the scan (up to 10 s)")
            HelpStep(3, "Tap the host's phone name")
            HelpStep(4, "You'll appear in the lobby")
        }

        HelpSection("Troubleshooting") {
            HelpTrouble("Can't see host?", "Check Bluetooth is on, host is advertising")
            HelpTrouble("Connection drops?", "Move closer, avoid Wi-Fi 5 GHz interference")
            HelpTrouble("Slow responses?", "Close other BLE apps, restart Bluetooth")
            HelpTrouble("\"Permission denied\"?", "Settings → Apps → Kibitzer → Permissions → Nearby devices → Allow")
        }
    }
}
```

### Helper Composables

```kotlin
@Composable
fun HelpTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
fun HelpSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Column(content = content)
    }
}

@Composable
fun HelpBullet(text: String) {
    Row(modifier = Modifier.padding(start = 8.dp, vertical = 2.dp)) {
        Text("• ", fontSize = 14.sp)
        Text(text, fontSize = 14.sp)
    }
}

@Composable
fun HelpStep(num: Int, text: String) {
    Row(modifier = Modifier.padding(start = 8.dp, vertical = 2.dp)) {
        Text("$num. ", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(text, fontSize = 14.sp)
    }
}

@Composable
fun HelpTrouble(problem: String, fix: String) {
    Column(modifier = Modifier.padding(start = 8.dp, vertical = 4.dp)) {
        Text("• $problem", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text("  → $fix", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
```

---

## Game Rules Screens (`help/{game}`)

Each game gets a dedicated rules screen. Content is written for a
**first-time player** — assumes no prior knowledge. Structure is
consistent across all six:

1. **Objective** (1–2 sentences)
2. **Setup** (deck, dealing)
3. **Turn** (what you can do)
4. **Winning / Losing**
5. **Tips** (optional, 1–3 bullets)

### Navigation

```kotlin
// In NavHost:
composable("help") { HelpScreen(...) }
composable("help/connection") { ConnectionHelpScreen(...) }
composable("help/{game}") { backStackEntry ->
    val game = Game.fromName(backStackEntry.arguments?.getString("game"))
    GameRulesScreen(game = game, onBack = { navController.popBackStack() })
}
composable("help/about") { AboutScreen(...) }
```

### GameRulesScreen (generic shell)

```kotlin
@Composable
fun GameRulesScreen(game: Game, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        HelpTopBar(title = game.displayName, onBack = onBack)

        HelpSection("Objective") {
            Text(game.objective, fontSize = 14.sp)
        }
        HelpSection("Setup") {
            game.setup.forEach { HelpBullet(it) }
        }
        HelpSection("Your Turn") {
            game.turnRules.forEach { HelpStep(it.index, it.text) }
        }
        HelpSection("Winning") {
            Text(game.winCondition, fontSize = 14.sp)
        }
        if (game.tips.isNotEmpty()) {
            HelpSection("Tips") {
                game.tips.forEach { HelpBullet(it) }
            }
        }
    }
}
```

### Game Rules Content

Rules content lives in a `GameRules` data object (or a `when` block in a
companion object). Written in plain language, no jargon.

```kotlin
data class RuleStep(val index: Int, val text: String)

data class GameRules(
    val objective: String,
    val setup: List<String>,
    val turnRules: List<RuleStep>,
    val winCondition: String,
    val tips: List<String> = emptyList()
)
```

#### Warlords

```kotlin
GameRules(
    objective = "Get rid of all your cards first. The last player with cards loses.",
    setup = listOf(
        "52-card deck, dealt evenly (7–8 cards each for 6–7 players)",
        "One card is placed face-up on the pile to start"
    ),
    turnRules = listOf(
        RuleStep(1, "Play one or more cards that match the rank of the top pile card"),
        RuleStep(2, "If you can't match, pick up 1 card from the deck"),
        RuleStep(3, "If you pick up and now have a matching card, you may play it")
    ),
    winCondition = "First player to empty their hand wins. If you're the last with cards, you lose.",
    tips = listOf(
        "Holding multiple matching ranks lets you play them all at once",
        "Picking up strategically can set you up for a big play next turn"
    )
)
```

#### Free

```kotlin
GameRules(
    objective = "Get rid of all your cards. No matching required — play anything.",
    setup = listOf(
        "52-card deck, dealt evenly",
        "One card face-up on the pile to start"
    ),
    turnRules = listOf(
        RuleStep(1, "Play any card(s) from your hand to the pile"),
        RuleStep(2, "If you have no cards to play, pick up 1 from the deck"),
        RuleStep(3, "After picking up, you may play a card from your hand")
    ),
    winCondition = "First to empty their hand wins.",
    tips = listOf(
        "Dump your biggest piles early — you can play any number of cards",
        "Save single cards for when you're down to your last few"
    )
)
```

#### Crazy Eights

```kotlin
GameRules(
    objective = "Get rid of all your cards. Match the rank OR the suit of the top card.",
    setup = listOf(
        "52-card deck, dealt 5 cards each",
        "One card face-up on the pile to start"
    ),
    turnRules = listOf(
        RuleStep(1, "Play a card matching the rank OR suit of the top pile card"),
        RuleStep(2, "Playing an 8 lets you change the active suit"),
        RuleStep(3, "If you can't play, pick up 1 card from the deck"),
        RuleStep(4, "After picking up, you may play a card if it matches")
    ),
    winCondition = "First to empty their hand wins.",
    tips = listOf(
        "Save your 8s — they're your escape hatch when stuck",
        "Watch the active suit: if it's hearts, any heart is playable"
    )
)
```

#### Cheat

```kotlin
GameRules(
    objective = "Get rid of all your cards by playing them in rank order (A→2→3→…→K→A). You can lie about what you're playing.",
    setup = listOf(
        "52-card deck, dealt evenly",
        "Play starts with Aces. Each turn, the current rank advances (A→2→3→…→K→A)"
    ),
    turnRules = listOf(
        RuleStep(1, "Bid: declare how many cards of the current rank you're playing"),
        RuleStep(2, "Play that many cards face-down to the discard pile"),
        RuleStep(3, "Any opponent can call \"cheat\" before the next turn"),
        RuleStep(4, "If called: your played cards are revealed. If you lied, you pick up the whole discard. If you told the truth, the caller picks it up")
    ),
    winCondition = "First to empty their hand wins. Picking up a huge discard can end your game.",
    tips = listOf(
        "Bidding 0 is legal — just say you have none and play nothing",
        "If you're called and lose, the discard can be 20+ cards. Bid conservatively early on"
    )
)
```

#### Old Maid

```kotlin
GameRules(
    objective = "Get rid of all your cards by finding pairs. The player left with the Old Maid (the unmatched card) loses.",
    setup = listOf(
        "49 cards: 24 pairs + 1 Old Maid (the unmatched card)",
        "Dealt evenly; you may have an extra card"
    ),
    turnRules = listOf(
        RuleStep(1, "Look through your hand for pairs — discard any you find"),
        RuleStep(2, "Ask another player for a card of a rank you hold"),
        RuleStep(3, "If they have it, you take it (and may draw again from their hand). If not, you draw 1 from their hand face-down"),
        RuleStep(4, "Check for new pairs after each draw")
    ),
    winCondition = "Last player holding the Old Maid loses. Everyone else is safe.",
    tips = listOf(
        "Memorize what players discard — it tells you what they DON'T have",
        "Ask for ranks you already hold: a hit gives you a pair immediately"
    )
)
```

#### Go Fish

```kotlin
GameRules(
    objective = "Collect all four cards of a rank (a \"book\"). First to complete all books wins.",
    setup = listOf(
        "52-card deck, dealt 5–7 cards each (remainder is the draw pile)",
        "No face-up pile — you draw from the central pile"
    ),
    turnRules = listOf(
        RuleStep(1, "Ask another player: \"Do you have a [rank]?\""),
        RuleStep(2, "If they have it, you take ALL cards of that rank from their hand"),
        RuleStep(3, "If you complete a book (4 of a rank), discard it and go again"),
        RuleStep(4, "If they don't have it, they say \"Go Fish\" and you draw 1 from the pile"),
        RuleStep(5, "If the pile is empty, the game ends immediately")
    ),
    winCondition = "First to complete all 13 books wins. If the pile runs out, the player with the most completed books wins.",
    tips = listOf(
        "Track what's been played — if three 7s are gone, the fourth is in the pile",
        "Asking for a rank you hold 3 of guarantees a book on a hit"
    )
)
```

---

## About Screen (`help/about`)

Standard app info. Kept minimal — version, build, credits, license.

```
┌─────────────────────────┐
│  ←  About               │
├─────────────────────────┤
│                         │
│         🃏              │
│       KIBITZER          │
│                         │
│  Version: 1.0.0 (42)    │
│  Build: 2025-07-11      │
│                         │
│  ─────────────────────  │
│                         │
│  **Credits**            │
│  • Design & Code:       │
│    Chris W. S.          │
│  • Card art:            │
│    Unicode symbols      │
│  • Inspired by:         │
│    kibitzer (C++/X11)   │
│                         │
│  ─────────────────────  │
│                         │
│  **License**            │
│  MIT License            │
│  See LICENSE for        │
│  full text.             │
│                         │
│  ─────────────────────  │
│                         │
│  **Links**              │
│  • GitHub: [link]       │
│  • Report a bug: [link] │
│                         │
└─────────────────────────┘
```

### Compose Sketch

```kotlin
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HelpTopBar(title = "About", onBack = onBack)

        Spacer(Modifier.height(16.dp))
        Text("🃏", fontSize = 48.sp)
        Text("KIBITZER", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // Version info (from BuildConfig)
        Text("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
             fontSize = 14.sp)
        Text("Build: ${BuildConfig.BUILD_DATE}", fontSize = 14.sp)

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()

        HelpSection("Credits") {
            HelpBullet("Design & Code: Chris W. S.")
            HelpBullet("Card art: Unicode symbols")
            HelpBullet("Inspired by: kibitzer (C++/X11)")
        }

        HelpSection("License") {
            Text("MIT License", fontSize = 14.sp)
            Text("See LICENSE for full text.", fontSize = 13.sp,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        HelpSection("Links") {
            HelpLink("GitHub", "https://github.com/chrisws/kibitzer")
            HelpLink("Report a bug", "https://github.com/chrisws/kibitzer/issues")
        }
    }
}

@Composable
fun HelpLink(label: String, url: String) {
    Text(
        text = "$label: $url",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(vertical = 2.dp)
            .clickable {
                // Open in browser via Intent
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                // context.startActivity(intent)
            }
    )
}
```

---

## Navigation Update

The `NavHost` now has 7 routes:

```kotlin
NavHost(navController, startDestination = "home") {
    composable("home")       { HomeScreen(...) }
    composable("lobby")      { LobbyScreen(...) }
    composable("game")       { GameScreen(...) }
    composable("gameover")   { GameOverScreen(...) }
    composable("help")       { HelpScreen(...) }
    composable("help/connection") { ConnectionHelpScreen(...) }
    composable("help/{game}") { GameRulesScreen(...) }
    composable("help/about") { AboutScreen(...) }
}
```

### HomeScreen Change

The "How to Play" `TextButton` is renamed to **"Help"** and navigates
to the `help` route:

```kotlin
TextButton(onClick = { navController.navigate("help") }) {
    Text("Help")
}
```

### In-Game Access

A `?` icon button in the top-right of the GameScreen and LobbyScreen
top bars navigates to `help` (preserving the game state on the back stack):

```kotlin
// In GameScreen top bar:
IconButton(onClick = { navController.navigate("help") }) {
    Icon(Icons.Filled.Help, contentDescription = "Help")
}
```

Popping the back stack returns to the game with all state intact
(the `GameViewModel` is scoped to the `activityViewModels` or a
shared `ViewModelStoreOwner` so it survives navigation).