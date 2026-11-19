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
    const val PATH_FILES = "files/"

    const val FUNCTION_SIGN_UP = "signUp"
    const val FUNCTION_CHECK_AVAILABILITY = "checkAvailability"
    /** Returns the signed-in user with the email, which the standard /login and /users/me answers leave out. */
    const val FUNCTION_GET_CURRENT_USER = "getCurrentUser"
    const val FUNCTION_CHANGE_PASSWORD = "changeUserPassword"
    const val FUNCTION_SAVE_NEW_POST = "saveNewPost"
    const val FUNCTION_GET_USER_HOOPS = "getUserHoops"
    const val FUNCTION_CREATE_HOOP = "createHoop"
    const val FUNCTION_UPDATE_HOOP = "updateHoop"
    const val FUNCTION_DELETE_HOOPS = "deleteHoops"
    const val FUNCTION_SEARCH_POSTS = "searchForPostsByPosition"
    const val FUNCTION_LOAD_COMMENTS = "loadComments"
    const val FUNCTION_SAVE_COMMENT = "saveComment"
    const val FUNCTION_GET_NOTIFICATIONS = "getNotifications"

    // Parse error codes
    const val ERROR_INVALID_LOGIN = 101
    const val ERROR_USERNAME_TAKEN = 202
    const val ERROR_EMAIL_TAKEN = 203
    const val ERROR_INVALID_EMAIL = 125
    const val ERROR_INVALID_SESSION = 209
    /** A Cloud Function that needs a user, called without a session token. */
    const val ERROR_VALIDATION = 141
    /** Not a Parse code: it stands for the HTTP status 429 of the rate limiter. */
    const val ERROR_TOO_MANY_REQUESTS = 429
    /** Hoop server code: the current password given to change it is wrong. */
    const val ERROR_WRONG_PASSWORD = 5001
    /** Hoop server codes of publishing: type not accepted by the Hoop, user too far from it, Hoop unknown. */
    const val ERROR_POST_NOT_ALLOWED = 5003
    const val ERROR_OUTSIDE_HOOP = 5004
    const val ERROR_HOOP_NOT_FOUND = 5005
}
