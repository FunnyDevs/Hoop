package app.hoop.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.runtime.remember
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
import app.hoop.designsystem.component.HoopMapIconButton
import app.hoop.designsystem.component.HoopPostCardDefaults
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.domain.post.FeedPost
import app.hoop.domain.post.PostType
import app.hoop.presentation.home.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

private object StorySpec {
    val panelHeight = 340.dp
    val panelTravel = 360.dp
    val panelCorner = 28.dp
    /** Spring of the expansion: damping ratio and stiffness taken from the prototype (k = 230, c = 24). */
    const val DAMPING = 0.79f
    const val STIFFNESS = 230f
    /** Dragging this many dp downwards closes the card completely. */
    val dragRange = 380.dp
    const val CLOSE_THRESHOLD = 0.72f
    val closeButtonTop = 48.dp
}

/**
 * A card that expands from where it sits in the carousel to full screen, with the replies below.
 * Dragging down shrinks it back. The expansion is a spring on a single progress value that interpolates the rectangle.
 */
@Composable
internal fun PostStory(
    post: FeedPost,
    startBounds: Rect,
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onClosed: () -> Unit,
    progress: Animatable<Float, AnimationVector1D>,
    autoExpand: Boolean,
    cardContent: @Composable (progress: Float) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val spec = remember { storySpring() }

    // When the card is opened by dragging, the finger drives the progress instead.
    LaunchedEffect(Unit) { if (autoExpand) progress.animateTo(1f, spec) }
    fun close() = scope.launch { progress.animateTo(0f, spec); onClosed() }
    HoopBackHandler(enabled = true) { close() }

    Box(Modifier.fillMaxSize().imePadding()) {
        val q = progress.value.coerceIn(0f, 1f)
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            val full = Rect(0f, 0f, with(density) { maxWidth.toPx() }, with(density) { maxHeight.toPx() })
            fun lerp(a: Float, b: Float) = a + (b - a) * q
            val left = lerp(startBounds.left, full.left)
            val top = lerp(startBounds.top, full.top)
            val width = lerp(startBounds.width, full.width)
            val height = lerp(startBounds.height, full.height)
            val dragPx = with(density) { StorySpec.dragRange.toPx() }
            Box(
                Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                    .size(with(density) { width.toDp() }, with(density) { height.toDp() })
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(HoopPostCardDefaults.CornerRadius * (1f - q)))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dy -> scope.launch { progress.snapTo((progress.value - dy / dragPx).coerceIn(0f, 1.05f)) } },
                            onDragEnd = { scope.launch { if (progress.value < StorySpec.CLOSE_THRESHOLD) close() else progress.animateTo(1f, spec) } },
                            onDragCancel = { scope.launch { progress.animateTo(1f, spec) } },
                        )
                    },
            ) {
                cardContent(q)
            }
        }
        HoopMapIconButton(
            HoopIcons.Close, stringResource(Res.string.story_close), { close() },
            Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 4.dp).alpha(q),
        )
        RepliesPanel(post, replyText, onReplyTextChange, onSend, WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(), Modifier.align(Alignment.BottomCenter).graphicsLayer { translationY = (1f - q) * with(density) { StorySpec.panelTravel.toPx() } })
    }
}

internal val StoryPanelHeight: Dp = StorySpec.panelHeight

/** Dragging a card this far upwards expands it completely. */
internal val StoryDragRange: Dp = StorySpec.dragRange

/** Fraction of the expansion past which releasing the finger completes it. */
internal const val STORY_OPEN_THRESHOLD = 0.3f

internal fun storySpring() = spring<Float>(dampingRatio = StorySpec.DAMPING, stiffness = StorySpec.STIFFNESS)

@Composable
private fun RepliesPanel(post: FeedPost, text: String, onTextChange: (String) -> Unit, onSend: () -> Unit, bottomInset: Dp, modifier: Modifier) {
    val colors = HoopTheme.colors
    val spacing = HoopTheme.spacing
    Column(
        modifier.fillMaxWidth().height(StorySpec.panelHeight + bottomInset)
            .background(colors.surface, androidx.compose.foundation.shape.RoundedCornerShape(topStart = StorySpec.panelCorner, topEnd = StorySpec.panelCorner))
            .padding(start = spacing.lg - 4.dp, end = spacing.lg - 4.dp, top = spacing.md + spacing.xs, bottom = spacing.md + bottomInset),
    ) {
        Text(repliesHeading(post).uppercase(), style = HoopTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        LazyColumn(Modifier.weight(1f).padding(top = spacing.xs)) {
            items(post.replies, key = { it.id }) { reply ->
                Row(Modifier.fillMaxWidth().padding(vertical = spacing.sm - 1.dp), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Box(Modifier.size(30.dp).background(colors.avatarSoft, HoopTheme.shapes.circle), contentAlignment = Alignment.Center) {
                        Text(initialsOf(reply.author.name), style = HoopTheme.typography.labelSmall, color = colors.onSurface)
                    }
                    Column {
                        Text(reply.author.name, style = HoopTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = colors.onSurface)
                        Text(reply.text, style = HoopTheme.typography.bodyMedium, color = colors.onSurface)
                    }
                }
            }
        }
        HoopInputBar(
            value = text, onValueChange = onTextChange,
            placeholder = stringResource(if (post.type == PostType.Question) Res.string.reply_placeholder else Res.string.comment_placeholder),
            sendLabel = stringResource(Res.string.reply_send), onSend = onSend, modifier = Modifier.padding(top = spacing.xs),
        )
    }
}
