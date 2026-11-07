package app.hoop.presentation.onboarding

import app.hoop.domain.common.AuthPolicy
import app.hoop.domain.common.DomainError
import app.hoop.domain.identity.EmailTypoDetector
import app.hoop.domain.usecase.*
import app.hoop.presentation.onboarding.recovery.*
import app.hoop.presentation.onboarding.signin.*
import app.hoop.presentation.onboarding.signup.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = TestAuthRepository()
    private val location = TestLocationProvider()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun signIn() = SignInViewModel(SignInUseCase(repository))
    private fun signUp() = SignUpViewModel(
        CheckEmailAvailabilityUseCase(repository), CheckUsernameAvailabilityUseCase(repository),
        SignUpUseCase(repository), RequestLocationUseCase(location), EmailTypoDetector(),
    )
    private fun recovery() = RecoveryViewModel(RequestPasswordResetUseCase(repository), ConfirmPasswordResetUseCase(repository))

    @Test fun signInWithWrongCredentialsShowsError() = runTest(dispatcher) {
        repository.wrongLogin = "errore@hoop.app"
        val vm = signIn()
        vm.onIntent(SignInIntent.EmailChanged("errore@hoop.app"))
        vm.onIntent(SignInIntent.PasswordChanged("password123"))
        vm.onIntent(SignInIntent.Submit)
        advanceUntilIdle()
        assertEquals(DomainError.InvalidCredentials, vm.state.value.error)
        assertTrue(!vm.state.value.isLoading)
    }

    @Test fun signInSuccessEmitsEffect() = runTest(dispatcher) {
        val vm = signIn()
        vm.onIntent(SignInIntent.EmailChanged("demo@hoop.app"))
        vm.onIntent(SignInIntent.PasswordChanged("password123"))
        vm.onIntent(SignInIntent.Submit)
        advanceUntilIdle()
        assertEquals(SignInEffect.SignedIn, vm.effects.first())
    }

    @Test fun signUpSuggestsTypoFix() = runTest(dispatcher) {
        val vm = signUp()
        vm.onIntent(SignUpIntent.EmailChanged("luca@gmial.com"))
        assertEquals("luca@gmail.com", vm.state.value.emailSuggestion)
        vm.onIntent(SignUpIntent.EmailSuggestionAccepted)
        assertEquals("luca@gmail.com", vm.state.value.email)
        assertNull(vm.state.value.emailSuggestion)
    }

    @Test fun signUpRejectsTakenEmail() = runTest(dispatcher) {
        repository.takenEmails = setOf("mario@hoop.app")
        val vm = signUp()
        vm.onIntent(SignUpIntent.EmailChanged("mario@hoop.app"))
        vm.onIntent(SignUpIntent.PasswordChanged("hoop2026!"))
        vm.onIntent(SignUpIntent.CredentialsSubmitted)
        advanceUntilIdle()
        assertEquals(DomainError.EmailAlreadyUsed, vm.state.value.emailError)
    }

    @Test fun signUpMovesToUsernameWhenEmailIsFree() = runTest(dispatcher) {
        val vm = signUp()
        vm.onIntent(SignUpIntent.EmailChanged("marta@hoop.app"))
        vm.onIntent(SignUpIntent.PasswordChanged("hoop2026!"))
        vm.onIntent(SignUpIntent.CredentialsSubmitted)
        advanceUntilIdle()
        assertEquals(SignUpEffect.NavigateTo(SignUpStep.Username), vm.effects.first())
    }

    @Test fun usernameAvailabilityIsCheckedAfterDebounce() = runTest(dispatcher) {
        repository.takenUsernames = setOf("luca")
        val vm = signUp()
        vm.onIntent(SignUpIntent.UsernameChanged("marta.rossi"))
        assertEquals(UsernameStatus.Checking, vm.state.value.usernameStatus)
        advanceUntilIdle()
        assertEquals(UsernameStatus.Available, vm.state.value.usernameStatus)
        vm.onIntent(SignUpIntent.UsernameChanged("luca"))
        advanceUntilIdle()
        assertEquals(UsernameStatus.Taken, vm.state.value.usernameStatus)
        vm.onIntent(SignUpIntent.UsernameChanged("a b"))
        assertEquals(UsernameStatus.Invalid, vm.state.value.usernameStatus)
    }

    @Test fun termsMustBeAcceptedToContinue() = runTest(dispatcher) {
        val vm = signUp()
        vm.onIntent(SignUpIntent.UsernameChanged("marta.rossi"))
        advanceUntilIdle()
        vm.onIntent(SignUpIntent.UsernameSubmitted)
        assertTrue(vm.state.value.termsError)
        vm.onIntent(SignUpIntent.TermsChanged(true))
        vm.onIntent(SignUpIntent.UsernameSubmitted)
        advanceUntilIdle()
        assertEquals(SignUpEffect.NavigateTo(SignUpStep.Location), vm.effects.first())
    }

    @Test fun creatingTheAccountEmitsAccountCreated() = runTest(dispatcher) {
        val vm = signUp()
        vm.onIntent(SignUpIntent.EmailChanged("marta@hoop.app"))
        vm.onIntent(SignUpIntent.PasswordChanged("hoop2026!"))
        vm.onIntent(SignUpIntent.UsernameChanged("marta.rossi"))
        vm.onIntent(SignUpIntent.RadiusChanged(2500))
        vm.onIntent(SignUpIntent.CreateHoopClicked)
        advanceUntilIdle()
        assertEquals(SignUpEffect.AccountCreated("marta.rossi", 2500), vm.effects.first())
    }

    @Test fun grantedLocationIsKeptAndMovesToRadius() = runTest(dispatcher) {
        val vm = signUp()
        vm.onIntent(SignUpIntent.LocationAllowed)
        advanceUntilIdle()
        assertEquals(45.0, vm.state.value.location?.latitude)
        assertEquals(SignUpEffect.NavigateTo(SignUpStep.Radius), vm.effects.first())
    }

    @Test fun deniedLocationShowsErrorAndStaysOnTheScreen() = runTest(dispatcher) {
        location.result = app.hoop.domain.common.DomainResult.failure(DomainError.LocationPermissionDenied)
        val vm = signUp()
        vm.onIntent(SignUpIntent.LocationAllowed)
        advanceUntilIdle()
        assertEquals(DomainError.LocationPermissionDenied, vm.state.value.error)
        assertNull(vm.state.value.location)
    }

    @Test fun recoveryCountsDownBeforeAllowingResend() = runTest(dispatcher) {
        val vm = recovery()
        vm.onIntent(RecoveryIntent.EmailChanged("Marta@Esempio.it"))
        vm.onIntent(RecoveryIntent.SendLinkClicked)
        advanceTimeBy(1)
        assertEquals("marta@esempio.it", vm.state.value.sentTo)
        assertEquals(AuthPolicy.RESEND_RESET_AFTER_SECONDS, vm.state.value.resendSecondsLeft)
        vm.onIntent(RecoveryIntent.ResendClicked)
        advanceTimeBy(1)
        assertEquals(1, repository.resetRequests) // resend is ignored while counting down
        advanceTimeBy(AuthPolicy.RESEND_RESET_AFTER_SECONDS * 1000L + 1)
        assertEquals(0, vm.state.value.resendSecondsLeft)
        vm.onIntent(RecoveryIntent.ResendClicked)
        advanceTimeBy(1)
        assertEquals(2, repository.resetRequests)
    }

    @Test fun recoveryRejectsInvalidEmail() = runTest(dispatcher) {
        val vm = recovery()
        vm.onIntent(RecoveryIntent.EmailChanged("non-una-email"))
        vm.onIntent(RecoveryIntent.SendLinkClicked)
        advanceUntilIdle()
        assertTrue(vm.state.value.emailError != null)
        assertEquals(0, repository.resetRequests)
    }

    @Test fun newPasswordMustMatchBeforeSaving() = runTest(dispatcher) {
        val vm = recovery()
        vm.onIntent(RecoveryIntent.NewPasswordChanged("hoop2026!"))
        vm.onIntent(RecoveryIntent.RepeatPasswordChanged("diversa"))
        vm.onIntent(RecoveryIntent.SavePasswordClicked)
        advanceUntilIdle()
        assertNull(repository.confirmedPassword)
        vm.onIntent(RecoveryIntent.RepeatPasswordChanged("hoop2026!"))
        vm.onIntent(RecoveryIntent.SavePasswordClicked)
        advanceUntilIdle()
        assertEquals("hoop2026!", repository.confirmedPassword)
        assertEquals(RecoveryEffect.PasswordChanged, vm.effects.first())
    }
}
