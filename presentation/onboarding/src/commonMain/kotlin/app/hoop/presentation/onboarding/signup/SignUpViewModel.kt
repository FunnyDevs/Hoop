package app.hoop.presentation.onboarding.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hoop.domain.common.DomainError
import app.hoop.domain.common.HoopPolicy
import app.hoop.domain.common.Violation
import app.hoop.domain.common.fold
import app.hoop.domain.common.getOrNull
import app.hoop.domain.geo.Radius
import app.hoop.domain.identity.Email
import app.hoop.domain.identity.EmailTypoDetector
import app.hoop.domain.identity.Password
import app.hoop.domain.identity.PasswordStrength
import app.hoop.domain.identity.Username
import app.hoop.domain.usecase.CheckEmailAvailabilityUseCase
import app.hoop.domain.usecase.CheckUsernameAvailabilityUseCase
import app.hoop.domain.usecase.RequestLocationUseCase
import app.hoop.domain.usecase.SignUpUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Shared by the five sign-up screens: it is scoped to the sign-up navigation graph. */
@OptIn(FlowPreview::class)
class SignUpViewModel(
    private val checkEmail: CheckEmailAvailabilityUseCase,
    private val checkUsername: CheckUsernameAvailabilityUseCase,
    private val signUp: SignUpUseCase,
    private val requestLocation: RequestLocationUseCase,
    private val typoDetector: EmailTypoDetector,
) : ViewModel() {
    private val _state = MutableStateFlow(SignUpUiState(radiusMeters = Radius.Default.meters))
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private val effectChannel = Channel<SignUpEffect>(Channel.BUFFERED)
    val effects: Flow<SignUpEffect> = effectChannel.receiveAsFlow()

    // A StateFlow keeps the latest query even if the collector has not started yet.
    private val usernameQueries = MutableStateFlow("")

    init {
        viewModelScope.launch {
            usernameQueries.debounce(USERNAME_DEBOUNCE_MILLIS).filter { it.isNotEmpty() }.collectLatest { query -> verifyUsername(query) }
        }
    }

    fun onIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.EmailChanged -> onEmailChanged(intent.value)
            SignUpIntent.EmailSuggestionAccepted -> _state.value.emailSuggestion?.let { onEmailChanged(it) }
            is SignUpIntent.PasswordChanged -> _state.update {
                it.copy(password = intent.value, passwordStrength = PasswordStrength.of(intent.value), passwordError = null)
            }
            is SignUpIntent.PasswordVisibilityChanged -> _state.update { it.copy(passwordVisible = intent.visible) }
            SignUpIntent.CredentialsSubmitted -> submitCredentials()
            is SignUpIntent.UsernameChanged -> onUsernameChanged(intent.value)
            is SignUpIntent.TermsChanged -> _state.update { it.copy(termsAccepted = intent.accepted, termsError = false) }
            SignUpIntent.UsernameSubmitted -> submitUsername()
            SignUpIntent.LocationAllowed -> allowLocation()
            SignUpIntent.LocationSkipped -> emit(SignUpEffect.NavigateTo(SignUpStep.Radius))
            is SignUpIntent.RadiusChanged -> _state.update { it.copy(radiusMeters = intent.meters) }
            SignUpIntent.CreateHoopClicked -> createAccount()
            SignUpIntent.SignInClicked -> emit(SignUpEffect.GoToSignIn)
            SignUpIntent.BackClicked -> emit(SignUpEffect.NavigateBack)
        }
    }

    private fun onEmailChanged(value: String) {
        val suggestion = Email.create(value).getOrNull()?.let { typoDetector.suggest(it) }
        _state.update { it.copy(email = value, emailSuggestion = suggestion, emailError = null) }
    }

    private fun submitCredentials() {
        val current = _state.value
        if (current.isLoading) return
        val email = Email.create(current.email)
        val password = Password.create(current.password)
        if (email.getOrNull() == null || password.getOrNull() == null) {
            _state.update {
                it.copy(
                    emailError = if (email.getOrNull() == null) DomainError.Validation(Violation.EmailInvalid) else null,
                    passwordError = if (password.getOrNull() == null) DomainError.Validation(Violation.PasswordTooShort) else null,
                )
            }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            checkEmail(current.email).fold(
                onSuccess = { availability ->
                    if (availability.emailTaken) {
                        _state.update { it.copy(isLoading = false, emailError = DomainError.EmailAlreadyUsed) }
                    } else {
                        _state.update { it.copy(isLoading = false) }
                        emit(SignUpEffect.NavigateTo(SignUpStep.Username))
                    }
                },
                onFailure = { error -> _state.update { it.copy(isLoading = false, error = error) } },
            )
        }
    }

    private fun onUsernameChanged(value: String) {
        val trimmed = value.trim()
        val status = when {
            trimmed.isEmpty() -> UsernameStatus.Idle
            Username.create(trimmed).getOrNull() == null -> UsernameStatus.Invalid
            else -> UsernameStatus.Checking
        }
        _state.update { it.copy(username = value, usernameStatus = status) }
        if (status == UsernameStatus.Checking) usernameQueries.value = trimmed
    }

    private suspend fun verifyUsername(query: String) {
        checkUsername(query).fold(
            onSuccess = { availability ->
                if (_state.value.username.trim() == query) {
                    _state.update { it.copy(usernameStatus = if (availability.usernameTaken) UsernameStatus.Taken else UsernameStatus.Available) }
                }
            },
            onFailure = { error -> _state.update { it.copy(usernameStatus = UsernameStatus.Idle, error = error) } },
        )
    }

    private fun submitUsername() {
        val current = _state.value
        when {
            current.usernameStatus != UsernameStatus.Available -> Unit
            !current.termsAccepted -> _state.update { it.copy(termsError = true) }
            else -> emit(SignUpEffect.NavigateTo(SignUpStep.Location))
        }
    }

    private fun allowLocation() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            requestLocation().fold(
                onSuccess = { point ->
                    _state.update { it.copy(isLoading = false, location = point) }
                    emit(SignUpEffect.NavigateTo(SignUpStep.Radius))
                },
                onFailure = { error -> _state.update { it.copy(isLoading = false, error = error) } },
            )
        }
    }

    private fun createAccount() {
        val current = _state.value
        if (current.isLoading) return
        val radius = Radius.create(current.radiusMeters.coerceIn(HoopPolicy.RADIUS_MIN_METERS, HoopPolicy.RADIUS_MAX_METERS)).getOrNull() ?: Radius.Default
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            signUp(current.username.trim(), current.email, current.password, radius, current.location).fold(
                onSuccess = { user ->
                    _state.update { it.copy(isLoading = false, createdUsername = user.displayName) }
                    emit(SignUpEffect.AccountCreated(user.displayName, radius.meters))
                },
                onFailure = { error -> _state.update { it.copy(isLoading = false, error = error) } },
            )
        }
    }

    private fun emit(effect: SignUpEffect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }

    private companion object {
        const val USERNAME_DEBOUNCE_MILLIS = 350L
    }
}
