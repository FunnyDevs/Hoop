package app.hoop.presentation.onboarding.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import app.hoop.designsystem.icon.HoopBrandIcon
import app.hoop.presentation.onboarding.OnboardingConfig
import app.hoop.presentation.onboarding.recovery.*
import app.hoop.presentation.onboarding.signin.SignInRoute
import app.hoop.presentation.onboarding.signup.*
import app.hoop.presentation.onboarding.welcome.WelcomeRoute
import kotlinx.coroutines.flow.Flow
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Navigation destinations of the onboarding flows. */
object OnboardingRoute {
    const val WELCOME = "welcome"
    const val SIGN_IN = "sign_in"

    const val SIGN_UP_GRAPH = "sign_up"
    private const val SIGN_UP_CREDENTIALS = "sign_up/credentials"
    private const val SIGN_UP_USERNAME = "sign_up/username"
    private const val SIGN_UP_LOCATION = "sign_up/location"
    private const val SIGN_UP_RADIUS = "sign_up/radius"
    const val SIGN_UP_DONE = "sign_up_done"
    private const val ARG_USERNAME = "username"
    private const val ARG_RADIUS = "radius"
    private const val SIGN_UP_DONE_PATTERN = "$SIGN_UP_DONE/{$ARG_USERNAME}/{$ARG_RADIUS}"

    const val RECOVERY_GRAPH = "recovery"
    private const val RECOVERY_EMAIL = "recovery/email"
    private const val RECOVERY_CHECK_MAIL = "recovery/check_mail"
    private const val RECOVERY_NEW_PASSWORD = "recovery/new_password"
    const val RECOVERY_DONE = "recovery_done"

    internal fun signUpStep(step: SignUpStep) = when (step) {
        SignUpStep.Credentials -> SIGN_UP_CREDENTIALS
        SignUpStep.Username -> SIGN_UP_USERNAME
        SignUpStep.Location -> SIGN_UP_LOCATION
        SignUpStep.Radius -> SIGN_UP_RADIUS
    }

    internal fun recoveryStep(step: RecoveryStep) = when (step) {
        RecoveryStep.Email -> RECOVERY_EMAIL
        RecoveryStep.CheckMail -> RECOVERY_CHECK_MAIL
        RecoveryStep.NewPassword -> RECOVERY_NEW_PASSWORD
    }

    internal fun signUpDone(username: String, radiusMeters: Int) = "$SIGN_UP_DONE/$username/$radiusMeters"

    internal const val SIGN_UP_START = SIGN_UP_CREDENTIALS
    internal const val RECOVERY_START = RECOVERY_EMAIL
    internal const val SIGN_UP_DONE_ROUTE = SIGN_UP_DONE_PATTERN
    internal const val ARG_USERNAME_KEY = ARG_USERNAME
    internal const val ARG_RADIUS_KEY = ARG_RADIUS
}

/**
 * Adds every onboarding destination to a NavHost: welcome, sign in, sign up (4 steps + done)
 * and password recovery (3 steps + done). Calls back into the app for what lives outside onboarding.
 */
fun NavGraphBuilder.onboardingGraph(
    navController: NavHostController,
    onEnterApp: () -> Unit,
    onSocialSignIn: (HoopBrandIcon) -> Unit,
    onOpenMailApp: () -> Unit,
) {
    composable(OnboardingRoute.WELCOME) {
        WelcomeRoute(
            onNavigateToSignUp = { navController.navigate(OnboardingRoute.SIGN_UP_GRAPH) },
            onNavigateToSignIn = { navController.navigate(OnboardingRoute.SIGN_IN) },
            onSocialSignIn = onSocialSignIn,
        )
    }

    composable(OnboardingRoute.SIGN_IN) {
        SignInRoute(
            onSignedIn = onEnterApp,
            onNavigateToRecovery = { navController.navigate(OnboardingRoute.RECOVERY_GRAPH) },
            onNavigateToSignUp = { navController.navigate(OnboardingRoute.SIGN_UP_GRAPH) },
            onNavigateBack = { navController.popBackStack() },
            onSocialSignIn = onSocialSignIn,
        )
    }

    navigation(startDestination = OnboardingRoute.SIGN_UP_START, route = OnboardingRoute.SIGN_UP_GRAPH) {
        val handle: (SignUpEffect) -> Unit = { effect ->
            when (effect) {
                is SignUpEffect.NavigateTo -> navController.navigate(OnboardingRoute.signUpStep(effect.step))
                SignUpEffect.NavigateBack -> navController.popBackStack()
                SignUpEffect.GoToSignIn -> navController.navigate(OnboardingRoute.SIGN_IN) {
                    popUpTo(OnboardingRoute.WELCOME)
                }
                is SignUpEffect.AccountCreated -> navController.navigate(OnboardingRoute.signUpDone(effect.username, effect.radiusMeters)) {
                    popUpTo(OnboardingRoute.SIGN_UP_GRAPH) { inclusive = true }
                }
            }
        }
        composable(OnboardingRoute.SIGN_UP_START) { entry ->
            val vm = graphViewModel<SignUpViewModel>(navController, entry, OnboardingRoute.SIGN_UP_GRAPH)
            Collect(vm.effects, handle)
            SignUpCredentialsScreen(vm.state.collectAsState().value, vm::onIntent)
        }
        composable(OnboardingRoute.signUpStep(SignUpStep.Username)) { entry ->
            val vm = graphViewModel<SignUpViewModel>(navController, entry, OnboardingRoute.SIGN_UP_GRAPH)
            Collect(vm.effects, handle)
            SignUpUsernameScreen(vm.state.collectAsState().value, vm::onIntent)
        }
        composable(OnboardingRoute.signUpStep(SignUpStep.Location)) { entry ->
            val vm = graphViewModel<SignUpViewModel>(navController, entry, OnboardingRoute.SIGN_UP_GRAPH)
            Collect(vm.effects, handle)
            SignUpLocationScreen(vm.state.collectAsState().value, vm::onIntent)
        }
        composable(OnboardingRoute.signUpStep(SignUpStep.Radius)) { entry ->
            val vm = graphViewModel<SignUpViewModel>(navController, entry, OnboardingRoute.SIGN_UP_GRAPH)
            Collect(vm.effects, handle)
            val config: OnboardingConfig = koinInject()
            SignUpRadiusScreen(vm.state.collectAsState().value, config, vm::onIntent)
        }
    }

    composable(
        OnboardingRoute.SIGN_UP_DONE_ROUTE,
        arguments = listOf(
            navArgument(OnboardingRoute.ARG_USERNAME_KEY) { type = NavType.StringType },
            navArgument(OnboardingRoute.ARG_RADIUS_KEY) { type = NavType.IntType },
        ),
    ) { entry ->
        SignUpDoneScreen(
            username = entry.arguments?.getString(OnboardingRoute.ARG_USERNAME_KEY).orEmpty(),
            radiusMeters = entry.arguments?.getInt(OnboardingRoute.ARG_RADIUS_KEY) ?: 0,
            onEnter = onEnterApp,
        )
    }

    navigation(startDestination = OnboardingRoute.RECOVERY_START, route = OnboardingRoute.RECOVERY_GRAPH) {
        val handle: (RecoveryEffect) -> Unit = { effect ->
            when (effect) {
                is RecoveryEffect.NavigateTo -> navController.navigate(OnboardingRoute.recoveryStep(effect.step))
                RecoveryEffect.NavigateBack -> navController.popBackStack()
                RecoveryEffect.OpenMailApp -> onOpenMailApp()
                RecoveryEffect.PasswordChanged -> navController.navigate(OnboardingRoute.RECOVERY_DONE) {
                    popUpTo(OnboardingRoute.RECOVERY_GRAPH) { inclusive = true }
                }
                RecoveryEffect.GoToSignIn -> navController.navigate(OnboardingRoute.SIGN_IN) { popUpTo(OnboardingRoute.WELCOME) }
            }
        }
        composable(OnboardingRoute.RECOVERY_START) { entry ->
            val vm = graphViewModel<RecoveryViewModel>(navController, entry, OnboardingRoute.RECOVERY_GRAPH)
            Collect(vm.effects, handle)
            RecoveryEmailScreen(vm.state.collectAsState().value, vm::onIntent)
        }
        composable(OnboardingRoute.recoveryStep(RecoveryStep.CheckMail)) { entry ->
            val vm = graphViewModel<RecoveryViewModel>(navController, entry, OnboardingRoute.RECOVERY_GRAPH)
            val config: OnboardingConfig = koinInject()
            Collect(vm.effects, handle)
            RecoveryCheckMailScreen(vm.state.collectAsState().value, config.showResetLinkSimulator, vm::onIntent)
        }
        composable(OnboardingRoute.recoveryStep(RecoveryStep.NewPassword)) { entry ->
            val vm = graphViewModel<RecoveryViewModel>(navController, entry, OnboardingRoute.RECOVERY_GRAPH)
            Collect(vm.effects, handle)
            RecoveryNewPasswordScreen(vm.state.collectAsState().value, vm::onIntent)
        }
    }

    composable(OnboardingRoute.RECOVERY_DONE) {
        RecoveryDoneScreen(onSignIn = {
            navController.navigate(OnboardingRoute.SIGN_IN) { popUpTo(OnboardingRoute.WELCOME) }
        })
    }
}

/** A ViewModel shared by all the screens of a navigation graph: scoped to the graph's back stack entry. */
@Composable
private inline fun <reified VM : ViewModel> graphViewModel(navController: NavHostController, entry: NavBackStackEntry, graphRoute: String): VM {
    val parent = remember(entry) { navController.getBackStackEntry(graphRoute) }
    return koinViewModel(viewModelStoreOwner = parent)
}

@Composable
private fun <T> Collect(flow: Flow<T>, handler: (T) -> Unit) {
    LaunchedEffect(flow) { flow.collect { handler(it) } }
}
