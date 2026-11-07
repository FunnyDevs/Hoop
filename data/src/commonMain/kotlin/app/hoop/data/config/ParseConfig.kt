package app.hoop.data.config

/**
 * Parse server configuration. It comes from outside (build, unversioned local file):
 * the code never contains addresses or keys.
 */
data class ParseConfig(
    val baseUrl: String,
    val applicationId: String,
    val clientKey: String,
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && applicationId.isNotBlank()
}
