# Overlapping card hand in Kotlin Compose

Use this guide to build overlapping card hands in Kotlin Compose, or to port a web card layout: fan layout, tap to select and lift, drag and drop between hand, pile and deck, and a hover pulse.

It is a port of a Svelte/CSS card table. It uses only runtime, foundation, animation and ui APIs, so it should suit Android and Desktop. **The code has not been compiled or run**: build it first and fix any version differences before extending it.

## Web to Compose mapping

| Web | Compose |
|---|---|
| Grid, each card `grid-column: 1+3i / 53+3i` | Custom `Layout` that computes one x step |
| `z-index: id` | Placement order (later is on top) |
| `.selected { translate(0,-1.1em) }` | Animated `Modifier.offset` (moves the hit area too) |
| `draggable`, `dragover`, `drop` | `DragState` + `DragHost` overlay |
| `preventDefault()` in `dragover` | The target's `accepts(payload)` |
| `@keyframes heartbeat` | `Animatable` with `keyframes` |

## Geometry

Each card spans 52 columns and starts 3 columns after the previous one, so there are `52 + 3(n-1)` columns. A column is the larger of `containerWidth / columns` and `cardWidth / 52`; the step is 3 columns. The hand therefore spreads as the container widens and tightens as cards are added. It is left aligned, with spare space at the right.

This approximates browser grid track sizing and ignores the 1em CSS margin. One deliberate change: the step is clamped so the last card never overflows.

## Rules

- Do not use `Row` with negative spacing or per-card padding. Use one custom `Layout`.
- Keep card state immutable or in snapshot state. Mutating a plain `card.selected` field will not recompose.
- Wrap each card in `key(card)` so gestures and animations follow the card, not the slot. Card ids must be unique.
- Put the lift `offset` before the drag and click modifiers.
- Only selected cards are draggable; dragging one drags all selected, in hand order.
- `pointerInput` captures stale lambdas: use `rememberUpdatedState`.
- Cancel the drag if its source leaves composition (the server may replace the hand mid-drag).
- Draw the dragged cards in a root overlay so no parent clips or covers them.
- Do hit testing in root coordinates.

## CardFan.kt

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val SPAN = 52
private const val STRIDE = 3

fun fanStep(count: Int, cardWidth: Float, available: Float): Float {
    if (count < 2) return 0f
    val minColumn = cardWidth / SPAN
    if (!available.isFinite()) return STRIDE * minColumn
    val column = max(minColumn, available / (SPAN + STRIDE * (count - 1)))
    val fit = max(0f, (available - cardWidth) / (count - 1))
    return min(STRIDE * column, fit)
}

@Composable
fun CardFan(
    cardWidth: Dp,
    cardHeight: Dp,
    lift: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val w = cardWidth.roundToPx()
        val h = cardHeight.roundToPx()
        val top = lift.roundToPx()
        val cards = measurables.map { it.measure(Constraints.fixed(w, h)) }
        val available =
            if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat()
            else Float.POSITIVE_INFINITY
        val step = fanStep(cards.size, w.toFloat(), available)
        val width =
            if (constraints.hasBoundedWidth) constraints.maxWidth
            else max(constraints.minWidth,
                if (cards.isEmpty()) 0 else (w + step * (cards.size - 1)).roundToInt())
        // headroom above the cards for the selected lift
        layout(width, constraints.constrainHeight(h + top)) {
            cards.forEachIndexed { i, card -> card.place((i * step).roundToInt(), top) }
        }
    }
}
```

## CardDrag.kt

```kotlin
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.round

sealed interface DragPayload {
    data class Hand(val cards: List<String>) : DragPayload
    object Deck : DragPayload
}

class DropTarget {
    var bounds: Rect = Rect.Zero
    var accepts: (DragPayload) -> Boolean = { false }
    var onDrop: (DragPayload) -> Unit = {}
}

@Stable
class DragState {
    var payload by mutableStateOf<DragPayload?>(null)
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    var grab by mutableStateOf(Offset.Zero)
        private set
    var preview by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set
    var hovered by mutableStateOf<DropTarget?>(null)
        private set

    private var owner: Any? = null
    private val targets = mutableListOf<DropTarget>()

    fun add(target: DropTarget) { targets.add(target) }

    fun remove(target: DropTarget) {
        targets.remove(target)
        if (hovered === target) hovered = null
    }

    fun start(source: Any, data: DragPayload, at: Offset, grabbed: Offset,
              image: @Composable () -> Unit) {
        if (owner != null) return // ignore a second finger
        owner = source
        payload = data
        grab = grabbed
        preview = image
        move(source, at)
    }

    fun move(source: Any, at: Offset) {
        if (owner !== source) return
        pointer = at
        val data = payload ?: return
        hovered = targets.firstOrNull { it.bounds.contains(at) && it.accepts(data) }
    }

    fun end(source: Any) {
        if (owner !== source) return
        val data = payload
        val target = hovered
        cancel(source)
        if (data != null && target != null && target.accepts(data)) target.onDrop(data)
    }

    fun cancel(source: Any) {
        if (owner !== source) return
        owner = null
        payload = null
        preview = null
        hovered = null
    }
}

private class CoordinatesRef { var value: LayoutCoordinates? = null }

@Composable
fun Modifier.cardDragSource(
    state: DragState,
    enabled: Boolean,
    payload: () -> DragPayload,
    preview: @Composable () -> Unit,
): Modifier {
    val source = remember { Any() }
    val ref = remember { CoordinatesRef() }
    val currentPayload by rememberUpdatedState(payload)
    val currentPreview by rememberUpdatedState(preview)
    DisposableEffect(state, enabled) { onDispose { state.cancel(source) } }
    val tracked = onGloballyPositioned { ref.value = it }
    if (!enabled) return tracked
    return tracked.pointerInput(state) {
        detectDragGestures(
            onDragStart = { local ->
                ref.value?.let {
                    state.start(source, currentPayload(), it.localToRoot(local), local, currentPreview)
                }
            },
            onDrag = { change, _ ->
                change.consume()
                ref.value?.let { state.move(source, it.localToRoot(change.position)) }
            },
            onDragEnd = { state.end(source) },
            onDragCancel = { state.cancel(source) },
        )
    }
}

@Composable
fun rememberDropTarget(
    state: DragState,
    accepts: (DragPayload) -> Boolean,
    onDrop: (DragPayload) -> Unit,
): DropTarget {
    val target = remember { DropTarget() }
    SideEffect {
        target.accepts = accepts
        target.onDrop = onDrop
    }
    DisposableEffect(state, target) {
        state.add(target)
        onDispose { state.remove(target) }
    }
    return target
}

fun Modifier.cardDropTarget(target: DropTarget): Modifier =
    onGloballyPositioned { target.bounds = it.boundsInRoot() }

@Composable
fun DragHost(state: DragState, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.onGloballyPositioned { origin = it.positionInRoot() }) {
        content()
        state.preview?.let { image ->
            Box(Modifier.offset { (state.pointer - origin - state.grab).round() }) { image() }
        }
    }
}
```

## CardTable.kt

`face` draws one card (an `Image`, say) and must apply the modifier it is given. The callbacks stand in for the websocket sends.

```kotlin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

class HandState {
    var cards by mutableStateOf(listOf<String>())
        private set
    var selected by mutableStateOf(setOf<String>())
        private set
    val selectedInOrder: List<String> get() = cards.filter { it in selected }

    fun deal(newCards: List<String>) {
        cards = newCards
        selected = emptySet()
    }

    fun toggle(card: String) {
        selected = if (card in selected) selected - card else selected + card
    }
}

@Composable
fun Modifier.heartbeat(active: Boolean, repeat: Boolean = false): Modifier {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(active, repeat) {
        if (!active) {
            scale.snapTo(1f)
            return@LaunchedEffect
        }
        do {
            scale.animateTo(1f, keyframes {
                durationMillis = 1300
                1f at 0
                1.03f at 182
                1f at 364
                1.02f at 546
                1f at 910
            })
        } while (repeat)
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

@Composable
fun CardHand(
    hand: HandState,
    drag: DragState,
    face: @Composable (String, Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val width = 120.dp
    val height = 174.dp
    val lift = 19.dp
    val previewGap = 24.dp

    CardFan(width, height, lift, modifier) {
        hand.cards.forEach { card ->
            key(card) {
                val selected = card in hand.selected
                val y by animateDpAsState(if (selected) -lift else 0.dp)
                val dragged = (drag.payload as? DragPayload.Hand)?.cards?.contains(card) == true
                val interaction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .offset { IntOffset(0, y.roundToPx()) }
                        .alpha(if (dragged) 0.4f else 1f)
                        .cardDragSource(
                            state = drag,
                            enabled = selected,
                            payload = { DragPayload.Hand(hand.selectedInOrder) },
                            preview = {
                                val cards = hand.selectedInOrder
                                val index = cards.indexOf(card).coerceAtLeast(0)
                                // keep the grabbed card under the pointer
                                Box(Modifier.offset(x = -previewGap * index.toFloat())) {
                                    cards.forEachIndexed { i, c ->
                                        face(c, Modifier.offset(x = previewGap * i.toFloat()).size(width, height))
                                    }
                                }
                            },
                        )
                        .clickable(interactionSource = interaction, indication = null) {
                            hand.toggle(card)
                        }
                ) {
                    face(card, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun CardTable(
    hand: HandState,
    pileTop: String?,
    face: @Composable (String, Modifier) -> Unit,
    back: @Composable (Modifier) -> Unit,
    onPutDown: (List<String>) -> Unit,
    onPickUp: () -> Unit,
) {
    val drag = remember { DragState() }
    val pile = rememberDropTarget(drag, { it is DragPayload.Hand }) {
        if (it is DragPayload.Hand) onPutDown(it.cards)
    }
    val handArea = rememberDropTarget(drag, { it === DragPayload.Deck }) { onPickUp() }
    val cardSize = Modifier.size(120.dp, 174.dp)

    DragHost(drag, Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(cardSize.cardDropTarget(pile).heartbeat(drag.hovered === pile)) {
                    if (pileTop != null) face(pileTop, Modifier.fillMaxSize())
                    else back(Modifier.fillMaxSize())
                }
                Box(
                    cardSize.cardDragSource(
                        state = drag,
                        enabled = true,
                        payload = { DragPayload.Deck },
                        preview = { back(cardSize) },
                    )
                ) { back(Modifier.fillMaxSize()) }
            }
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .cardDropTarget(handArea)
                    .heartbeat(drag.hovered === handArea)
            ) {
                CardHand(hand, drag, face, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }
        }
    }
}
```

## Extending

- Player avatars as targets: call `rememberDropTarget` per player, accepting `DragPayload.Hand` only for another, active player.
- Face-down hands: have `face` draw the back; ids stay the same.
- Dragging between separate windows or apps needs Compose's platform drag-and-drop APIs instead.

## Verify

1. 1, 5 and 20 cards, narrow and wide window: no overflow, later cards on top.
2. Tap raises a card; tapping it again lowers it.
3. Unselected cards do not drag; selected cards drag together.
4. Pile and hand pulse only for payloads they accept; dropping elsewhere does nothing.
5. Replace the hand during a drag: the preview disappears.
