package app.shockyourpet.data.api

import okhttp3.Request

object OpenShockAuth {
    const val TOKEN_HEADER = "OpenShockToken"
    const val TOKEN_HEADER_ALIAS = "Open-Shock-Token"
    const val USER_AGENT = "ShockYourPet-Android/1.0"
    const val EXPECTED_TOKEN_LENGTH = 64

    private val uuidPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun sanitizeToken(raw: String): String {
        return raw
            .trim()
            .replace("\n", "")
            .replace("\r", "")
            .removePrefix("Bearer ")
            .removePrefix("bearer ")
            .trim('"', '\'')
            .trim()
    }

    fun validateTokenFormat(token: String): String? {
        if (token.isBlank()) return "API token is required"
        if (uuidPattern.matches(token)) {
            return "That looks like a token or shocker UUID, not the API token secret. Create a token on OpenShock and copy the $EXPECTED_TOKEN_LENGTH-character secret shown once."
        }
        if (token.length < 32) {
            return "API token looks too short. Copy the full secret from OpenShock API Tokens (usually $EXPECTED_TOKEN_LENGTH characters)."
        }
        return null
    }
}

fun Request.Builder.withOpenShockAuth(token: String): Request.Builder {
    header("User-Agent", OpenShockAuth.USER_AGENT)
    val clean = OpenShockAuth.sanitizeToken(token)
    if (clean.isNotBlank()) {
        header(OpenShockAuth.TOKEN_HEADER, clean)
        header(OpenShockAuth.TOKEN_HEADER_ALIAS, clean)
    }
    return this
}
