package app.hoop.data.di

import app.hoop.data.config.ParseConfig
import app.hoop.data.local.InMemoryPendingPostsRepository
import app.hoop.data.local.SqlDriverFactory
import app.hoop.data.local.SqlPendingPostsRepository
import app.hoop.data.media.MediaFileReader
import app.hoop.data.remote.ParsePostsApi
import app.hoop.data.remote.SessionHolder
import app.hoop.data.fake.FakeAuthRepository
import app.hoop.data.fake.FakeHomeStore
import app.hoop.data.remote.ParseAuthApi
import app.hoop.data.remote.createParseHttpClient
import app.hoop.data.repository.ParseAuthRepository
import app.hoop.data.repository.ParsePostsRepository
import app.hoop.data.session.InMemorySessionTokenStore
import app.hoop.data.session.SessionTokenStore
import app.hoop.domain.AuthRepository
import app.hoop.domain.FeedRepository
import app.hoop.domain.HoopsRepository
import app.hoop.domain.PendingPostsRepository
import app.hoop.domain.PostsRepository
import org.koin.dsl.binds
import org.koin.dsl.module

/**
 * Binds the domain interfaces to their implementations.
 * If the server is not configured it uses the fake repository, so the app stays testable.
 */
fun dataModule(config: ParseConfig) = module {
    // Secure storage comes from the platform module; without one the session lasts until the app closes.
    single { SessionHolder(getOrNull<SessionTokenStore>() ?: InMemorySessionTokenStore()) }
    single { createParseHttpClient(config, get()) }
    single<AuthRepository> {
        if (config.isConfigured) ParseAuthRepository(ParseAuthApi(get()), get()) else FakeAuthRepository(getOrNull<SessionTokenStore>() ?: InMemorySessionTokenStore())
    }
    // Hoops and feeds: sample content in memory until the server functions are wired.
    single { FakeHomeStore() } binds arrayOf(HoopsRepository::class, FeedRepository::class)
    // Posts waiting to be sent live in the local database when the platform provides one.
    single<PendingPostsRepository> {
        getOrNull<SqlDriverFactory>()?.let { SqlPendingPostsRepository(it.create()) } ?: InMemoryPendingPostsRepository()
    }
    // Posts: published on the server when it is configured (reading local files needs the platform [MediaFileReader]).
    single<PostsRepository> {
        val reader = getOrNull<MediaFileReader>()
        if (config.isConfigured && reader != null) ParsePostsRepository(ParsePostsApi(get()), reader) else get<FakeHomeStore>()
    }
}
