package app.hoop.data.remote

/** Names of the Parse REST protocol and of the Hoop server Cloud Functions, in one place. */
internal object ParseProtocol {
    const val HEADER_APPLICATION_ID = "X-Parse-Application-Id"
    const val HEADER_CLIENT_KEY = "X-Parse-REST-API-Key"
    const val HEADER_SESSION_TOKEN = "X-Parse-Session-Token"

    const val PATH_LOGIN = "login"
    const val PATH_LOGOUT = "logout"
    /** Native Parse endpoint: always answers 200, even for unknown emails. */
    const val PATH_REQUEST_PASSWORD_RESET = "requestPasswordReset"
    const val PATH_FUNCTIONS = "functions/"
    const val PATH_CURRENT_USER = "users/me"

    const val FUNCTION_SIGN_UP = "signUp"
    const val FUNCTION_CHECK_AVAILABILITY = "checkAvailability"
    /** Returns the signed-in user with the email, which the standard /login and /users/me answers leave out. */
    const val FUNCTION_GET_CURRENT_USER = "getCurrentUser"
    const val FUNCTION_CHANGE_PASSWORD = "changeUserPassword"

    // Parse error codes
    const val ERROR_INVALID_LOGIN = 101
    const val ERROR_USERNAME_TAKEN = 202
    const val ERROR_EMAIL_TAKEN = 203
    const val ERROR_INVALID_EMAIL = 125
    const val ERROR_INVALID_SESSION = 209
    /** Hoop server code: the current password given to change it is wrong. */
    const val ERROR_WRONG_PASSWORD = 5001
}
