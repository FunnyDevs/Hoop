package app.hoop.session

import app.hoop.data.session.SessionTokenStore
import platform.Foundation.NSUserDefaults

/**
 * Keeps the session token in NSUserDefaults. Written but never compiled, like the rest of the iOS
 * code (Xcode is not installed on the development machine).
 *
 * TODO: move it to the Keychain (SecItemAdd / SecItemCopyMatching) before the iOS release:
 * NSUserDefaults is not encrypted. The Keychain needs the Security cinterop, which is better
 * written with the compiler at hand.
 */
class IosSessionTokenStore : SessionTokenStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun load(): String? = defaults.stringForKey(KEY_TOKEN)

    override fun save(token: String?) {
        if (token == null) defaults.removeObjectForKey(KEY_TOKEN) else defaults.setObject(token, forKey = KEY_TOKEN)
    }

    private companion object {
        const val KEY_TOKEN = "app.hoop.session.token"
    }
}
