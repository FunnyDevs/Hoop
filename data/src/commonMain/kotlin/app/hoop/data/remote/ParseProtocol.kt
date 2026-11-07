package app.hoop.data.remote

/** Names of the Parse REST protocol and of the Hoop server Cloud Functions, in one place. */
internal object ParseProtocol {
    const val HEADER_APPLICATION_ID = "X-Parse-Application-Id"
    const val HEADER_CLIENT_KEY = "X-Parse-REST-API-Key"
    const val HEADER_SESSION_TOKEN = "X-Parse-Session-Token"

    const val PATH_LOGIN = "login"
    const val PATH_FUNCTIONS = "functions/"
    const val PARAM_USERNAME = "username"
    const val PARAM_PASSWORD = "password"

    const val FUNCTION_SIGN_UP = "signUp"
    const val FUNCTION_CHECK_AVAILABILITY = "checkAvailability"
    const val FUNCTION_REQUEST_PASSWORD_RESET = "requestPasswordReset"

    // Parse error codes
    const val ERROR_INVALID_LOGIN = 101
    const val ERROR_USERNAME_TAKEN = 202
    const val ERROR_EMAIL_TAKEN = 203
    const val ERROR_INVALID_EMAIL = 125
}
