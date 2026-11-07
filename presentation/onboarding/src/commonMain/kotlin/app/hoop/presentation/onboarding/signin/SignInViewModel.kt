package app.hoop.presentation.onboarding.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hoop.domain.common.fold
import app.hoop.domain.usecase.SignInUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel(private val signIn: SignInUseCase) : ViewModel() {
    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    private val effectChannel = Channel<SignInEffect>(Channel.BUFFERED)
    val effects: Flow<SignInEffect> = effectChannel.receiveAsFlow()

    fun onIntent(intent: SignInIntent) {
        when (intent) {
            is SignInIntent.EmailChanged -> _state.update { it.copy(email = intent.value, error = null) }
            is SignInIntent.PasswordChanged -> _state.update { it.copy(password = intent.value, error = null) }
            is SignInIntent.PasswordVisibilityChanged -> _state.update { it.copy(passwordVisible = intent.visible) }
            SignInIntent.Submit -> submit()
            SignInIntent.ForgotPasswordClicked -> emit(SignInEffect.NavigateToRecovery)
            SignInIntent.SignUpClicked -> emit(SignInEffect.NavigateToSignUp)
            SignInIntent.BackClicked -> emit(SignInEffect.NavigateBack)
            is SignInIntent.SocialClicked -> emit(SignInEffect.StartSocialSignIn(intent.provider))
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            signIn(current.email, current.password).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    emit(SignInEffect.SignedIn)
                },
                onFailure = { error -> _state.update { it.copy(isLoading = false, error = error) } },
            )
        }
    }

    private fun emit(effect: SignInEffect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }
}
