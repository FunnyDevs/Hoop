package app.hoop.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.component.HoopBackHandler
import app.hoop.designsystem.component.HoopInputBar
import app.hoop.designsystem.component.HoopPageDots
import app.hoop.designsystem.component.rampOf
import app.hoop.designsystem.component.HoopPostCardDefaults
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.domain.post.FeedPost
import app.hoop.domain.post.PostType
import app.hoop.presentation.home.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private object StorySpec {
    val dotsBottom = 30.dp
    val DOTS_FADE = 0.7f to 1f
    /** Spring of the expansion: damping ratio and stiffness taken from the prototype (stiffness 120, damping 17). */
    const val DAMPING = 0.78f
    const val STIFFNESS = 120f
    /** Dragging this many dp downwards closes the card completely. */
    val dragRange = 380.dp
    const val CLOSE_THRESHOLD = 0.72f
    val closeButtonTop = 48.dp
    val cardsGap = 14.dp
    /** Progress from which the row counts as fully open. */
    const val OPEN = 0.999f
    /** How much a sideways fling carries over to the next page. */
    const val FLING_FACTOR = 0.2f
}

/**
 * The whole row of cards expands: every card grows from where it sits in the carousel to a full-screen page, side by side,
 * so once open you can swipe sideways between posts. Dragging down shrinks the row back. A single spring on [progress]
 * interpolates every rectangle; [pos] is the (fractional) index of the page shown.
 */
@Composable
internal fun PostStory(
    posts: List<FeedPost>,
    openId: String,
    startBounds: Rect,
    boundsOf: (String) -> Rect?,
    cardsStartPx: Float,
    onClosed: () -> Unit,
    onPageChanged: (String) -> Unit,
    progress: Animatable<Float, AnimationVector1D>,
    autoExpand: Boolean,
    holding: Boolean,
    cardContent: @Composable (post: FeedPost, progress: Float) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val spec = remember { storySpring() }
    val startIndex = remember { posts.indexOfFirst { it.id.value == openId }.coerceAtLeast(0) }
    val initial = remember { startBounds }
    val pos = remember { Animatable(startIndex.toFloat()) }
    // Where the cards sit in the carousel: card [base] has its left edge at [startLeft].
    var base by remember { mutableIntStateOf(startIndex) }
    // Until the carousel has been aligned on the shown post, the cards start from where the opened one was.
    var live by remember { mutableStateOf(false) }
    var verticalDrag by remember { mutableStateOf(false) }
    val holdingNow by rememberUpdatedState(holding)
    val openIdNow by rememberUpdatedState(openId)
    val onPageChangedNow by rememberUpdatedState(onPageChanged)
    val lastIndex = (posts.size - 1).coerceAtLeast(0)

    // When the card is opened by dragging, the finger drives the progress instead.
    LaunchedEffect(Unit) { if (autoExpand) progress.animateTo(1f, spec) }
    // Once fully open and released, the carousel is aligned on the shown post: that is where closing goes back to.
    LaunchedEffect(Unit) {
        snapshotFlow { progress.value >= StorySpec.OPEN && !holdingNow && !verticalDrag && !pos.isRunning }.collect { settled ->
            if (!settled) return@collect
            val page = pos.value.roundToInt().coerceIn(0, lastIndex)
            base = page
            live = true
            posts.getOrNull(page)?.let { if (it.id.value != openIdNow) onPageChangedNow(it.id.value) }
        }
    }
    fun close() = scope.launch { progress.animateTo(0f, spec); onClosed() }
    HoopBackHandler(enabled = true) { close() }

    Box(Modifier.fillMaxSize().imePadding()) {
        val q = progress.value.coerceIn(0f, 1f)
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenW = with(density) { maxWidth.toPx() }
            val screenH = with(density) { maxHeight.toPx() }
            val dragPx = with(density) { StorySpec.dragRange.toPx() }
            val cardW = with(density) { HoopPostCardDefaults.CarouselWidth.toPx() }
            val cardH = with(density) { HoopPostCardDefaults.CarouselHeight.toPx() }
            val gap = with(density) { StorySpec.cardsGap.toPx() }
            fun lerp(a: Float, b: Float) = a + (b - a) * q
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { verticalDrag = true },
                            onVerticalDrag = { _, dy -> scope.launch { progress.snapTo((progress.value - dy / dragPx).coerceIn(0f, 1.05f)) } },
                            onDragEnd = {
                                verticalDrag = false
                                scope.launch { if (progress.value < StorySpec.CLOSE_THRESHOLD) close() else progress.animateTo(1f, spec) }
                            },
                            onDragCancel = { verticalDrag = false; scope.launch { progress.animateTo(1f, spec) } },
                        )
                    }
                    .draggable(
                        rememberDraggableState { dx -> scope.launch { pos.snapTo((pos.value - dx / screenW).coerceIn(0f, lastIndex.toFloat())) } },
                        Orientation.Horizontal,
                        enabled = q >= StorySpec.OPEN,
                        onDragStopped = { velocity ->
                            scope.launch {
                                // A fling carries at most to the neighbouring page.
                                val target = (pos.value - velocity / screenW * StorySpec.FLING_FACTOR).roundToInt()
                                val low = floor(pos.value).toInt().coerceIn(0, lastIndex)
                                val high = ceil(pos.value).toInt().coerceIn(0, lastIndex)
                                pos.animateTo(target.coerceIn(low, high).toFloat(), spec)
                            }
                        },
                    ),
            ) {
                // Once aligned, the carousel's own layout is the target (it may be clamped at the end of the list).
                val anchorLeft = if (live) boundsOf(posts[base.coerceIn(0, lastIndex)].id.value)?.left ?: cardsStartPx else initial.left
                posts.forEachIndexed { index, post ->
                    val left = lerp(anchorLeft + (index - base) * (cardW + gap), (index - pos.value) * screenW)
                    val width = lerp(cardW, screenW)
                    if (left + width < 0f || left > screenW) return@forEachIndexed
                    val shape = androidx.compose.foundation.shape.RoundedCornerShape(HoopPostCardDefaults.CornerRadius * (1f - q))
                    Box(
                        Modifier.offset { IntOffset(left.roundToInt(), lerp(initial.top, 0f).roundToInt()) }
                            .size(with(density) { width.toDp() }, with(density) { lerp(cardH, screenH).toDp() })
                            .shadow(HoopTheme.elevation.card * (1f - q), shape)
                            .clip(shape),
                    ) { cardContent(post, q) }
                }
            }
        }
        // Carousel dots near the bottom, appearing at the end of the opening.
        val shownIndex = pos.value.roundToInt().coerceIn(0, lastIndex)
        HoopPageDots(
            posts.size, shownIndex,
            Modifier.align(Alignment.BottomCenter).padding(bottom = StorySpec.dotsBottom + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()).alpha(rampOf(q, StorySpec.DOTS_FADE)),
            activeColor = if (posts[shownIndex].type == PostType.Question) HoopTheme.colors.onSurface else HoopTheme.colors.onScrim,
            inactiveColor = if (posts[shownIndex].type == PostType.Question) HoopTheme.colors.outlineStrong else HoopTheme.colors.onScrim.copy(alpha = 0.45f),
        )
    }
}

/** Dragging a card this far upwards expands it completely. */
internal val StoryDragRange: Dp = StorySpec.dragRange

/** Fraction of the expansion past which releasing the finger completes it. */
internal const val STORY_OPEN_THRESHOLD = 0.3f

internal fun storySpring() = spring<Float>(dampingRatio = StorySpec.DAMPING, stiffness = StorySpec.STIFFNESS)
