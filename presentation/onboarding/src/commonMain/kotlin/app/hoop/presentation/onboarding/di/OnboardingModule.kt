package app.hoop.presentation.onboarding.di

import app.hoop.presentation.onboarding.recovery.RecoveryViewModel
import app.hoop.presentation.onboarding.signin.SignInViewModel
import app.hoop.presentation.onboarding.signup.SignUpViewModel
import app.hoop.presentation.onboarding.welcome.WelcomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingPresentationModule = module {
    viewModelOf(::WelcomeViewModel)
    viewModelOf(::SignInViewModel)
    viewModelOf(::SignUpViewModel)
    viewModelOf(::RecoveryViewModel)
}
