package app.hoop.di

import app.hoop.config.platformMapConfig
import app.hoop.config.platformParseConfig
import app.hoop.data.di.dataModule
import app.hoop.presentation.onboarding.di.onboardingPresentationModule
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
                        showResetLinkSimulator = !platformParseConfig().isConfigured,
                        mapStyleUri = map.styleUri,
                        fallbackLatitude = map.fallbackLatitude,
                        fallbackLongitude = map.fallbackLongitude,
                    )
                }
            },
        )
    }
}
