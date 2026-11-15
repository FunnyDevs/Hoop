package app.hoop.presentation.onboarding.recovery

import app.hoop.domain.common.DomainError

enum class RecoveryStep { Email, CheckMail }

data class RecoveryUiState(
    val email: String = "",
    val emailError: DomainError? = null,
    val sentTo: String = "",
    /** Seconds before the link can be requested again; 0 means "now". */
    val resendSecondsLeft: Int = 0,
    val isLoading: Boolean = false,
    val error: DomainError? = null,
)

sealed interface RecoveryIntent {
    data class EmailChanged(val value: String) : RecoveryIntent
    data object SendLinkClicked : RecoveryIntent
    data object ResendClicked : RecoveryIntent
    data object OpenMailClicked : RecoveryIntent
    data object SignInClicked : RecoveryIntent
    data object BackClicked : RecoveryIntent
}

sealed interface RecoveryEffect {
    data class NavigateTo(val step: RecoveryStep) : RecoveryEffect
    data object NavigateBack : RecoveryEffect
    data object OpenMailApp : RecoveryEffect
    data object GoToSignIn : RecoveryEffect
}
