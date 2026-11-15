package app.hoop.data.remote

import app.hoop.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

/** Error returned by Parse, with its code. */
internal class ParseApiException(val code: Int, message: String) : Exception(message)

internal class ParseAuthApi(private val client: HttpClient) {

    /** POST /login with the credentials in the JSON body, so the password never ends up in a URL. */
    suspend fun login(username: String, password: String): UserDto =
        client.post(ParseProtocol.PATH_LOGIN) { setBody(LoginBody(username, password)) }.parse()

    suspend fun signUp(body: SignUpBody): SignUpResultDto =
        client.post(ParseProtocol.PATH_FUNCTIONS + ParseProtocol.FUNCTION_SIGN_UP) { setBody(body) }
            .parse<FunctionResponse<SignUpResultDto>>().result

    suspend fun checkAvailability(body: AvailabilityBody): AvailabilityDto =
        client.post(ParseProtocol.PATH_FUNCTIONS + ParseProtocol.FUNCTION_CHECK_AVAILABILITY) { setBody(body) }
            .parse<FunctionResponse<AvailabilityDto>>().result

    /** GET /users/me: the user owning the session token (error 209 if the token is not valid). */
    suspend fun currentUser(): UserDto = client.get(ParseProtocol.PATH_CURRENT_USER).parse()

    /** Cloud Function: like [currentUser] but with the email (the server hides it from the standard endpoints). */
    suspend fun currentUserWithEmail(): UserDto =
        client.post(ParseProtocol.PATH_FUNCTIONS + ParseProtocol.FUNCTION_GET_CURRENT_USER) { setBody(emptyMap<String, String>()) }
            .parse<FunctionResponse<UserDto>>().result

    suspend fun changePassword(body: ChangePasswordBody): SignUpResultDto =
        client.post(ParseProtocol.PATH_FUNCTIONS + ParseProtocol.FUNCTION_CHANGE_PASSWORD) { setBody(body) }
            .parse<FunctionResponse<SignUpResultDto>>().result

    suspend fun logout() {
        client.post(ParseProtocol.PATH_LOGOUT) { setBody(emptyMap<String, String>()) }
    }

    suspend fun requestPasswordReset(body: PasswordResetBody) {
        client.post(ParseProtocol.PATH_REQUEST_PASSWORD_RESET) { setBody(body) }.ensureSuccess()
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
