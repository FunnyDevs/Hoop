package app.hoop.presentation.onboarding.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.hoop.presentation.onboarding.OnboardingConfig
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import app.hoop.designsystem.component.*
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.domain.common.HoopPolicy
import app.hoop.presentation.onboarding.common.*
import app.hoop.presentation.onboarding.messageRes
import app.hoop.presentation.onboarding.resources.*
import org.jetbrains.compose.resources.stringResource

private const val SIGNUP_STEPS = 4
private const val RADIUS_SLIDER_MIN_METERS = 500
private const val RADIUS_SLIDER_MAX_METERS = 10_000
private const val RADIUS_SLIDER_STEP_METERS = 100

/** Step 1 of 4: email and password. */
@Composable
fun SignUpCredentialsScreen(state: SignUpUiState, onIntent: (SignUpIntent) -> Unit) {
    val spacing = HoopTheme.spacing
    AuthScreenFrame(
        onBack = { onIntent(SignUpIntent.BackClicked) },
        progress = StepProgress(1, SIGNUP_STEPS),
        actions = {
            HoopButton(stringResource(Res.string.signup_continue), { onIntent(SignUpIntent.CredentialsSubmitted) }, loading = state.isLoading)
        },
        secondaryActions = {
            HoopFooterPrompt(stringResource(Res.string.signup_have_account), stringResource(Res.string.signup_sign_in), { onIntent(SignUpIntent.SignInClicked) })
        },
    ) {
        ScreenHeading(stringResource(Res.string.signup_credentials_title), stringResource(Res.string.signup_credentials_subtitle))
        if (state.error != null && state.emailError == null) {
            HoopErrorBanner(stringResource(state.error.messageRes()), Modifier.padding(bottom = spacing.md))
        }
        HoopTextField(
            label = stringResource(Res.string.field_email),
            value = state.email,
            onValueChange = { onIntent(SignUpIntent.EmailChanged(it)) },
            placeholder = stringResource(Res.string.field_email_placeholder),
            state = if (state.emailError != null) HoopFieldState.Error else HoopFieldState.Default,
            supportingText = state.emailError?.let { stringResource(it.messageRes()) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        if (state.emailSuggestion != null) {
            Row(Modifier.padding(top = spacing.sm - spacing.xxs, start = spacing.xxs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(stringResource(Res.string.signup_typo_prompt), style = HoopTheme.typography.labelMedium, color = HoopTheme.colors.onSurfaceVariant)
                HoopChip(state.emailSuggestion, { onIntent(SignUpIntent.EmailSuggestionAccepted) })
                Text(stringResource(Res.string.signup_typo_suffix), style = HoopTheme.typography.labelMedium, color = HoopTheme.colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(spacing.md))
        HoopPasswordField(
            label = stringResource(Res.string.field_password),
            value = state.password,
            onValueChange = { onIntent(SignUpIntent.PasswordChanged(it)) },
            visible = state.passwordVisible,
            onVisibilityChange = { onIntent(SignUpIntent.PasswordVisibilityChanged(it)) },
            showDescription = stringResource(Res.string.password_show),
            hideDescription = stringResource(Res.string.password_hide),
            state = if (state.passwordError != null) HoopFieldState.Error else HoopFieldState.Default,
            supportingText = state.passwordError?.let { stringResource(it.messageRes()) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onIntent(SignUpIntent.CredentialsSubmitted) }),
        )
        Spacer(Modifier.height(spacing.sm))
        HoopStrengthMeter(state.passwordStrength.level(), STRENGTH_LEVELS, state.passwordStrength.color(), state.passwordStrength.caption())
    }
}

/** Step 2 of 4: username and terms. */
@Composable
fun SignUpUsernameScreen(state: SignUpUiState, onIntent: (SignUpIntent) -> Unit) {
    val spacing = HoopTheme.spacing
    val name = state.username.trim()
    val fieldState = when (state.usernameStatus) {
        UsernameStatus.Available -> HoopFieldState.Success
        UsernameStatus.Taken, UsernameStatus.Invalid -> HoopFieldState.Error
        else -> HoopFieldState.Default
    }
    val message = when (state.usernameStatus) {
        UsernameStatus.Available -> stringResource(Res.string.username_available, name)
        UsernameStatus.Taken -> stringResource(Res.string.username_taken, name)
        UsernameStatus.Invalid -> stringResource(Res.string.violation_username_invalid)
        UsernameStatus.Checking -> stringResource(Res.string.username_checking)
        UsernameStatus.Idle -> null
    }
    AuthScreenFrame(
        onBack = { onIntent(SignUpIntent.BackClicked) },
        progress = StepProgress(2, SIGNUP_STEPS),
        actions = {
            HoopButton(
                stringResource(Res.string.signup_continue),
                { onIntent(SignUpIntent.UsernameSubmitted) },
                enabled = state.usernameStatus == UsernameStatus.Available,
            )
        },
    ) {
        ScreenHeading(stringResource(Res.string.signup_username_title), stringResource(Res.string.signup_username_subtitle))
        HoopTextField(
            label = stringResource(Res.string.field_username),
            value = state.username,
            onValueChange = { onIntent(SignUpIntent.UsernameChanged(it)) },
            state = fieldState,
            supportingText = message,
            successContentDescription = stringResource(Res.string.username_available_description),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
        )
        Spacer(Modifier.height(spacing.lg - spacing.xxs * 2))
        HoopCheckboxRow(state.termsAccepted, { onIntent(SignUpIntent.TermsChanged(it)) }, isError = state.termsError) {
            Text(
                hoopLinkedText(
                    stringResource(Res.string.terms_prefix) to null,
                    stringResource(Res.string.terms_terms) to { /* TODO: open the terms */ },
                    stringResource(Res.string.terms_and) to null,
                    stringResource(Res.string.terms_privacy) to { /* TODO: open the privacy policy */ },
                    stringResource(Res.string.terms_suffix) to null,
                ),
                style = HoopTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = HoopTheme.colors.onSurfaceVariant,
            )
        }
        if (state.termsError) {
            Text(stringResource(Res.string.terms_required), Modifier.padding(top = spacing.xs), style = HoopTheme.typography.labelMedium, color = HoopTheme.colors.error)
        }
    }
}

/** Step 3 of 4: location permission. */
@Composable
fun SignUpLocationScreen(state: SignUpUiState, onIntent: (SignUpIntent) -> Unit) {
    val spacing = HoopTheme.spacing
    Box(Modifier.fillMaxSize().background(HoopTheme.colors.surface)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = spacing.screenGutter), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md - spacing.xxs * 2)) {
                HoopIconButton(HoopIcons.ArrowBack, stringResource(Res.string.action_back), { onIntent(SignUpIntent.BackClicked) })
                HoopProgressSegments(SIGNUP_STEPS, 3, stringResource(Res.string.progress_step, 3, SIGNUP_STEPS), Modifier.weight(1f))
            }
            Box(Modifier.padding(top = spacing.md - spacing.xxs * 2)) { HoopLocationMap() }
            Column(Modifier.weight(1f).padding(horizontal = spacing.screenGutter)) {
                ScreenHeading(stringResource(Res.string.location_title), stringResource(Res.string.location_body))
                if (state.error != null) {
                    HoopErrorBanner(stringResource(state.error.messageRes()), Modifier.padding(top = spacing.md))
                }
            }
            Column(Modifier.padding(horizontal = spacing.screenGutter).padding(bottom = spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                HoopButton(stringResource(Res.string.location_allow), { onIntent(SignUpIntent.LocationAllowed) }, loading = state.isLoading)
                HoopButton(stringResource(Res.string.location_not_now), { onIntent(SignUpIntent.LocationSkipped) }, style = HoopButtonStyle.Plain)
            }
        }
    }
}

/** Step 4 of 4: radius of the first Hoop. */
@Composable
fun SignUpRadiusScreen(state: SignUpUiState, config: OnboardingConfig, onIntent: (SignUpIntent) -> Unit) {
    val spacing = HoopTheme.spacing
    val range = RADIUS_SLIDER_MIN_METERS.toFloat()..RADIUS_SLIDER_MAX_METERS.toFloat()
    val fraction = (state.radiusMeters - RADIUS_SLIDER_MIN_METERS).toFloat() / (RADIUS_SLIDER_MAX_METERS - RADIUS_SLIDER_MIN_METERS)
    val short = isShorterThanKilometer(state.radiusMeters)
    val density = LocalDensity.current
    var sheetHeight by remember { mutableStateOf(0.dp) }
    BoxWithConstraints(Modifier.fillMaxSize().background(HoopTheme.colors.mapLand)) {
        HoopRadiusMap(
            latitude = state.location?.latitude ?: config.fallbackLatitude,
            longitude = state.location?.longitude ?: config.fallbackLongitude,
            radiusMeters = state.radiusMeters,
            styleUri = config.mapStyleUri,
            contentPadding = PaddingValues(bottom = sheetHeight),
            mapWidth = maxWidth,
            fitFraction = RADIUS_MAP_FIT_MIN + (RADIUS_MAP_FIT_MAX - RADIUS_MAP_FIT_MIN) * fraction,
        )
        HoopIconButton(
            HoopIcons.ArrowBack, stringResource(Res.string.action_back), { onIntent(SignUpIntent.BackClicked) },
            Modifier.statusBarsPadding().padding(start = spacing.md, top = spacing.sm), style = HoopIconButtonStyle.Elevated,
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .onSizeChanged { sheetHeight = with(density) { it.height.toDp() } }
                .shadow(HoopTheme.elevation.card, HoopTheme.shapes.sheet)
                .background(HoopTheme.colors.surface, HoopTheme.shapes.sheet)
                .padding(start = spacing.screenGutter, end = spacing.screenGutter, top = spacing.md + spacing.xs, bottom = spacing.md + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        ) {
            HoopProgressSegments(SIGNUP_STEPS, SIGNUP_STEPS, stringResource(Res.string.progress_step, SIGNUP_STEPS, SIGNUP_STEPS))
            Row(Modifier.padding(top = spacing.lg - spacing.xxs * 2), verticalAlignment = Alignment.Bottom) {
                Text(
                    if (short) "${state.radiusMeters}" else formatKilometers(state.radiusMeters),
                    style = HoopTheme.typography.displayLarge.copy(fontSize = HoopTheme.typography.displayLarge.fontSize * RADIUS_VALUE_SCALE),
                    color = HoopTheme.colors.onSurface,
                )
                Text(
                    stringResource(if (short) Res.string.radius_unit_m else Res.string.radius_unit_km),
                    Modifier.padding(start = spacing.xs, bottom = spacing.xs),
                    style = HoopTheme.typography.bodyLarge,
                    color = HoopTheme.colors.onSurfaceVariant,
                )
            }
            Text(
                buildAnnotatedString {
                    append(stringResource(Res.string.radius_explainer_before))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = HoopTheme.colors.onSurface)) { append(stringResource(Res.string.radius_home_name)) }
                    append(stringResource(Res.string.radius_explainer_after))
                },
                Modifier.padding(top = spacing.sm),
                style = HoopTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = HoopTheme.colors.onSurfaceVariant,
            )
            HoopSlider(
                value = state.radiusMeters.toFloat(),
                onValueChange = { onIntent(SignUpIntent.RadiusChanged(((it / RADIUS_SLIDER_STEP_METERS).toInt() * RADIUS_SLIDER_STEP_METERS).coerceIn(HoopPolicy.RADIUS_MIN_METERS, HoopPolicy.RADIUS_MAX_METERS))) },
                valueRange = range,
                description = stringResource(Res.string.radius_slider_description),
                modifier = Modifier.padding(top = spacing.sm),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(Res.string.distance_meters, RADIUS_SLIDER_MIN_METERS), style = HoopTheme.typography.caption, color = HoopTheme.colors.onSurfaceVariant)
                Text(stringResource(Res.string.distance_kilometers, formatKilometers(RADIUS_SLIDER_MAX_METERS)), style = HoopTheme.typography.caption, color = HoopTheme.colors.onSurfaceVariant)
            }
            if (state.error != null) {
                HoopErrorBanner(stringResource(state.error.messageRes()), Modifier.padding(top = spacing.sm))
            }
            Spacer(Modifier.height(spacing.md))
            HoopButton(stringResource(Res.string.radius_create), { onIntent(SignUpIntent.CreateHoopClicked) }, loading = state.isLoading)
        }
    }
}

private const val RADIUS_VALUE_SCALE = 1.3f

/** Share of the map width taken by the circle: it grows with the slider so the change is visible. */
private const val RADIUS_MAP_FIT_MIN = 0.4f
private const val RADIUS_MAP_FIT_MAX = 0.8f

/** Final screen: the account and the first Hoop are ready. */
@Composable
fun SignUpDoneScreen(username: String, radiusMeters: Int, onEnter: () -> Unit) {
    val radius = if (isShorterThanKilometer(radiusMeters)) stringResource(Res.string.distance_meters, radiusMeters)
    else stringResource(Res.string.distance_kilometers, formatKilometers(radiusMeters))
    DoneScreenFrame(
        badgeColor = HoopTheme.colors.brand,
        ringColor = HoopTheme.colors.accent,
        title = stringResource(Res.string.done_title, username),
        body = stringResource(Res.string.done_body, radius),
        actionLabel = stringResource(Res.string.done_enter),
        onAction = onEnter,
    )
}
