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
    const val FLING_UP = 1500f
    const val FLING_CLOSE = 2500f

    // Where the sheet starts: the right-hand pill of the open post.
    val pillBottom = 64.dp
    val pillHeight = 46.dp
    val pillInset = 16.dp
    val pillGap = 10.dp

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
    var postId: String? by mutableStateOf(null)
        private set
    val isShown: Boolean get() = postId != null

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
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = HoopTheme.colors
    val isQuestion = post.type == PostType.Question
    Box(
        modifier
            .layout { measurable, constraints ->
                val width = constraints.maxWidth.toFloat()
                val height = constraints.maxHeight.toFloat()
                val s = state.open.value.coerceAtLeast(0f)
                val inset = CommentsSpec.pillInset.toPx()
                val pillWidth = (width - inset * 2 - CommentsSpec.pillGap.toPx()) / 2f
                val pillLeft = inset + pillWidth + CommentsSpec.pillGap.toPx()
                state.heightPx = height
                val pillBottomEdge = height - (CommentsSpec.pillBottom.toPx() + bottomInset.toPx())
                val finalTop = sheetTop(state.position.value, height, isQuestion)
                // The spring may overshoot a little: only the height uses it, the sides and bottom stay on the screen.
                val edge = s.coerceIn(0f, 1f)
                val left = pillLeft * (1f - edge)
                val right = width - inset * (1f - edge)
                val bottom = height - (CommentsSpec.pillBottom.toPx() + bottomInset.toPx()) * (1f - edge)
                val pillTop = pillBottomEdge - CommentsSpec.pillHeight.toPx()
                val top = pillTop + (finalTop - pillTop) * s
                val w = max(right - left, 0f).roundToInt()
                val h = max(bottom - top, 0f).roundToInt()
                val placeable = measurable.measure(Constraints.fixed(w, h))
                layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(left.roundToInt(), top.roundToInt()) }
            }
            .graphicsLayer {
                val s = state.open.value.coerceAtLeast(0f)
                val topRadius = (CommentsSpec.topRadiusClosed + (CommentsSpec.topRadiusOpen - CommentsSpec.topRadiusClosed) * s.coerceIn(0f, 1f)).toPx()
                val bottomRadius = (CommentsSpec.bottomRadiusClosed * (1f - s.coerceIn(0f, 1f))).toPx()
                shape = RoundedCornerShape(topRadius, topRadius, bottomRadius, bottomRadius)
                clip = true
                shadowElevation = CommentsSpec.shadowElevation.toPx() * rampOf(s, CommentsSpec.shadow)
            }
            .drawBehind { drawRect(colors.surface.copy(alpha = 0.7f + 0.3f * rampOf(state.open.value, CommentsSpec.background))) },
    ) {
        SheetContent(post, state, isQuestion, replyText, onReplyTextChange, onSend)
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
                placeholder = stringResource(if (isQuestion) Res.string.reply_placeholder else Res.string.comment_placeholder),
                sendLabel = stringResource(Res.string.reply_send), onSend = onSend,
            )
        }
    }
}

private fun sheetRelease(state: CommentsState, velocity: Float, isQuestion: Boolean) {
    val position = state.position.value
    when {
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
