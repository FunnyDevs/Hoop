package app.hoop.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.compose.koinInject
import app.hoop.domain.usecase.RestoreSessionUseCase
import app.hoop.domain.usecase.SignOutUseCase
import kotlinx.coroutines.launch
import app.hoop.presentation.home.HomeRoute
import app.hoop.presentation.onboarding.navigation.OnboardingRoute
import app.hoop.presentation.onboarding.navigation.onboardingGraph

/** Destinations owned by the app shell (everything else belongs to feature modules). */
internal object HoopRoute {
    const val HOME = "home"
}

/** Spring-based screen transitions: a short slide combined with a fade. */
private object NavTransitions {
    private const val SLIDE_FRACTION = 4
    private val slideSpec = spring<androidx.compose.ui.unit.IntOffset>(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = androidx.compose.ui.unit.IntOffset(1, 1))
    private val fadeSpec = spring<Float>(stiffness = Spring.StiffnessMedium)

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { slideInHorizontally(slideSpec) { it / SLIDE_FRACTION } + fadeIn(fadeSpec) }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { slideOutHorizontally(slideSpec) { -it / SLIDE_FRACTION } + fadeOut(fadeSpec) }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { slideInHorizontally(slideSpec) { -it / SLIDE_FRACTION } + fadeIn(fadeSpec) }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { slideOutHorizontally(slideSpec) { it / SLIDE_FRACTION } + fadeOut(fadeSpec) }
}

private const val MAIL_SCHEME = "mailto:"

@Composable
internal fun HoopNavHost(modifier: Modifier = Modifier) {
    // The start screen depends on the saved session: until it is known the themed background shows.
    val restoreSession = koinInject<RestoreSessionUseCase>()
    val signOut = koinInject<SignOutUseCase>()
    val scope = rememberCoroutineScope()
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        startDestination = if (restoreSession()) HoopRoute.HOME else OnboardingRoute.WELCOME
    }
    val start = startDestination
    if (start == null) {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    val navController = rememberNavController()
    val uriHandler = LocalUriHandler.current
    NavHost(
        navController = navController,
        startDestination = start,
        modifier = modifier,
        enterTransition = NavTransitions.enter,
        exitTransition = NavTransitions.exit,
        popEnterTransition = NavTransitions.popEnter,
        popExitTransition = NavTransitions.popExit,
    ) {
        onboardingGraph(
            navController = navController,
            onEnterApp = {
                navController.navigate(HoopRoute.HOME) { popUpTo(OnboardingRoute.WELCOME) { inclusive = true } }
            },
            onSocialSignIn = { /* TODO: sign in with Google, Facebook, Apple */ },
            onOpenMailApp = { runCatching { uriHandler.openUri(MAIL_SCHEME) } },
        )
        composable(HoopRoute.HOME) {
            HomeRoute(
                onSignInRequested = {
                    // The session is no longer valid: sign out here and go to the welcome screen. The waiting posts stay on the device.
                    scope.launch {
                        signOut()
                        navController.navigate(OnboardingRoute.WELCOME) { popUpTo(HoopRoute.HOME) { inclusive = true } }
                    }
                },
            )
        }
    }
}
