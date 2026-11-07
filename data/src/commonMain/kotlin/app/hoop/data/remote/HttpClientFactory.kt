package app.hoop.data.remote

import app.hoop.data.config.ParseConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val ParseJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

internal fun createParseHttpClient(config: ParseConfig): HttpClient = HttpClient {
    expectSuccess = false
    install(ContentNegotiation) { json(ParseJson) }
    defaultRequest {
        url(config.baseUrl.trimEnd('/') + "/")
        contentType(ContentType.Application.Json)
        header(ParseProtocol.HEADER_APPLICATION_ID, config.applicationId)
        if (config.clientKey.isNotBlank()) header(ParseProtocol.HEADER_CLIENT_KEY, config.clientKey)
    }
}
