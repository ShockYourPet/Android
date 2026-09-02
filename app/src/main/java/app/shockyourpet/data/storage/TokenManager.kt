package app.shockyourpet.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.shockyourpet.data.api.models.AccountConnection
import app.shockyourpet.data.api.models.SharedApiKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class TokenManager(context: Context) {

    private val gson = Gson()
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (_: Exception) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var apiToken: String
        get() = prefs.getString(KEY_PRIMARY_ACCOUNT_TOKEN, "") ?: prefs.getString(KEY_API_TOKEN, "") ?: ""
        set(value) {
            prefs.edit {
                putString(KEY_PRIMARY_ACCOUNT_TOKEN, value)
                putString(KEY_API_TOKEN, value)
            }
        }

    var primaryAccountUsername: String
        get() = prefs.getString(KEY_PRIMARY_ACCOUNT_USERNAME, "") ?: ""
        set(value) = prefs.edit { putString(KEY_PRIMARY_ACCOUNT_USERNAME, value) }

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) {
            val cleanUrl = value.trim()
                .trimEnd('/')
                .replace("openshhock.app", "openshock.app")
            prefs.edit { putString(KEY_SERVER_URL, cleanUrl) }
        }

    var useCustomServer: Boolean
        get() = prefs.getBoolean(KEY_USE_CUSTOM_SERVER, false)
        set(value) = prefs.edit { putBoolean(KEY_USE_CUSTOM_SERVER, value) }

    var developerDebugLogging: Boolean
        get() = prefs.getBoolean(KEY_DEVELOPER_DEBUG_LOGGING, false)
        set(value) = prefs.edit { putBoolean(KEY_DEVELOPER_DEBUG_LOGGING, value) }

    fun getEffectiveServerUrl(): String {
        val url = if (useCustomServer && serverUrl.isNotBlank()) {
            serverUrl
        } else {
            DEFAULT_SERVER_URL
        }
        return url.replace("openshhock.app", "openshock.app")
    }

    // Shared Live Control API Keys
    fun getSharedApiKeys(): List<SharedApiKey> {
        val json = prefs.getString(KEY_SHARED_API_KEYS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SharedApiKey>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveSharedApiKeys(keys: List<SharedApiKey>) {
        val json = gson.toJson(keys)
        prefs.edit { putString(KEY_SHARED_API_KEYS, json) }
    }

    fun addSharedApiKey(name: String, token: String, url: String = DEFAULT_SERVER_URL): SharedApiKey {
        val current = getSharedApiKeys().toMutableList()
        val cleanUrl = url.ifBlank { DEFAULT_SERVER_URL }.replace("openshhock.app", "openshock.app")
        val key = SharedApiKey(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Shared Hub Key ${current.size + 1}" },
            token = token.trim(),
            serverUrl = cleanUrl,
        )
        current.add(key)
        saveSharedApiKeys(current)
        return key
    }

    fun removeSharedApiKey(id: String) {
        val current = getSharedApiKeys().filterNot { it.id == id }
        saveSharedApiKeys(current)
    }

    // Account Connections compatibility helper
    @Suppress("unused")
    fun getAccountConnections(): List<AccountConnection> {
        val shared = getSharedApiKeys()
        if (shared.isNotEmpty()) {
            return shared.map {
                AccountConnection(
                    id = it.id,
                    name = it.name,
                    token = it.token,
                    serverUrl = it.serverUrl,
                )
            }
        }
        if (apiToken.isNotBlank()) {
            val name = primaryAccountUsername.ifBlank { "Primary Account" }
            return listOf(
                AccountConnection(
                    id = "primary",
                    name = name,
                    token = apiToken,
                    serverUrl = serverUrl,
                ),
            )
        }
        return emptyList()
    }

    @Suppress("unused")
    fun saveAccountConnections(connections: List<AccountConnection>) {
        val shared = connections.map {
            SharedApiKey(id = it.id, name = it.name, token = it.token, serverUrl = it.serverUrl)
        }
        saveSharedApiKeys(shared)
    }

    fun updateActiveConnectionName(name: String) {
        if (name.isBlank()) return
        primaryAccountUsername = name
    }

    fun addAccountConnection(name: String, token: String, url: String = DEFAULT_SERVER_URL): AccountConnection {
        val key = addSharedApiKey(name, token, url)
        return AccountConnection(id = key.id, name = key.name, token = key.token, serverUrl = key.serverUrl)
    }

    fun removeAccountConnection(id: String) {
        removeSharedApiKey(id)
    }

    fun getCustomShockerIds(): List<String> {
        val set = prefs.getStringSet(KEY_CUSTOM_SHOCKER_IDS, emptySet()) ?: emptySet()
        return set.toList()
    }

    fun addCustomShockerId(id: String) {
        val cleanId = id.trim()
        if (cleanId.isNotBlank()) {
            val current = getCustomShockerIds().toMutableSet()
            current.add(cleanId)
            prefs.edit { putStringSet(KEY_CUSTOM_SHOCKER_IDS, current) }
        }
    }

    fun removeCustomShockerId(id: String) {
        val current = getCustomShockerIds().toMutableSet()
        current.remove(id.trim())
        prefs.edit { putStringSet(KEY_CUSTOM_SHOCKER_IDS, current) }
    }

    @Suppress("unused")
    fun clear() {
        prefs.edit { clear() }
    }

    companion object {
        private const val PREFS_NAME = "openshock_secure_prefs"
        private const val KEY_API_TOKEN = "key_api_token"
        private const val KEY_PRIMARY_ACCOUNT_TOKEN = "key_primary_account_token"
        private const val KEY_PRIMARY_ACCOUNT_USERNAME = "key_primary_account_username"
        private const val KEY_SHARED_API_KEYS = "key_shared_api_keys"
        private const val KEY_SERVER_URL = "key_server_url"
        private const val KEY_USE_CUSTOM_SERVER = "key_use_custom_server"
        private const val KEY_DEVELOPER_DEBUG_LOGGING = "key_developer_debug_logging"
        @Suppress("unused")
        private const val KEY_ACCOUNT_CONNECTIONS = "key_account_connections"
        private const val KEY_CUSTOM_SHOCKER_IDS = "key_custom_shocker_ids"

        const val DEFAULT_SERVER_URL = "https://api.openshock.app"
    }
}
