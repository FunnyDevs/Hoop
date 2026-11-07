package app.hoop.presentation.onboarding.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import app.hoop.designsystem.component.*
import app.hoop.designsystem.icon.HoopBrandIcon
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.domain.common.DomainError
import app.hoop.presentation.onboarding.common.AuthScreenFrame
import app.hoop.presentation.onboarding.common.ScreenHeading
import app.hoop.presentation.onboarding.messageRes
import app.hoop.presentation.onboarding.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInRoute(
    onSignedIn: () -> Unit,
    onNavigateToRecovery: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateBack: () -> Unit,
    onSocialSignIn: (HoopBrandIcon) -> Unit,
    viewModel: SignInViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SignInEffect.SignedIn -> onSignedIn()
                SignInEffect.NavigateToRecovery -> onNavigateToRecovery()
                SignInEffect.NavigateToSignUp -> onNavigateToSignUp()
                SignInEffect.NavigateBack -> onNavigateBack()
                is SignInEffect.StartSocialSignIn -> onSocialSignIn(effect.provider)
            }
        }
    }
    SignInScreen(state, viewModel::onIntent)
}

@Composable
fun SignInScreen(state: SignInUiState, onIntent: (SignInIntent) -> Unit) {
    val spacing = HoopTheme.spacing
    val hasCredentialsError = state.error == DomainError.InvalidCredentials
    val fieldState = if (hasCredentialsError) HoopFieldState.Error else HoopFieldState.Default

    AuthScreenFrame(
        onBack = { onIntent(SignInIntent.BackClicked) },
        showRings = true,
        actions = {
            HoopButton(stringResource(Res.string.signin_submit), { onIntent(SignInIntent.Submit) }, loading = state.isLoading)
        },
        secondaryActions = {
            Row(Modifier.fillMaxWidth().padding(vertical = spacing.lg - spacing.xxs * 2)) {
                HoopLabeledDivider(stringResource(Res.string.signin_or))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.md, Alignment.CenterHorizontally)) {
                socialProviders().forEach { (provider, description) ->
                    HoopBrandIconButton(provider, description, { onIntent(SignInIntent.SocialClicked(provider)) })
                }
            }
            HoopFooterPrompt(stringResource(Res.string.signin_no_account), stringResource(Res.string.signin_register), { onIntent(SignInIntent.SignUpClicked) })
        },
    ) {
        ScreenHeading(stringResource(Res.string.signin_title), stringResource(Res.string.signin_subtitle))
        if (state.error != null) {
            HoopErrorBanner(stringResource(state.error.messageRes()), Modifier.padding(bottom = spacing.md - spacing.xxs * 2))
        }
        HoopTextField(
            label = stringResource(Res.string.field_email),
            value = state.email,
            onValueChange = { onIntent(SignInIntent.EmailChanged(it)) },
            placeholder = stringResource(Res.string.field_email_placeholder),
            state = fieldState,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(spacing.md - spacing.xxs * 2))
        HoopPasswordField(
            label = stringResource(Res.string.field_password),
            value = state.password,
            onValueChange = { onIntent(SignInIntent.PasswordChanged(it)) },
            visible = state.passwordVisible,
            onVisibilityChange = { onIntent(SignInIntent.PasswordVisibilityChanged(it)) },
            showDescription = stringResource(Res.string.password_show),
            hideDescription = stringResource(Res.string.password_hide),
            placeholder = stringResource(Res.string.signin_password_placeholder),
            state = fieldState,
            keyboardActions = KeyboardActions(onDone = { onIntent(SignInIntent.Submit) }),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            HoopTextLink(stringResource(Res.string.signin_forgot_password), { onIntent(SignInIntent.ForgotPasswordClicked) })
        }
    }
}

@Composable
internal fun socialProviders(): List<Pair<HoopBrandIcon, String>> = listOf(
    HoopBrandIcon.Google to stringResource(Res.string.welcome_continue_with_google),
    HoopBrandIcon.Facebook to stringResource(Res.string.welcome_continue_with_facebook),
    HoopBrandIcon.Apple to stringResource(Res.string.welcome_continue_with_apple),
)
