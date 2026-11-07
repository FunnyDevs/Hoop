package app.hoop.presentation.onboarding.signup

import app.hoop.domain.common.DomainError
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.identity.PasswordStrength

enum class SignUpStep { Credentials, Username, Location, Radius }

enum class UsernameStatus { Idle, Checking, Available, Taken, Invalid }

data class SignUpUiState(
    val email: String = "",
    val emailSuggestion: String? = null,
    val emailError: DomainError? = null,
    val password: String = "",
    val passwordVisible: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.Empty,
    val passwordError: DomainError? = null,
    val username: String = "",
    val usernameStatus: UsernameStatus = UsernameStatus.Idle,
    val termsAccepted: Boolean = false,
    val termsError: Boolean = false,
    val location: GeoPoint? = null,
    val radiusMeters: Int,
    val isLoading: Boolean = false,
    val error: DomainError? = null,
    val createdUsername: String? = null,
)

sealed interface SignUpIntent {
    data class EmailChanged(val value: String) : SignUpIntent
    data object EmailSuggestionAccepted : SignUpIntent
    data class PasswordChanged(val value: String) : SignUpIntent
    data class PasswordVisibilityChanged(val visible: Boolean) : SignUpIntent
    data object CredentialsSubmitted : SignUpIntent
    data class UsernameChanged(val value: String) : SignUpIntent
    data class TermsChanged(val accepted: Boolean) : SignUpIntent
    data object UsernameSubmitted : SignUpIntent
    data object LocationAllowed : SignUpIntent
    data object LocationSkipped : SignUpIntent
    data class RadiusChanged(val meters: Int) : SignUpIntent
    data object CreateHoopClicked : SignUpIntent
    data object SignInClicked : SignUpIntent
    data object BackClicked : SignUpIntent
}

sealed interface SignUpEffect {
    data class NavigateTo(val step: SignUpStep) : SignUpEffect
    data object NavigateBack : SignUpEffect
    data object GoToSignIn : SignUpEffect
    data class AccountCreated(val username: String, val radiusMeters: Int) : SignUpEffect
}
