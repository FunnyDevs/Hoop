package app.hoop.data.remote

import app.hoop.data.HoopLog
import app.hoop.data.config.ParseConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val ParseJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** [engine] is only given by tests; production uses the platform engine. */
internal fun createParseHttpClient(config: ParseConfig, session: SessionHolder, engine: HttpClientEngine? = null): HttpClient =
    if (engine == null) HttpClient(parseClientConfig(config, session)) else HttpClient(engine, parseClientConfig(config, session))

private fun parseClientConfig(config: ParseConfig, session: SessionHolder): HttpClientConfig<*>.() -> Unit = {
    expectSuccess = false
    install(ContentNegotiation) { json(ParseJson) }
    // Request line and status only (INFO): bodies hold passwords and headers hold the session token.
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) = HoopLog.http.d { message }
        }
        level = LogLevel.INFO
        sanitizeHeader { header -> header in SENSITIVE_HEADERS }
    }
    defaultRequest {
        url(config.baseUrl.trimEnd('/') + "/")
        contentType(ContentType.Application.Json)
        header(ParseProtocol.HEADER_APPLICATION_ID, config.applicationId)
        if (config.clientKey.isNotBlank()) header(ParseProtocol.HEADER_CLIENT_KEY, config.clientKey)
        session.token?.let { header(ParseProtocol.HEADER_SESSION_TOKEN, it) }
    }
}

private val SENSITIVE_HEADERS = setOf(
    ParseProtocol.HEADER_SESSION_TOKEN,
    ParseProtocol.HEADER_CLIENT_KEY,
    ParseProtocol.HEADER_APPLICATION_ID,
)
