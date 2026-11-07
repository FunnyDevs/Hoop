package app.hoop.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.theme.HoopTheme

object HoopCardDefaults {
    val ScrimHeight: Dp = 96.dp
    const val SCRIM_TOP_ALPHA = 0f
    val PlayBadgeOffsetFraction = 0.44f
    const val PLAY_GLYPH_LEFT = 0.29f
    const val PLAY_GLYPH_RIGHT = 0.83f
    const val PLAY_GLYPH_TOP = 0.17f
    const val PLAY_GLYPH_BOTTOM = 0.83f
    const val SHADOW_ALPHA = 0.3f
}

/**
 * Card frame: shape, shadow, rotation and a gentle continuous vertical float.
 * [scale] fits the cards of a collage to the available space.
 */
@Composable
fun HoopCardFrame(
    modifier: Modifier = Modifier,
    rotationDegrees: Float = 0f,
    floatDelayMillis: Int = 0,
    scale: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    val motion = HoopTheme.motion
    val transition = rememberInfiniteTransition()
    val offsetDp by transition.animateFloat(
        initialValue = 0f,
        targetValue = motion.floatDistance.value,
        animationSpec = infiniteRepeatable(
            tween(motion.floatDurationMillis, delayMillis = floatDelayMillis, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
    )
    val shadowColor = HoopTheme.colors.shadow.copy(alpha = HoopCardDefaults.SHADOW_ALPHA)
    Box(
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotationDegrees
                scaleX = scale
                scaleY = scale
                translationY = offsetDp * scale * density
            }
            .shadow(HoopTheme.elevation.card, HoopTheme.shapes.card, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(HoopTheme.shapes.card),
        content = content,
    )
}

/** Question card: colored background, serif text and a details line. */
@Composable
fun HoopQuestionCard(question: String, details: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().background(HoopTheme.colors.questionContainer).padding(HoopTheme.spacing.md - HoopTheme.spacing.xxs),
    ) {
        Text(question, style = HoopTheme.typography.titleLarge, color = HoopTheme.colors.onSurface)
        Spacer(Modifier.height(HoopTheme.spacing.sm))
        Text(details, style = HoopTheme.typography.caption, color = HoopTheme.colors.onQuestionContainer)
    }
}

/**
 * Card whose photo or video fills the whole frame, with overlaid text.
 * Texts and images come from the caller: the component contains no text.
 */
@Composable
fun HoopMediaCard(
    painter: Painter,
    modifier: Modifier = Modifier,
    title: String? = null,
    authorInitials: String? = null,
    authorName: String? = null,
    isVideo: Boolean = false,
) {
    Box(modifier.fillMaxSize()) {
        Image(painter, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        if (title != null) {
            val scrim = HoopTheme.colors.scrim
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(HoopCardDefaults.ScrimHeight).background(
                    Brush.verticalGradient(
                        listOf(scrim.copy(alpha = HoopCardDefaults.SCRIM_TOP_ALPHA), scrim.copy(alpha = HoopTheme.motion.scrimAlphaOnMedia)),
                    ),
                ),
            )
            Text(
                title,
                modifier = Modifier.align(Alignment.BottomStart).padding(HoopTheme.spacing.md - HoopTheme.spacing.xxs - HoopTheme.spacing.xxs),
                style = HoopTheme.typography.titleMedium,
                color = HoopTheme.colors.onScrim,
            )
        }
        if (authorName != null && authorInitials != null) {
            HoopUserChip(authorInitials, authorName, Modifier.align(Alignment.TopStart).padding(HoopTheme.spacing.sm))
        }
        if (isVideo) HoopPlayBadge(Modifier.align(Alignment.Center))
    }
}

@Composable
fun HoopUserChip(initials: String, name: String, modifier: Modifier = Modifier) {
    val spacing = HoopTheme.spacing
    Row(
        modifier = modifier
            .height(HoopTheme.sizes.chipHeight)
            .clip(HoopTheme.shapes.chip)
            .background(HoopTheme.colors.surface)
            .padding(start = spacing.xxs + 1.dp, end = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(spacing.xs),
    ) {
        Box(
            Modifier.size(HoopTheme.sizes.avatar).clip(HoopTheme.shapes.circle).background(HoopTheme.colors.avatar),
            contentAlignment = Alignment.Center,
        ) { Text(initials, style = HoopTheme.typography.labelSmall, color = HoopTheme.colors.onSurface) }
        Text(name, style = HoopTheme.typography.labelSmall, color = HoopTheme.colors.onSurface)
    }
}

@Composable
fun HoopPlayBadge(modifier: Modifier = Modifier) {
    val glyphColor = HoopTheme.colors.onSurface
    Box(
        modifier
            .size(HoopTheme.sizes.playBadge)
            .clip(HoopTheme.shapes.circle)
            .background(HoopTheme.colors.surface.copy(alpha = HoopTheme.motion.playBadgeAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(HoopTheme.sizes.playGlyph)) {
            val triangle = Path().apply {
                moveTo(size.width * HoopCardDefaults.PLAY_GLYPH_LEFT, size.height * HoopCardDefaults.PLAY_GLYPH_TOP)
                lineTo(size.width * HoopCardDefaults.PLAY_GLYPH_RIGHT, size.height / 2f)
                lineTo(size.width * HoopCardDefaults.PLAY_GLYPH_LEFT, size.height * HoopCardDefaults.PLAY_GLYPH_BOTTOM)
                close()
            }
            drawPath(triangle, glyphColor)
        }
    }
}
