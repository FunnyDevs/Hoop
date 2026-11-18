package app.hoop.data.remote

import app.hoop.data.remote.dto.ParseErrorDto
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

/** Error returned by Parse, with its code. */
internal class ParseApiException(val code: Int, message: String) : Exception(message)

/** Throws [ParseApiException] with the code and message of Parse when the answer is not a success. */
internal suspend fun HttpResponse.ensureSuccess() {
    if (status.isSuccess()) return
    // The rate limiter of the server answers with the HTTP status only, not with a Parse code.
    if (status == HttpStatusCode.TooManyRequests) throw ParseApiException(ParseProtocol.ERROR_TOO_MANY_REQUESTS, "Too many requests")
    val error = runCatching { body<ParseErrorDto>() }.getOrDefault(ParseErrorDto())
    throw ParseApiException(error.code, error.message)
}

/** Checks the answer and reads its body. */
internal suspend inline fun <reified T> HttpResponse.parse(): T {
    ensureSuccess()
    return body()
}
