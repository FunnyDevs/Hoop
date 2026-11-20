package app.hoop.data.di

import app.cash.sqldelight.db.SqlDriver
import app.hoop.data.config.ParseConfig
import app.hoop.data.local.InMemoryHomeCacheRepository
import app.hoop.data.local.InMemoryPendingPostsRepository
import app.hoop.data.local.SqlDriverFactory
import app.hoop.data.local.SqlHomeCacheRepository
import app.hoop.data.local.SqlPendingPostsRepository
import app.hoop.data.media.MediaFileReader
import app.hoop.data.remote.ParsePostsApi
import app.hoop.data.remote.SessionHolder
import app.hoop.data.fake.FakeAuthRepository
import app.hoop.data.remote.ParseAuthApi
import app.hoop.data.remote.createParseHttpClient
import app.hoop.data.repository.ParseAuthRepository
import app.hoop.data.remote.ParseHomeApi
import app.hoop.data.repository.ParseFeedRepository
import app.hoop.data.repository.ParseHoopsRepository
import app.hoop.data.repository.ParsePostsRepository
import app.hoop.data.session.InMemorySessionTokenStore
import app.hoop.data.session.SessionTokenStore
import app.hoop.domain.AuthRepository
import app.hoop.domain.FeedRepository
import app.hoop.domain.HomeCacheRepository
import app.hoop.domain.HoopsRepository
import app.hoop.domain.PendingPostsRepository
import app.hoop.domain.PostsRepository
import org.koin.dsl.module

/**
 * Binds the domain interfaces to their implementations.
 * Without a configured server only sign-in has a fake (so the app opens); Hoops, feeds and posts need the server
 * and fail with a network error.
 */
fun dataModule(config: ParseConfig) = module {
    // Secure storage comes from the platform module; without one the session lasts until the app closes.
    single { SessionHolder(getOrNull<SessionTokenStore>() ?: InMemorySessionTokenStore()) }
    single { createParseHttpClient(config, get()) }
    single<AuthRepository> {
        if (config.isConfigured) ParseAuthRepository(ParseAuthApi(get()), get()) else FakeAuthRepository(getOrNull<SessionTokenStore>() ?: InMemorySessionTokenStore())
    }
    // Hoops, feeds and posts always come from the server: there is no sample content.
    single { ParseHomeApi(get()) }
    single<HoopsRepository> { ParseHoopsRepository(get()) }
    single<FeedRepository> { ParseFeedRepository(get()) }
    // One local database, shared by the waiting posts and the saved Home, when the platform provides one.
    single { LocalDatabase(getOrNull<SqlDriverFactory>()?.create()) }
    single<PendingPostsRepository> { get<LocalDatabase>().driver?.let { SqlPendingPostsRepository(it) } ?: InMemoryPendingPostsRepository() }
    single<HomeCacheRepository> { get<LocalDatabase>().driver?.let { SqlHomeCacheRepository(it) } ?: InMemoryHomeCacheRepository() }
    // Posts are published on the server (reading local files needs the platform [MediaFileReader]).
    single<PostsRepository> { ParsePostsRepository(ParsePostsApi(get()), get<MediaFileReader>()) }
}

/** The one connection to the local database; `null` when the platform provides none. */
private class LocalDatabase(val driver: SqlDriver?)
