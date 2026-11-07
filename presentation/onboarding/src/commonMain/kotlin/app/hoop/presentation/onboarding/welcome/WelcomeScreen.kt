package app.hoop.presentation.onboarding.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.component.*
import app.hoop.designsystem.icon.HoopBrandIcon
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.presentation.onboarding.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Entry point: connects the ViewModel to navigation. */
@Composable
fun WelcomeRoute(
    onNavigateToSignUp: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onSocialSignIn: (HoopBrandIcon) -> Unit,
    viewModel: WelcomeViewModel = koinViewModel(),
) {
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                WelcomeEffect.NavigateToSignUp -> onNavigateToSignUp()
                WelcomeEffect.NavigateToSignIn -> onNavigateToSignIn()
                is WelcomeEffect.StartSocialSignIn -> onSocialSignIn(effect.provider)
            }
        }
    }
    WelcomeScreen(onIntent = viewModel::onIntent)
}

/** "Fading map" welcome: a collage of cards over a map that fades into the light background. */
@Composable
fun WelcomeScreen(onIntent: (WelcomeIntent) -> Unit) {
    val colors = HoopTheme.colors
    val spacing = HoopTheme.spacing

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.background)) {
        val logoTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + spacing.md
        val collageTop = logoTop + HoopTheme.sizes.logoBadgeHeight + spacing.sm + spacing.xxs
        val fitToWidth = maxWidth.value / WelcomeLayout.REFERENCE_WIDTH
        val fitToHeight = (maxHeight.value - WelcomeLayout.BOTTOM_BLOCK_RESERVE - collageTop.value) / WelcomeLayout.COLLAGE_HEIGHT
        val scale = minOf(fitToWidth, fitToHeight).coerceAtLeast(WelcomeLayout.MIN_SCALE)
        val leftInset = (maxWidth.value - WelcomeLayout.REFERENCE_WIDTH * scale) / 2f

        fun x(reference: Float): Dp = (leftInset + reference * scale).dp
        fun y(reference: Float): Dp = collageTop + ((reference - WelcomeLayout.COLLAGE_TOP) * scale).dp

        HoopMapBackdrop()

        WelcomeLayout.cards.forEach { card ->
            val width = card.width.dp
            val height = card.height.dp
            HoopCardFrame(
                modifier = Modifier
                    .offset(x(card.x) + (width * scale - width) / 2, y(card.y) + (height * scale - height) / 2)
                    .size(width, height),
                rotationDegrees = card.rotation,
                floatDelayMillis = card.floatDelayMillis,
                scale = scale,
            ) { CollageCardContent(card.kind) }
        }

        // Fade: the map disappears into the light background
        Box(
            Modifier.offset(y = y(WelcomeLayout.FADE_START)).fillMaxWidth()
                .height((WelcomeLayout.FADE_HEIGHT * scale).dp)
                .background(
                    Brush.verticalGradient(
                        0f to colors.background.copy(alpha = 0f),
                        WelcomeLayout.FADE_SOLID_STOP to colors.background,
                        1f to colors.background,
                    ),
                ),
        )
        Box(Modifier.offset(y = y(WelcomeLayout.FADE_SOLID_START)).fillMaxSize().background(colors.background))

        HoopLogoBadge(
            name = stringResource(Res.string.brand_name),
            modifier = Modifier.offset(spacing.screenGutter, logoTop),
        )

        BottomBlock(onIntent, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun BottomBlock(onIntent: (WelcomeIntent) -> Unit, modifier: Modifier) {
    val spacing = HoopTheme.spacing
    val navigationInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier.fillMaxWidth().padding(PaddingValues(start = spacing.screenGutter, end = spacing.screenGutter, bottom = spacing.lg + spacing.xxs + navigationInset)),
    ) {
        Text(stringResource(Res.string.welcome_title), style = HoopTheme.typography.displayLarge, color = HoopTheme.colors.onSurface)
        Spacer(Modifier.height(spacing.sm - spacing.xxs))
        Text(stringResource(Res.string.welcome_subtitle), style = HoopTheme.typography.bodyMedium, color = HoopTheme.colors.onSurfaceVariant)
        Spacer(Modifier.height(spacing.lg))
        HoopButton(stringResource(Res.string.welcome_create_account), { onIntent(WelcomeIntent.CreateAccountClicked) })
        Spacer(Modifier.height(spacing.sm))
        HoopButton(stringResource(Res.string.welcome_sign_in), { onIntent(WelcomeIntent.SignInClicked) }, style = HoopButtonStyle.Secondary)
        Spacer(Modifier.height(spacing.md))
        HoopLabeledDivider(stringResource(Res.string.welcome_or_continue_with))
        Spacer(Modifier.height(spacing.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.md, Alignment.CenterHorizontally)) {
            HoopBrandIcon.entries.forEach { provider ->
                HoopBrandIconButton(
                    icon = provider,
                    contentDescription = stringResource(provider.descriptionRes()),
                    onClick = { onIntent(WelcomeIntent.SocialClicked(provider)) },
                )
            }
        }
    }
}

@Composable
private fun CollageCardContent(kind: WelcomeLayout.CardKind) {
    when (kind) {
        WelcomeLayout.CardKind.SunsetPhoto -> HoopMediaCard(
            painter = painterResource(Res.drawable.welcome_sunset),
            title = stringResource(Res.string.welcome_sample_photo_title),
            authorInitials = stringResource(Res.string.welcome_sample_author_initials),
            authorName = stringResource(Res.string.welcome_sample_author_name),
        )
        WelcomeLayout.CardKind.Question -> HoopQuestionCard(
            question = stringResource(Res.string.welcome_sample_question),
            details = stringResource(Res.string.welcome_sample_question_details),
        )
        WelcomeLayout.CardKind.Video -> HoopMediaCard(painter = painterResource(Res.drawable.welcome_video), isVideo = true)
        WelcomeLayout.CardKind.MarketPhoto -> HoopMediaCard(painter = painterResource(Res.drawable.welcome_photo))
    }
}

private fun HoopBrandIcon.descriptionRes() = when (this) {
    HoopBrandIcon.Google -> Res.string.welcome_continue_with_google
    HoopBrandIcon.Facebook -> Res.string.welcome_continue_with_facebook
    HoopBrandIcon.Apple -> Res.string.welcome_continue_with_apple
}
