package app.hoop.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hoop.designsystem.component.HoopPostCardDefaults
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.presentation.home.resources.*
import org.jetbrains.compose.resources.stringResource

private object EmptySpec {
    val firstEdge = 2.5.dp
    val ghostEdge = 2.dp
    val dash = 7.dp
    val dashGap = 5.dp
    val title = 21.sp
    val badge = 52.dp
    val badgeIcon = 24.dp
    val gap = 12.dp
    val shapeCircle = 34.dp
    val shapeInset = 12.dp
    val barHeight = 12.dp
    val barBottom = listOf(44.dp to 50.dp, 22.dp to 80.dp) // (distance from the bottom, width taken off the card)
    const val FIRST_TINT = 0.12f
    const val GHOST_FILL = 0.5f
    const val GHOSTS = 3
}

/**
 * The carousel of a Hoop without posts: three placeholder cards where the real ones would be.
 * The first one invites to publish (tapping it does what the "+" does); the others only hint at what will come.
 */
@Composable
internal fun EmptyHoopCards(accent: Color, onPublish: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    BoxWithConstraints(modifier) {
        val listState = rememberLazyListState()
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = HomeSpec.cardsStart, end = (maxWidth - HoopPostCardDefaults.CarouselWidth - HomeSpec.cardsStart).coerceAtLeast(HomeSpec.cardsEndPadding)),
            horizontalArrangement = Arrangement.spacedBy(HomeSpec.cardsGap),
            flingBehavior = rememberSnapFlingBehavior(listState),
        ) {
            items(EmptySpec.GHOSTS) { index ->
                val card = Modifier.width(HoopPostCardDefaults.CarouselWidth).height(HoopPostCardDefaults.CarouselHeight)
                if (index == 0) FirstGhost(accent, onPublish, card) else Ghost(card, colors.ghostEdge, colors.ghostShape)
            }
        }
    }
}

@Composable
private fun FirstGhost(accent: Color, onClick: () -> Unit, modifier: Modifier) {
    val colors = HoopTheme.colors
    val shape = RoundedCornerShape(HoopPostCardDefaults.CornerRadius)
    Column(
        modifier.clip(shape)
            .background(colors.surface.copy(alpha = 0.7f), shape)
            .background(accent.copy(alpha = EmptySpec.FIRST_TINT), shape)
            .dashedBorder(accent, EmptySpec.firstEdge, HoopPostCardDefaults.CornerRadius)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(EmptySpec.gap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(EmptySpec.badge).background(accent, CircleShape), contentAlignment = Alignment.Center) {
            Icon(HoopIcons.Pin, null, tint = colors.surface, modifier = Modifier.size(EmptySpec.badgeIcon))
        }
        Text(stringResource(Res.string.empty_hoop_title), style = HoopTheme.typography.titleLarge.copy(fontSize = EmptySpec.title), color = colors.onSurface, textAlign = TextAlign.Center)
        Text(stringResource(Res.string.empty_hoop_hint), style = HoopTheme.typography.labelMedium, color = colors.ghostHint, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Ghost(modifier: Modifier, edge: Color, shapeColor: Color) {
    val colors = HoopTheme.colors
    val shape = RoundedCornerShape(HoopPostCardDefaults.CornerRadius)
    Box(modifier.clip(shape).background(colors.surface.copy(alpha = EmptySpec.GHOST_FILL), shape).dashedBorder(edge, EmptySpec.ghostEdge, HoopPostCardDefaults.CornerRadius)) {
        Box(Modifier.padding(EmptySpec.shapeInset).size(EmptySpec.shapeCircle).background(shapeColor, CircleShape))
        EmptySpec.barBottom.forEach { (bottom, inset) ->
            Box(
                Modifier.align(Alignment.BottomStart).padding(start = EmptySpec.shapeInset, bottom = bottom)
                    .width(HoopPostCardDefaults.CarouselWidth - inset).height(EmptySpec.barHeight).background(shapeColor, RoundedCornerShape(EmptySpec.barHeight / 2)),
            )
        }
    }
}

/** A dashed rounded outline drawn inside the bounds, so that it is not cut by the clip. */
private fun Modifier.dashedBorder(color: Color, width: Dp, radius: Dp): Modifier = drawBehind {
    val stroke = width.toPx()
    val dash = EmptySpec.dash.toPx()
    val gap = EmptySpec.dashGap.toPx()
    drawRoundRect(
        color = color, topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius.toPx() - stroke / 2),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap))),
    )
}
