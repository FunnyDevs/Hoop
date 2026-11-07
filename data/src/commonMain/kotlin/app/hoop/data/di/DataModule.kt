package app.hoop.data.di

import app.hoop.data.config.ParseConfig
import app.hoop.data.fake.FakeAuthRepository
import app.hoop.data.remote.ParseAuthApi
import app.hoop.data.remote.createParseHttpClient
import app.hoop.data.repository.ParseAuthRepository
import app.hoop.domain.AuthRepository
import org.koin.dsl.module

/**
 * Binds the domain interfaces to their implementations.
 * If the server is not configured it uses the fake repository, so the app stays testable.
 */
fun dataModule(config: ParseConfig) = module {
    single<AuthRepository> {
        if (config.isConfigured) ParseAuthRepository(ParseAuthApi(createParseHttpClient(config)))
        else FakeAuthRepository()
    }
}
