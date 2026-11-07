package app.hoop.presentation.onboarding.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import app.hoop.designsystem.component.HoopButton
import app.hoop.designsystem.component.HoopSuccessBadge
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.presentation.onboarding.resources.*
import org.jetbrains.compose.resources.stringResource

/** Full-screen "done" layout shared by sign-up and password recovery. */
@Composable
internal fun DoneScreenFrame(
    badgeColor: Color,
    ringColor: Color,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val spacing = HoopTheme.spacing
    Column(
        Modifier.fillMaxSize().background(HoopTheme.colors.surface).windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = spacing.screenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        HoopSuccessBadge(HoopIcons.Check, stringResource(Res.string.done_description), badgeColor, ringColor)
        Spacer(Modifier.height(spacing.xl + spacing.xs))
        Text(title, style = HoopTheme.typography.displayLarge, color = HoopTheme.colors.onSurface, textAlign = TextAlign.Center)
        Text(body, Modifier.padding(top = spacing.sm), style = HoopTheme.typography.bodyMedium, color = HoopTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        HoopButton(actionLabel, onAction, Modifier.fillMaxWidth().padding(bottom = spacing.md))
    }
}
