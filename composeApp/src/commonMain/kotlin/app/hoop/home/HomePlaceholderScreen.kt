package app.hoop.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.resources.*
import org.jetbrains.compose.resources.stringResource

/** Placeholder shown after sign-in until the real home is built. */
@Composable
fun HomePlaceholderScreen() {
    Column(
        Modifier.fillMaxSize().background(HoopTheme.colors.background).safeDrawingPadding().padding(HoopTheme.spacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(HoopTheme.spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.home_placeholder_title), style = HoopTheme.typography.displayLarge, color = HoopTheme.colors.onSurface)
        Text(stringResource(Res.string.home_placeholder_body), style = HoopTheme.typography.bodyMedium, color = HoopTheme.colors.onSurfaceVariant)
    }
}
