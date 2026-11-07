package app.hoop.presentation.onboarding.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class WelcomeViewModel : ViewModel() {
    private val effectChannel = Channel<WelcomeEffect>(Channel.BUFFERED)
    val effects: Flow<WelcomeEffect> = effectChannel.receiveAsFlow()

    fun onIntent(intent: WelcomeIntent) {
        val effect = when (intent) {
            WelcomeIntent.CreateAccountClicked -> WelcomeEffect.NavigateToSignUp
            WelcomeIntent.SignInClicked -> WelcomeEffect.NavigateToSignIn
            is WelcomeIntent.SocialClicked -> WelcomeEffect.StartSocialSignIn(intent.provider)
        }
        viewModelScope.launch { effectChannel.send(effect) }
    }
}
