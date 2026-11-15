package app.hoop.presentation.onboarding.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.component.*
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.presentation.onboarding.common.*
import app.hoop.presentation.onboarding.messageRes
import app.hoop.presentation.onboarding.resources.*
import org.jetbrains.compose.resources.stringResource

/** Step 1 of 2: email of the account. */
@Composable
fun RecoveryEmailScreen(state: RecoveryUiState, onIntent: (RecoveryIntent) -> Unit) {
    AuthScreenFrame(
        onBack = { onIntent(RecoveryIntent.BackClicked) },
        showRings = true,
        actions = {
            HoopButton(stringResource(Res.string.recovery_send_link), { onIntent(RecoveryIntent.SendLinkClicked) }, loading = state.isLoading)
        },
        secondaryActions = {
            HoopFooterPrompt(stringResource(Res.string.recovery_remembered), stringResource(Res.string.recovery_sign_in), { onIntent(RecoveryIntent.SignInClicked) })
        },
    ) {
        ScreenHeading(stringResource(Res.string.recovery_email_title), stringResource(Res.string.recovery_email_subtitle))
        HoopTextField(
            label = stringResource(Res.string.field_email),
            value = state.email,
            onValueChange = { onIntent(RecoveryIntent.EmailChanged(it)) },
            placeholder = stringResource(Res.string.field_email_placeholder),
            state = if (state.emailError != null) HoopFieldState.Error else HoopFieldState.Default,
            supportingText = state.emailError?.let { stringResource(it.messageRes()) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onIntent(RecoveryIntent.SendLinkClicked) }),
        )
    }
}

private object MailIllustrationSpec {
    val height = 230.dp
    val outerRing = 230.dp
    val innerRing = 170.dp
    val disc = 112.dp
    val glyph = 60.dp
    val checkBadge = 32.dp
    val checkGlyph = 16.dp
    val ringWidth = 2.dp
    const val OUTER_ALPHA = 0.2f
    const val INNER_ALPHA = 0.4f
}

/** Step 2 of 2: check the mailbox; the new password is chosen on the web page opened from the email. */
@Composable
fun RecoveryCheckMailScreen(state: RecoveryUiState, onIntent: (RecoveryIntent) -> Unit) {
    val spacing = HoopTheme.spacing
    val canResend = state.resendSecondsLeft == 0
    AuthScreenFrame(
        onBack = { onIntent(RecoveryIntent.BackClicked) },
        actions = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                HoopButton(stringResource(Res.string.recovery_open_mail), { onIntent(RecoveryIntent.OpenMailClicked) })
                HoopButton(stringResource(Res.string.recovery_go_sign_in), { onIntent(RecoveryIntent.SignInClicked) }, style = HoopButtonStyle.Secondary)
            }
            Text(
                hoopLinkedText(
                    "${stringResource(Res.string.recovery_not_arrived)} " to null,
                    stringResource(Res.string.recovery_resend) to if (canResend) ({ onIntent(RecoveryIntent.ResendClicked) }) else null,
                    (if (canResend) "" else " " + stringResource(Res.string.recovery_resend_in, formatCountdown(state.resendSecondsLeft))) to null,
                    "\n${stringResource(Res.string.recovery_check_spam)}" to null,
                ),
                Modifier.fillMaxWidth().padding(top = spacing.md),
                style = HoopTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = HoopTheme.typography.bodyMedium.lineHeight),
                color = HoopTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        },
    ) {
        Box(Modifier.fillMaxWidth().height(MailIllustrationSpec.height), contentAlignment = Alignment.Center) {
            Box(Modifier.size(MailIllustrationSpec.outerRing).border(MailIllustrationSpec.ringWidth, HoopTheme.colors.accent.copy(alpha = MailIllustrationSpec.OUTER_ALPHA), HoopTheme.shapes.circle))
            Box(Modifier.size(MailIllustrationSpec.innerRing).border(MailIllustrationSpec.ringWidth, HoopTheme.colors.accent.copy(alpha = MailIllustrationSpec.INNER_ALPHA), HoopTheme.shapes.circle))
            Box(Modifier.size(MailIllustrationSpec.disc).background(HoopTheme.colors.warmContainer, HoopTheme.shapes.circle), contentAlignment = Alignment.Center) {
                Icon(HoopIcons.Mail, stringResource(Res.string.recovery_mail_description), tint = HoopTheme.colors.onSurface, modifier = Modifier.size(MailIllustrationSpec.glyph))
                Box(
                    Modifier.align(Alignment.TopEnd).padding(spacing.xs).size(MailIllustrationSpec.checkBadge)
                        .background(HoopTheme.colors.brand, HoopTheme.shapes.circle)
                        .border(HoopTheme.sizes.dividerThickness * 3, HoopTheme.colors.surface, HoopTheme.shapes.circle),
                    contentAlignment = Alignment.Center,
                ) { Icon(HoopIcons.Check, null, tint = HoopTheme.colors.onBrand, modifier = Modifier.size(MailIllustrationSpec.checkGlyph)) }
            }
        }
        Text(stringResource(Res.string.recovery_mail_title), Modifier.fillMaxWidth().padding(top = spacing.sm), style = HoopTheme.typography.headline, color = HoopTheme.colors.onSurface, textAlign = TextAlign.Center)
        Text(
            buildAnnotatedString {
                append(stringResource(Res.string.recovery_mail_body_before))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = HoopTheme.colors.onSurface)) { append(state.sentTo) }
                append(stringResource(Res.string.recovery_mail_body_after))
            },
            Modifier.fillMaxWidth().padding(top = spacing.sm),
            style = HoopTheme.typography.bodyMedium,
            color = HoopTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private const val SECONDS_PER_MINUTE = 60

/** 20 → "0:20". */
private fun formatCountdown(seconds: Int): String = "${seconds / SECONDS_PER_MINUTE}:${(seconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')}"
