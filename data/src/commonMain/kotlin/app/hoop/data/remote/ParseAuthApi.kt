package app.hoop.data.remote

import app.hoop.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

/** Error returned by Parse, with its code. */
internal class ParseApiException(val code: Int, message: String) : Exception(message)

internal class ParseAuthApi(private val client: HttpClient) {

    suspend fun login(username: String, password: String): UserDto =
        client.get(ParseProtocol.PATH_LOGIN) {
            parameter(ParseProtocol.PARAM_USERNAME, username)
            parameter(ParseProtocol.PARAM_PASSWORD, password)
        }.parse()

    suspend fun signUp(body: SignUpBody): SignUpResultDto =
        client.post(ParseProtocol.PATH_FUNCTIONS + ParseProtocol.FUNCTION_SIGN_UP) { setBody(body) }
            .parse<FunctionResponse<SignUpResultDto>>().result

    suspend fun checkAvailability(body: AvailabilityBody): AvailabilityDto =
        client.post(ParseProtocol.PATH_FUNCTIONS + ParseProtocol.FUNCTION_CHECK_AVAILABILITY) { setBody(body) }
            .parse<FunctionResponse<AvailabilityDto>>().result

    suspend fun requestPasswordReset(body: PasswordResetBody) {
        client.post(ParseProtocol.PATH_FUNCTIONS + ParseProtocol.FUNCTION_REQUEST_PASSWORD_RESET) { setBody(body) }.ensureSuccess()
    }

    private suspend inline fun <reified T> HttpResponse.parse(): T {
        ensureSuccess()
        return body()
    }

    private suspend fun HttpResponse.ensureSuccess() {
        if (status.isSuccess()) return
        val error = runCatching { body<ParseErrorDto>() }.getOrDefault(ParseErrorDto())
        throw ParseApiException(error.code, error.message)
    }
}
