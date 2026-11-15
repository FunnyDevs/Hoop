package app.hoop.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hoop.designsystem.component.HoopInputBar
import app.hoop.designsystem.component.rampOf
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.domain.post.FeedPost
import app.hoop.domain.post.PostType
import app.hoop.domain.post.Reply
import app.hoop.presentation.home.resources.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max
import kotlin.math.roundToInt

private object CommentsSpec {
    // Opening: one spring for the whole sheet; dragging between the two heights uses a second one.
    const val OPEN_DAMPING = 0.78f
    const val OPEN_STIFFNESS = 130f
    const val POSITION_DAMPING = 0.767f
    const val POSITION_STIFFNESS = 170f

    // Heights as fractions of the screen height (the design is 844 dp tall): top of the sheet.
    const val MID_TOP = 324f / 844f
    const val UP_TOP = 100f / 844f
    const val QUESTION_TOP = 96f / 844f
    /** A drag this far (fraction of the screen height) moves the sheet between its two positions. */
    const val DRAG_RANGE = 0.265f
    /** Position below which releasing the finger closes the sheet. */
    const val CLOSE_BELOW = -0.25f
    /** The sheet counts as raised above this position; pulled below [UP_RELEASE] from there it closes. */
    const val UP_THRESHOLD = 0.5f
    const val UP_RELEASE = 0.85f
    const val FLING_UP = 1500f
    const val FLING_CLOSE = 2500f

    // Where the sheet starts if the pill's bounds are not known yet.
    val fallbackPillBottom = 64.dp
    val fallbackPillHeight = 46.dp

    val background = 0f to 0.35f
    val content = 0.4f to 0.85f
    val contentRise = 0.4f to 1f
    val shadow = 0.2f to 1f
    val contentOffset = 24.dp
    val shadowElevation = 24.dp
    val topRadiusClosed = 23.dp
    val topRadiusOpen = 30.dp
    val bottomRadiusClosed = 23.dp

    val handleWidth = 40.dp
    val handleHeight = 5.dp
    val handleTop = 10.dp
    val avatarSize = 34.dp
    /** Distance from the top of the sheet to the "N commenti" title. */
    val titleTop = 34.dp
    val sectionInset = 20.dp
    val hintFade = 0f to 0.3f
    val rowPadding = 11.dp
    val barPadding = 10.dp
    val barSide = 16.dp
    val barBottom = 12.dp
}

/**
 * State of the comments sheet that rises over an open post. [open] (0..1) is the opening spring, [position] the place of the
 * sheet between its heights: 0 is the middle one, 1 the high one, below 0 it is being pulled down to close.
 */
internal class CommentsState(private val scope: CoroutineScope) {
    val open = Animatable(0f)
    val position = Animatable(0f)
    /** Bounds of the "comments" pill of the open post (window coordinates), where the sheet starts; updated by the card. */
    var pillBounds: Rect = Rect.Zero

    var postId: String? by mutableStateOf(null)
        private set
    val isShown: Boolean get() = postId != null

    /** True when the current drag started with the sheet fully raised: pulling it down from there closes it, with no stop in the middle. */
    var dragStartedUp: Boolean = false

    /** Height of the area the sheet is laid out in, written by the layout and used to turn drags into positions. */
    var heightPx: Float = 0f

    /** Moves the sheet with the finger: [dy] pixels downwards lowers its position. */
    fun dragBy(dy: Float, isQuestion: Boolean) {
        val range = CommentsSpec.DRAG_RANGE * heightPx
        if (range <= 0f) return
        val next = (position.value - dy / range).coerceIn(-1f, if (isQuestion) 0f else 1f)
        scope.launch { position.snapTo(next) }
    }

    fun show(post: FeedPost) {
        postId = post.id.value
        scope.launch {
            position.snapTo(0f)
            open.animateTo(1f, spring(CommentsSpec.OPEN_DAMPING, CommentsSpec.OPEN_STIFFNESS))
        }
    }

    fun hide() {
        scope.launch {
            open.animateTo(0f, spring(CommentsSpec.OPEN_DAMPING, CommentsSpec.OPEN_STIFFNESS))
            postId = null
            position.snapTo(0f)
        }
    }

    /** Hides at once, without animation (used when the phone turns to landscape). */
    fun hideNow() {
        scope.launch { open.snapTo(0f); postId = null; position.snapTo(0f) }
    }

    fun moveTo(target: Float) {
        scope.launch { position.animateTo(target, spring(CommentsSpec.POSITION_DAMPING, CommentsSpec.POSITION_STIFFNESS)) }
    }
}

private fun sheetTop(position: Float, heightPx: Float, isQuestion: Boolean): Float {
    val mid = (if (isQuestion) CommentsSpec.QUESTION_TOP else CommentsSpec.MID_TOP) * heightPx
    val up = (if (isQuestion) CommentsSpec.QUESTION_TOP else CommentsSpec.UP_TOP) * heightPx
    val range = CommentsSpec.DRAG_RANGE * heightPx
    return if (position >= 0f) mid + (up - mid) * position else mid - position * range
}

/**
 * The comments sheet. Its rectangle goes from the "comments" pill of the open post to its final place with one spring; the
 * content has a fixed size and only moves and fades (layout and draw phases read the progress, composition does not).
 */
@Composable
internal fun CommentsSheet(
    post: FeedPost,
    state: CommentsState,
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HoopTheme.colors
    val isQuestion = post.type == PostType.Question
    val shape = remember(state, isQuestion) { SheetShape(state, isQuestion) }
    Box(
        modifier
            .layout { measurable, constraints ->
                // The content is laid out once at its final size; it only moves when the sheet is dragged between its heights.
                val height = constraints.maxHeight.toFloat()
                state.heightPx = height
                val top = sheetTop(state.position.value, height, isQuestion).roundToInt()
                val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, (constraints.maxHeight - top).coerceAtLeast(0)))
                layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, top) }
            }
            .graphicsLayer {
                // The morph from the pill to the sheet happens here, in the draw phase: no composition, no layout.
                this.shape = shape
                clip = true
                shadowElevation = CommentsSpec.shadowElevation.toPx() * rampOf(state.open.value, CommentsSpec.shadow)
            }
            .drawBehind { drawRect(colors.surface.copy(alpha = 0.7f + 0.3f * rampOf(state.open.value, CommentsSpec.background))) },
    ) {
        SheetContent(post, state, isQuestion, replyText, onReplyTextChange, onSend)
    }
}

/**
 * Outline of the sheet while it opens: it starts as the "comments" pill (its real bounds, measured on the open post) and grows
 * to the sheet's rectangle. Read in the layer, so the animation costs no layout. Coordinates are relative to the sheet's top.
 */
private class SheetShape(private val state: CommentsState, private val isQuestion: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        // The spring may overshoot a little: only the top uses it, the sides and bottom stay on the screen.
        val s = state.open.value.coerceAtLeast(0f)
        val edge = s.coerceIn(0f, 1f)
        val width = size.width
        val height = state.heightPx
        val finalTop = sheetTop(state.position.value, height, isQuestion)
        val pill = state.pillBounds.takeIf { it.width > 0f } ?: with(density) {
            Rect(width / 2f, height - CommentsSpec.fallbackPillBottom.toPx() - CommentsSpec.fallbackPillHeight.toPx(), width - 16.dp.toPx(), height - CommentsSpec.fallbackPillBottom.toPx())
        }
        val left = pill.left * (1f - edge)
        val right = width - (width - pill.right) * (1f - edge)
        val top = pill.top + (finalTop - pill.top) * s - finalTop
        val bottom = (height - finalTop) - (height - pill.bottom) * (1f - edge)
        val topRadius = with(density) { (CommentsSpec.topRadiusClosed + (CommentsSpec.topRadiusOpen - CommentsSpec.topRadiusClosed) * edge).toPx() }
        val bottomRadius = with(density) { (CommentsSpec.bottomRadiusClosed * (1f - edge)).toPx() }
        return Outline.Rounded(
            RoundRect(left, top, right, bottom, CornerRadius(topRadius), CornerRadius(topRadius), CornerRadius(bottomRadius), CornerRadius(bottomRadius)),
        )
    }
}

@Composable
private fun SheetContent(
    post: FeedPost,
    state: CommentsState,
    isQuestion: Boolean,
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val colors = HoopTheme.colors
    val spec = CommentsSpec
    Column(
        Modifier.fillMaxSize().graphicsLayer {
            val s = state.open.value
            alpha = rampOf(s, spec.content)
            translationY = spec.contentOffset.toPx() * (1f - rampOf(s, spec.contentRise))
        },
    ) {
        // Everything above the list can be dragged to move the sheet between its heights or to close it.
        Column(
            Modifier.fillMaxWidth().draggable(
                rememberDraggableState { dy -> state.dragBy(dy, isQuestion) },
                Orientation.Vertical,
                onDragStarted = { state.dragStartedUp = state.position.value > CommentsSpec.UP_THRESHOLD },
                onDragStopped = { velocity -> sheetRelease(state, velocity, isQuestion) },
            ),
        ) {
            Box(
                Modifier.padding(top = spec.handleTop).align(Alignment.CenterHorizontally).size(spec.handleWidth, spec.handleHeight)
                    .background(colors.outlineStrong, RoundedCornerShape(3.dp)),
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = spec.sectionInset).padding(top = spec.titleTop - spec.handleTop - spec.handleHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    repliesHeading(post),
                    style = HoopTheme.typography.labelMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                    color = colors.onSurface,
                )
                Spacer(Modifier.weight(1f))
                if (!isQuestion) {
                    Text(
                        stringResource(Res.string.comments_hint),
                        Modifier.graphicsLayer { alpha = 1f - rampOf(state.position.value, spec.hintFade) },
                        style = HoopTheme.typography.labelMedium, color = colors.onSurfaceVariant, maxLines = 1,
                    )
                }
            }
            Box(Modifier.padding(top = HoopTheme.spacing.sm).fillMaxWidth().height(HoopTheme.sizes.dividerThickness).background(colors.divider))
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = spec.sectionInset)) {
            items(post.replies, key = { it.id }) { reply -> CommentRow(reply) }
        }
        Column(
            Modifier.fillMaxWidth().background(colors.surface)
                .padding(horizontal = spec.barSide).padding(top = spec.barPadding)
                .imePadding().navigationBarsPadding().padding(bottom = spec.barBottom),
        ) {
            HoopInputBar(
                value = replyText, onValueChange = onReplyTextChange,
                placeholder = stringResource(Res.string.comment_placeholder),
                sendLabel = stringResource(Res.string.reply_send), onSend = onSend,
            )
        }
    }
}

private fun sheetRelease(state: CommentsState, velocity: Float, isQuestion: Boolean) {
    val position = state.position.value
    when {
        // Raised to the top and pulled down: close right away instead of resting at the middle height.
        state.dragStartedUp && (position < CommentsSpec.UP_RELEASE || velocity > CommentsSpec.FLING_CLOSE) -> state.hide()
        position < CommentsSpec.CLOSE_BELOW || velocity > CommentsSpec.FLING_CLOSE && position <= 0.05f -> state.hide()
        !isQuestion && (position > 0.5f || velocity < -CommentsSpec.FLING_UP) -> state.moveTo(1f)
        else -> state.moveTo(0f)
    }
}

@Composable
private fun CommentRow(reply: Reply) {
    val colors = HoopTheme.colors
    val spec = CommentsSpec
    Row(
        Modifier.fillMaxWidth().padding(vertical = spec.rowPadding),
        horizontalArrangement = Arrangement.spacedBy(HoopTheme.spacing.sm),
    ) {
        Box(Modifier.size(spec.avatarSize).clip(HoopTheme.shapes.circle).background(colors.avatarSoft), contentAlignment = Alignment.Center) {
            Text(initialsOf(reply.author.name), style = HoopTheme.typography.labelSmall, color = colors.onSurface)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HoopTheme.spacing.xs)) {
                Text(
                    stringResource(Res.string.author_handle, reply.author.name.lowercase()),
                    style = HoopTheme.typography.labelMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold), color = colors.onSurface,
                )
                Text(formatAge(reply.ageMinutes), style = HoopTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
            Text(reply.text, style = HoopTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, lineHeight = 20.sp), color = colors.onSurface)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(HoopIcons.Fly, null, tint = colors.onSurface, modifier = Modifier.size(20.dp))
            Text(formatCount(reply.fliesCount), style = HoopTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold), color = colors.onSurfaceVariant)
        }
    }
}
