package app.hoop.presentation.onboarding.signin

import app.hoop.designsystem.icon.HoopBrandIcon
import app.hoop.domain.common.DomainError

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: DomainError? = null,
)

sealed interface SignInIntent {
    data class EmailChanged(val value: String) : SignInIntent
    data class PasswordChanged(val value: String) : SignInIntent
    data class PasswordVisibilityChanged(val visible: Boolean) : SignInIntent
    data object Submit : SignInIntent
    data object ForgotPasswordClicked : SignInIntent
    data object SignUpClicked : SignInIntent
    data object BackClicked : SignInIntent
    data class SocialClicked(val provider: HoopBrandIcon) : SignInIntent
}

sealed interface SignInEffect {
    data object SignedIn : SignInEffect
    data object NavigateToRecovery : SignInEffect
    data object NavigateToSignUp : SignInEffect
    data object NavigateBack : SignInEffect
    data class StartSocialSignIn(val provider: HoopBrandIcon) : SignInEffect
}
