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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.hoop.home.HomePlaceholderScreen
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
    val navController = rememberNavController()
    val uriHandler = LocalUriHandler.current
    NavHost(
        navController = navController,
        startDestination = OnboardingRoute.WELCOME,
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
        composable(HoopRoute.HOME) { HomePlaceholderScreen() }
    }
}
