package app.hoop.presentation.onboarding.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hoop.domain.common.AuthPolicy
import app.hoop.domain.common.DomainError
import app.hoop.domain.common.Violation
import app.hoop.domain.common.fold
import app.hoop.domain.common.getOrNull
import app.hoop.domain.identity.Email
import app.hoop.domain.identity.Password
import app.hoop.domain.identity.PasswordStrength
import app.hoop.domain.usecase.ConfirmPasswordResetUseCase
import app.hoop.domain.usecase.RequestPasswordResetUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Shared by the four recovery screens: it is scoped to the recovery navigation graph. */
class RecoveryViewModel(
    private val requestReset: RequestPasswordResetUseCase,
    private val confirmReset: ConfirmPasswordResetUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(RecoveryUiState())
    val state: StateFlow<RecoveryUiState> = _state.asStateFlow()

    private val effectChannel = Channel<RecoveryEffect>(Channel.BUFFERED)
    val effects: Flow<RecoveryEffect> = effectChannel.receiveAsFlow()

    private var countdown: Job? = null

    fun onIntent(intent: RecoveryIntent) {
        when (intent) {
            is RecoveryIntent.EmailChanged -> _state.update { it.copy(email = intent.value, emailError = null) }
            RecoveryIntent.SendLinkClicked -> sendLink(navigateOnSuccess = true)
            RecoveryIntent.ResendClicked -> if (_state.value.resendSecondsLeft == 0) sendLink(navigateOnSuccess = false)
            RecoveryIntent.OpenMailClicked -> emit(RecoveryEffect.OpenMailApp)
            RecoveryIntent.SimulateLinkClicked -> emit(RecoveryEffect.NavigateTo(RecoveryStep.NewPassword))
            is RecoveryIntent.NewPasswordChanged -> _state.update {
                it.copy(newPassword = intent.value, passwordStrength = PasswordStrength.of(intent.value), passwordError = null)
            }
            is RecoveryIntent.RepeatPasswordChanged -> _state.update { it.copy(repeatPassword = intent.value, passwordError = null) }
            is RecoveryIntent.PasswordVisibilityChanged -> _state.update { it.copy(passwordVisible = intent.visible) }
            RecoveryIntent.SavePasswordClicked -> savePassword()
            RecoveryIntent.SignInClicked -> emit(RecoveryEffect.GoToSignIn)
            RecoveryIntent.BackClicked -> emit(RecoveryEffect.NavigateBack)
        }
    }

    private fun sendLink(navigateOnSuccess: Boolean) {
        val current = _state.value
        if (current.isLoading) return
        if (Email.create(current.email).getOrNull() == null) {
            _state.update { it.copy(emailError = DomainError.Validation(Violation.EmailInvalid)) }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            requestReset(current.email).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, sentTo = current.email.trim().lowercase()) }
                    startCountdown()
                    if (navigateOnSuccess) emit(RecoveryEffect.NavigateTo(RecoveryStep.CheckMail))
                },
                onFailure = { error -> _state.update { it.copy(isLoading = false, emailError = error) } },
            )
        }
    }

    private fun startCountdown() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (left in AuthPolicy.RESEND_RESET_AFTER_SECONDS downTo 0) {
                _state.update { it.copy(resendSecondsLeft = left) }
                if (left > 0) delay(ONE_SECOND_MILLIS)
            }
        }
    }

    private fun savePassword() {
        val current = _state.value
        if (current.isLoading) return
        if (Password.create(current.newPassword).getOrNull() == null) {
            _state.update { it.copy(passwordError = DomainError.Validation(Violation.PasswordTooShort)) }
            return
        }
        if (!current.passwordsMatch) return // the screen already marks the mismatch and disables the button
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            confirmReset(current.newPassword).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    emit(RecoveryEffect.PasswordChanged)
                },
                onFailure = { error -> _state.update { it.copy(isLoading = false, error = error) } },
            )
        }
    }

    private fun emit(effect: RecoveryEffect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }

    private companion object {
        const val ONE_SECOND_MILLIS = 1_000L
    }
}
