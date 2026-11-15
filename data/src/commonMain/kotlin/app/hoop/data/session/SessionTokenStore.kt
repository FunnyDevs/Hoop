package app.hoop.data.session

/**
 * Where the session token of the signed-in user is kept between app launches.
 * Implemented per platform with the system secure storage (Android Keystore, iOS Keychain),
 * so the token is never written to disk in plain text.
 */
interface SessionTokenStore {
    fun load(): String?
    fun save(token: String?)
}

/** Keeps the token in memory only: used when no platform store is provided (tests, fake backend). */
class InMemorySessionTokenStore : SessionTokenStore {
    private var token: String? = null
    override fun load(): String? = token
    override fun save(token: String?) {
        this.token = token
    }
}
