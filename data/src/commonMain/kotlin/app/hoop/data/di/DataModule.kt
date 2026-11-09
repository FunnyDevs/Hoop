package app.hoop.data.di

import app.hoop.data.config.ParseConfig
import app.hoop.data.fake.FakeAuthRepository
import app.hoop.data.fake.FakeHomeStore
import app.hoop.data.remote.ParseAuthApi
import app.hoop.data.remote.createParseHttpClient
import app.hoop.data.repository.ParseAuthRepository
import app.hoop.domain.AuthRepository
import app.hoop.domain.FeedRepository
import app.hoop.domain.HoopsRepository
import app.hoop.domain.PostsRepository
import org.koin.dsl.binds
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
    // Hoops, feeds and posts: sample content in memory until the server functions are wired.
    single { FakeHomeStore() } binds arrayOf(HoopsRepository::class, FeedRepository::class, PostsRepository::class)
}
