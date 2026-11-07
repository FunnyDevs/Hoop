package app.hoop.domain.identity

import app.hoop.domain.common.AuthPolicy

/** Domain Service: suggests the corrected address when the domain looks like a typo (gmial.com → gmail.com). */
class EmailTypoDetector(private val knownDomains: List<String> = DEFAULT_DOMAINS) {

    /** Suggested corrected email, or null if the domain is known or resembles none. */
    fun suggest(email: Email): String? {
        val domain = email.domain
        if (domain in knownDomains) return null
        val best = knownDomains
            .map { it to distance(domain, it) }
            .filter { it.second in 1..AuthPolicy.EMAIL_TYPO_MAX_DISTANCE }
            .minByOrNull { it.second }
            ?: return null
        return email.value.substringBeforeLast('@') + "@" + best.first
    }

    private fun distance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost)
            }
            previous = current
        }
        return previous[b.length]
    }

    companion object {
        val DEFAULT_DOMAINS = listOf(
            "gmail.com", "hotmail.com", "hotmail.it", "outlook.com", "outlook.it",
            "yahoo.com", "yahoo.it", "icloud.com", "libero.it", "virgilio.it",
            "live.com", "tiscali.it", "alice.it", "tim.it", "proton.me", "duck.com",
        )
    }
}
