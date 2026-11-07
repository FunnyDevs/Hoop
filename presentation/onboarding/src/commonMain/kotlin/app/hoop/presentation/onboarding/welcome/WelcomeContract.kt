package app.hoop.presentation.onboarding.welcome

import app.hoop.designsystem.icon.HoopBrandIcon

/** User actions on the welcome screen. */
sealed interface WelcomeIntent {
    data object CreateAccountClicked : WelcomeIntent
    data object SignInClicked : WelcomeIntent
    data class SocialClicked(val provider: HoopBrandIcon) : WelcomeIntent
}

/** One-off events towards navigation. */
sealed interface WelcomeEffect {
    data object NavigateToSignUp : WelcomeEffect
    data object NavigateToSignIn : WelcomeEffect
    data class StartSocialSignIn(val provider: HoopBrandIcon) : WelcomeEffect
}
