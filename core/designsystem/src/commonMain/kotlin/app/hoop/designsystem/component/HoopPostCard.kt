package app.hoop.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme

enum class HoopPostKind { Question, Photo, Video }

object HoopPostCardDefaults {
    val CarouselWidth: Dp = 200.dp
    val CarouselHeight: Dp = 220.dp
    val CornerRadius: Dp = 30.dp
    val ExpandedCornerRadius: Dp = 0.dp
    val TextInset: Dp = 14.dp
    val BottomInset: Dp = 14.dp
    val ChipExpandedShift: Dp = 44.dp
    val TagInset: Dp = 12.dp
    val TitleSize = 18.sp
    val TitleSizeExpanded = 36.sp
    val QuestionTitleSize = 20.sp
    val QuestionTitleSizeExpanded = 38.sp
    const val FADE_OUT_FACTOR = 1.6f
    val TopScrimHeight: Dp = 64.dp
    const val TOP_SCRIM_ALPHA = 0.32f
    const val BOTTOM_SCRIM_ALPHA = 0.78f
    const val BOTTOM_SCRIM_FRACTION = 0.7f
}

/**
 * A post card used in the carousel and, with [progress] going from 0 to 1, expanded to full screen:
 * the title grows, the author chip moves down, tag and play badge fade out, the bottom block rises by [expandedBottomInset].
 * Text and images come from the caller.
 */
@Composable
fun HoopPostCard(
    kind: HoopPostKind,
    title: String,
    details: String,
    repliesLabel: String,
    authorInitials: String,
    authorName: String,
    tagLabel: String,
    modifier: Modifier = Modifier,
    questionColor: Color = HoopTheme.colors.questionCards.first(),
    media: Painter? = null,
    progress: Float = 0f,
    expandedBottomInset: Dp = 0.dp,
) {
    val colors = HoopTheme.colors
    val isQuestion = kind == HoopPostKind.Question
    val contentColor = if (isQuestion) colors.onSurface else colors.onScrim
    val fade = (1f - progress * HoopPostCardDefaults.FADE_OUT_FACTOR).coerceIn(0f, 1f)
    Box(modifier.fillMaxSize()) {
        if (isQuestion || media == null) {
            Box(Modifier.fillMaxSize().background(questionColor))
        } else {
            Image(media, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(HoopPostCardDefaults.BOTTOM_SCRIM_FRACTION)
                    .background(Brush.verticalGradient(listOf(colors.scrim.copy(alpha = 0f), colors.scrim.copy(alpha = HoopPostCardDefaults.BOTTOM_SCRIM_ALPHA)))),
            )
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().height(HoopPostCardDefaults.TopScrimHeight)
                    .background(Brush.verticalGradient(listOf(colors.scrim.copy(alpha = HoopPostCardDefaults.TOP_SCRIM_ALPHA), colors.scrim.copy(alpha = 0f)))),
            )
            if (kind == HoopPostKind.Video) {
                HoopPlayBadge(Modifier.align(Alignment.Center).offset(y = (-HoopTheme.spacing.xl)).alpha(fade))
            }
        }
        HoopUserChip(
            authorInitials, authorName,
            large = true,
            modifier = Modifier.align(Alignment.TopStart).padding(start = HoopPostCardDefaults.TagInset, top = HoopPostCardDefaults.TagInset + HoopPostCardDefaults.ChipExpandedShift * progress),
        )
        Box(Modifier.align(Alignment.TopEnd).padding(end = HoopPostCardDefaults.TagInset, top = HoopTheme.spacing.md).alpha(fade)) {
            HoopTagChip(kind, tagLabel)
        }
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .padding(start = HoopPostCardDefaults.TextInset, end = HoopPostCardDefaults.TextInset)
                .padding(bottom = HoopPostCardDefaults.BottomInset + (expandedBottomInset + HoopTheme.spacing.md) * progress),
        ) {
            val small = if (isQuestion) HoopPostCardDefaults.QuestionTitleSize else HoopPostCardDefaults.TitleSize
            val large = if (isQuestion) HoopPostCardDefaults.QuestionTitleSizeExpanded else HoopPostCardDefaults.TitleSizeExpanded
            val size = (small.value + (large.value - small.value) * progress).sp
            Text(
                title,
                style = HoopTheme.typography.titleLarge.copy(fontSize = size, lineHeight = size * 1.12f),
                color = contentColor,
            )
            Row(Modifier.padding(top = HoopTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text(details, style = HoopTheme.typography.caption, color = contentColor.copy(alpha = 0.92f))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Icon(HoopIcons.Chat, null, tint = contentColor, modifier = Modifier.size(HoopTheme.sizes.iconSmall - 4.dp))
                    Text(repliesLabel, Modifier.padding(start = HoopTheme.spacing.xs - HoopTheme.spacing.xxs), style = HoopTheme.typography.caption, color = contentColor)
                }
            }
        }
    }
}

/** Small colored label telling the post type. */
@Composable
fun HoopTagChip(kind: HoopPostKind, label: String, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    val (container, content) = when (kind) {
        HoopPostKind.Question -> colors.tagQuestion to colors.onTagQuestion
        HoopPostKind.Photo -> colors.tagPhoto to colors.onTagPhoto
        HoopPostKind.Video -> colors.tagVideo to colors.onTagVideo
    }
    Text(
        label,
        modifier.clip(HoopTheme.shapes.chip).background(container).padding(horizontal = HoopTheme.spacing.sm - 1.dp, vertical = HoopTheme.spacing.xxs + 1.dp),
        style = HoopTheme.typography.labelSmall, color = content,
    )
}
