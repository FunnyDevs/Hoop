package app.hoop.di

import app.hoop.config.platformMapConfig
import app.hoop.config.platformParseConfig
import app.hoop.data.di.dataModule
import app.hoop.presentation.onboarding.di.onboardingPresentationModule
import co.touchlab.kermit.Logger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import app.hoop.presentation.home.HomeConfig
import app.hoop.presentation.home.di.homePresentationModule
import app.hoop.presentation.onboarding.OnboardingConfig

/**
 * Composition root: wires the domain, data and presentation modules plus the [platformModules]
 * (device services such as location). Safe to call more than once.
 */
fun initKoin(platformModules: List<Module> = emptyList()) {
    if (GlobalContext.getOrNull() != null) return
    val parse = platformParseConfig()
    // First line to look for when the app "cannot reach the server": which backend this build uses.
    Logger.withTag("Hoop").i { if (parse.isConfigured) "Backend: Parse at ${parse.baseUrl}" else "Backend: FAKE (no server configured for this flavor)" }
    startKoin {
        modules(
            domainModule,
            *platformModules.toTypedArray(),
            dataModule(platformParseConfig()),
            onboardingPresentationModule,
            homePresentationModule,
            module {
                // The reset-link simulator is only useful while the backend is a mock.
                val map = platformMapConfig()
                single { HomeConfig(map.styleUri, map.fallbackLatitude, map.fallbackLongitude) }
                single {
                    OnboardingConfig(
                        mapStyleUri = map.styleUri,
                        fallbackLatitude = map.fallbackLatitude,
                        fallbackLongitude = map.fallbackLongitude,
                    )
                }
            },
        )
    }
}
