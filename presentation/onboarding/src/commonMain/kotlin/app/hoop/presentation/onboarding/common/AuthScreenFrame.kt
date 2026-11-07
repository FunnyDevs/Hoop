package app.hoop.presentation.onboarding.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.component.HoopCornerRings
import app.hoop.designsystem.component.HoopIconButton
import app.hoop.designsystem.component.HoopProgressSegments
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.presentation.onboarding.resources.*
import org.jetbrains.compose.resources.stringResource

/** Position inside a multi-step flow. */
internal class StepProgress(val current: Int, val total: Int)

/**
 * Common frame of the sign-in / sign-up / recovery screens: back button (with optional progress),
 * a scrollable [content] area, and [actions] pinned to the bottom. Keyboard-aware.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AuthScreenFrame(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    progress: StepProgress? = null,
    showRings: Boolean = false,
    secondaryActions: (@Composable ColumnScope.() -> Unit)? = null,
    actions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().background(HoopTheme.colors.surface)) {
        if (showRings) HoopCornerRings()
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).imePadding()
                .padding(horizontal = HoopTheme.spacing.screenGutter),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HoopTheme.spacing.md - HoopTheme.spacing.xxs * 2)) {
                HoopIconButton(HoopIcons.ArrowBack, stringResource(Res.string.action_back), onBack)
                if (progress != null) {
                    HoopProgressSegments(progress.total, progress.current, stringResource(Res.string.progress_step, progress.current, progress.total), Modifier.weight(1f))
                }
            }
            // The primary action is always pinned (and lifted above the keyboard). The secondary actions
            // (alternative sign-in, footer links) sit below it, and scroll with the form while the keyboard is open.
            val keyboardVisible = WindowInsets.isImeVisible
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                content()
                if (keyboardVisible && secondaryActions != null) {
                    Column(Modifier.padding(top = HoopTheme.spacing.lg)) { secondaryActions() }
                }
            }
            Column(Modifier.padding(top = HoopTheme.spacing.sm, bottom = HoopTheme.spacing.md)) {
                actions()
                if (!keyboardVisible && secondaryActions != null) secondaryActions()
            }
        }
    }
}

/**
 * Large serif title with its supporting line. While the keyboard is open the supporting line is hidden
 * and the spacing shrinks, so the fields and the checkbox below stay visible.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScreenHeading(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val compact = WindowInsets.isImeVisible
    Column(modifier.padding(top = if (compact) HoopTheme.spacing.xs else HoopTheme.spacing.lg + HoopTheme.spacing.xxs)) {
        Text(title, style = HoopTheme.typography.headline, color = HoopTheme.colors.onSurface)
        if (compact) {
            Spacer(Modifier.height(HoopTheme.spacing.md))
        } else {
            Text(
                subtitle,
                Modifier.padding(top = HoopTheme.spacing.sm - HoopTheme.spacing.xxs, bottom = HoopTheme.spacing.lg),
                style = HoopTheme.typography.bodyMedium,
                color = HoopTheme.colors.onSurfaceVariant,
            )
        }
    }
}
