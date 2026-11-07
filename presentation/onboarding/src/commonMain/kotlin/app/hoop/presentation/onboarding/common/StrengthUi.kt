package app.hoop.presentation.onboarding.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.domain.identity.PasswordStrength
import app.hoop.presentation.onboarding.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal const val STRENGTH_LEVELS = 4

/** How many of the meter bars are filled. */
internal fun PasswordStrength.level(): Int = when (this) {
    PasswordStrength.Empty -> 0
    PasswordStrength.Weak -> 1
    PasswordStrength.Fair -> 2
    PasswordStrength.Good -> 3
    PasswordStrength.Strong -> 4
}

@Composable
internal fun PasswordStrength.color(): Color = when (this) {
    PasswordStrength.Empty, PasswordStrength.Weak -> HoopTheme.colors.errorOutline
    PasswordStrength.Fair -> HoopTheme.colors.accent
    PasswordStrength.Good, PasswordStrength.Strong -> HoopTheme.colors.success
}

private fun PasswordStrength.captionRes(): StringResource? = when (this) {
    PasswordStrength.Empty -> null
    PasswordStrength.Weak -> Res.string.strength_weak
    PasswordStrength.Fair -> Res.string.strength_fair
    PasswordStrength.Good -> Res.string.strength_good
    PasswordStrength.Strong -> Res.string.strength_strong
}

@Composable
internal fun PasswordStrength.caption(): String = captionRes()?.let { stringResource(it) } ?: ""
