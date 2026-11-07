package app.hoop.presentation.onboarding.recovery

import app.hoop.domain.common.DomainError
import app.hoop.domain.identity.PasswordStrength

enum class RecoveryStep { Email, CheckMail, NewPassword }

data class RecoveryUiState(
    val email: String = "",
    val emailError: DomainError? = null,
    val sentTo: String = "",
    /** Seconds before the link can be requested again; 0 means "now". */
    val resendSecondsLeft: Int = 0,
    val newPassword: String = "",
    val repeatPassword: String = "",
    val passwordVisible: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.Empty,
    val passwordError: DomainError? = null,
    val isLoading: Boolean = false,
    val error: DomainError? = null,
) {
    val passwordsMatch: Boolean get() = repeatPassword.isNotEmpty() && repeatPassword == newPassword
}

sealed interface RecoveryIntent {
    data class EmailChanged(val value: String) : RecoveryIntent
    data object SendLinkClicked : RecoveryIntent
    data object ResendClicked : RecoveryIntent
    data object OpenMailClicked : RecoveryIntent
    data object SimulateLinkClicked : RecoveryIntent
    data class NewPasswordChanged(val value: String) : RecoveryIntent
    data class RepeatPasswordChanged(val value: String) : RecoveryIntent
    data class PasswordVisibilityChanged(val visible: Boolean) : RecoveryIntent
    data object SavePasswordClicked : RecoveryIntent
    data object SignInClicked : RecoveryIntent
    data object BackClicked : RecoveryIntent
}

sealed interface RecoveryEffect {
    data class NavigateTo(val step: RecoveryStep) : RecoveryEffect
    data object NavigateBack : RecoveryEffect
    data object OpenMailApp : RecoveryEffect
    data object PasswordChanged : RecoveryEffect
    data object GoToSignIn : RecoveryEffect
}
