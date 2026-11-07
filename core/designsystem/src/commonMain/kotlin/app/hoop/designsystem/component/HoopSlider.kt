package app.hoop.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.hoop.designsystem.theme.HoopTheme

@OptIn(ExperimentalMaterial3Api::class)
/** Slider with an accent track and a white thumb with an accent ring. */
@Composable
fun HoopSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = HoopTheme.colors
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier.fillMaxWidth().height(HoopTheme.sizes.touchTarget).semantics { contentDescription = description },
        colors = SliderDefaults.colors(activeTrackColor = colors.accent, inactiveTrackColor = colors.track),
        thumb = {
            Box(
                Modifier.size(HoopTheme.sizes.sliderThumb)
                    .shadow(HoopTheme.elevation.badge / 2, HoopTheme.shapes.circle)
                    .background(colors.surface, HoopTheme.shapes.circle)
                    .border(HoopTheme.sizes.sliderTrack / 2, colors.accent, HoopTheme.shapes.circle),
                contentAlignment = Alignment.Center,
            ) {}
        },
        track = { state ->
            SliderDefaults.Track(
                sliderState = state,
                modifier = Modifier.height(HoopTheme.sizes.sliderTrack),
                colors = SliderDefaults.colors(activeTrackColor = colors.accent, inactiveTrackColor = colors.track),
                thumbTrackGapSize = 0.dp,
                trackInsideCornerSize = 0.dp,
                drawStopIndicator = null,
            )
        },
    )
}
