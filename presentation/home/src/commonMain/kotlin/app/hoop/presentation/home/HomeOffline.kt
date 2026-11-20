package app.hoop.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.presentation.home.resources.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

private object OfflineSpec {
    val radius = 22.dp
    val buttonRadius = 18.dp
    val icon = 20.dp
    val gap = 10.dp
    val padStart = 14.dp
    val padOther = 8.dp
    val buttonPadH = 16.dp
    val buttonPadV = 9.dp
    val spinnerStroke = 2.dp
    val textSize = 14.sp
    const val DIM_ALPHA = 0.6f
    const val DIM_SATURATION = 0.5f
    const val TIME_PAD = 2
}

/** The saved copy of the data is on screen: the notice with when it was saved, and "Riprova". */
@Composable
internal fun OfflineNotice(offline: OfflineUi, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    val shape = RoundedCornerShape(OfflineSpec.radius)
    Row(
        modifier.shadow(HoopTheme.elevation.card, shape).background(colors.surface, shape)
            .padding(start = OfflineSpec.padStart, top = OfflineSpec.padOther, end = OfflineSpec.padOther, bottom = OfflineSpec.padOther),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OfflineSpec.gap),
    ) {
        Box(Modifier.size(OfflineSpec.icon), contentAlignment = Alignment.Center) {
            if (offline.retrying) {
                CircularProgressIndicator(Modifier.size(OfflineSpec.icon), color = colors.link, strokeWidth = OfflineSpec.spinnerStroke)
            } else {
                Icon(HoopIcons.CloudOff, null, tint = colors.link, modifier = Modifier.size(OfflineSpec.icon))
            }
        }
        Text(
            if (offline.retrying) stringResource(Res.string.offline_retrying) else offlineText(offline.lastUpdatedMillis),
            Modifier.weight(1f), style = HoopTheme.typography.labelMedium.copy(fontSize = OfflineSpec.textSize, fontWeight = FontWeight.SemiBold),
            color = colors.onSurface,
        )
        Text(
            stringResource(Res.string.offline_retry),
            Modifier.clip(RoundedCornerShape(OfflineSpec.buttonRadius)).background(colors.accent, RoundedCornerShape(OfflineSpec.buttonRadius))
                .clickable(enabled = !offline.retrying, onClick = onRetry)
                .padding(horizontal = OfflineSpec.buttonPadH, vertical = OfflineSpec.buttonPadV),
            style = HoopTheme.typography.labelMedium.copy(fontSize = OfflineSpec.textSize, fontWeight = FontWeight.Bold),
            color = colors.surface,
        )
    }
}

/** "Offline · aggiornato alle 20:12", "… ieri alle 20:12" or "… il 18 nov alle 20:12". */
@Composable
private fun offlineText(lastUpdatedMillis: Long?): String {
    if (lastUpdatedMillis == null) return stringResource(Res.string.offline_unknown)
    val zone = TimeZone.currentSystemDefault()
    val then = Instant.fromEpochMilliseconds(lastUpdatedMillis).toLocalDateTime(zone)
    val today = Clock.System.now().toLocalDateTime(zone).date
    val time = "${then.hour.toString().padStart(OfflineSpec.TIME_PAD, '0')}:${then.minute.toString().padStart(OfflineSpec.TIME_PAD, '0')}"
    val days = (today.toEpochDays() - then.date.toEpochDays()).toLong()
    return when {
        days <= 0 -> stringResource(Res.string.offline_today, time)
        days == 1L -> stringResource(Res.string.offline_yesterday, time)
        else -> stringResource(Res.string.offline_other_day, then.dayOfMonth, stringArrayResource(Res.array.months_short)[then.monthNumber - 1], time)
    }
}

/** Saved data looks switched off: translucent and with half the color. */
internal fun Modifier.dimmed(enabled: Boolean): Modifier = if (!enabled) this else drawWithContent {
    val paint = Paint().apply {
        alpha = OfflineSpec.DIM_ALPHA
        colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(OfflineSpec.DIM_SATURATION) })
    }
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
        drawContent()
        canvas.restore()
    }
}
