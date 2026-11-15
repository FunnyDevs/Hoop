package app.hoop.data.remote

import app.hoop.data.session.SessionTokenStore

/**
 * Session token of the signed-in user, added to every request. Every change is written to the
 * [store], so the user stays signed in after the app is closed.
 */
internal class SessionHolder(private val store: SessionTokenStore) {
    private var cached: String? = store.load()

    var token: String?
        get() = cached
        set(value) {
            cached = value
            store.save(value)
        }
}
