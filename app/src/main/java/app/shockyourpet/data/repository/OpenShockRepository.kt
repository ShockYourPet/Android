package app.shockyourpet.data.repository

import app.shockyourpet.data.api.OpenShockApiClient
import app.shockyourpet.data.api.OpenShockApiService
import app.shockyourpet.data.api.OpenShockAuth
import app.shockyourpet.data.api.models.AccountConnection
import app.shockyourpet.data.api.models.CommandLogEntry
import app.shockyourpet.data.api.models.ControlPreset
import app.shockyourpet.data.api.models.ControlRequest
import app.shockyourpet.data.api.models.ControlShockerPayload
import app.shockyourpet.data.api.models.LcgGatewayInfo
import app.shockyourpet.data.api.models.LoginRequest
import app.shockyourpet.data.api.models.ShockerDevice
import app.shockyourpet.data.api.models.ShockerItem
import app.shockyourpet.data.api.models.UserData
import app.shockyourpet.data.livecontrol.LiveControlClient
import app.shockyourpet.data.livecontrol.ShockerListParser
import app.shockyourpet.data.storage.TokenManager
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.ResponseBody
import retrofit2.Response
import java.net.UnknownHostException
import java.util.UUID

class OpenShockRepository(
    val tokenManager: TokenManager,
    private val apiClient: OpenShockApiClient,
) {

    private val _commandLogs = MutableStateFlow<List<CommandLogEntry>>(emptyList())
    val commandLogs: StateFlow<List<CommandLogEntry>> = _commandLogs.asStateFlow()

    fun clearLogs() {
        _commandLogs.value = emptyList()
    }

    private val _presets = MutableStateFlow<List<ControlPreset>>(emptyList())

    @Suppress("unused")
    val presets: StateFlow<List<ControlPreset>> = _presets.asStateFlow()

    fun invalidateApiClient() {
        apiClient.invalidateCache()
    }

    suspend fun validateToken(): Result<UserData> {
        val cleaned = OpenShockAuth.sanitizeToken(tokenManager.apiToken)
        OpenShockAuth.validateTokenFormat(cleaned)?.let { return Result.failure(Exception(it)) }
        if (cleaned != tokenManager.apiToken) {
            tokenManager.apiToken = cleaned
            apiClient.invalidateCache()
        }

        return try {
            val api = apiClient.getApiService()

            val calls = listOf<suspend () -> Response<ResponseBody>>(
                { api.getToken2Self() },
                { api.getToken1Self() },
            )

            var lastCode = 0
            var lastErrorBody = ""

            for (call in calls) {
                try {
                    val response = call()
                    lastCode = response.code()
                    if (response.isSuccessful) {
                        val bodyString = response.body()?.string() ?: ""
                        val tokenData = parseTokenSelfData(bodyString)
                        if (!tokenData.hasShockersUse) {
                            return Result.failure(
                                Exception("Token is valid but missing shockers.use permission. Edit the token on OpenShock and enable Use Shockers."),
                            )
                        }

                        val profileResponse = api.getUser1Self()
                        val user = if (profileResponse.isSuccessful) {
                            parseUserSelfData(profileResponse.body()?.string().orEmpty(), tokenData)
                        } else {
                            tokenData.copy(
                                username = tokenManager.primaryAccountUsername.ifBlank { "Connected account" },
                            )
                        }
                        return Result.success(user)
                    } else {
                        lastErrorBody = response.errorBody()?.string() ?: ""
                        if ((response.code() == 401) || (response.code() == 403)) {
                            val msg = parseErrorMessage(lastErrorBody, response.code())
                            return Result.failure(Exception(msg))
                        }
                    }
                } catch (e: Exception) {
                    (e as? UnknownHostException)?.let { throw it }
                    // Continue to next fallback route
                }
            }

            val errorMsg = parseErrorMessage(lastErrorBody, lastCode)
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            val msg = if (e is UnknownHostException) {
                "Unable to resolve host ${e.message}. Check URL in Settings (e.g. https://api.openshock.app)"
            } else {
                e.message ?: "Connection error"
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun login(email: String, pass: String): Result<String> {
        return try {
            val api = apiClient.getApiService()
            val req = LoginRequest(email, pass)

            var response = api.login2(req)
            if (!response.isSuccessful) {
                response = api.login1(req)
            }

            if (response.isSuccessful) {
                val bodyString = response.body()?.string() ?: ""
                val token = parseToken(bodyString)
                if (!token.isNullOrBlank()) {
                    tokenManager.apiToken = token
                    apiClient.invalidateCache()
                    Result.success(token)
                } else {
                    Result.failure(Exception("Login succeeded but no token was returned"))
                }
            } else {
                val rawError = response.errorBody()?.string() ?: ""
                Result.failure(Exception(parseErrorMessage(rawError, response.code())))
            }
        } catch (e: Exception) {
            val msg = if (e is UnknownHostException) {
                "Unable to resolve host ${e.message}. Check URL in Settings."
            } else {
                e.message ?: "Login error"
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun getShockers(): Result<List<ShockerItem>> {
        return try {
            val api = apiClient.getApiService()
            val allShockers = mutableListOf<ShockerItem>()
            var authError: String? = null

            val ownedResponse = api.getOwnedShockers1()
            if (ownedResponse.isSuccessful) {
                val bodyString = ownedResponse.body()?.string() ?: ""
                allShockers.addAll(parseShockerList(bodyString))
            } else if ((ownedResponse.code() == 401) || (ownedResponse.code() == 403)) {
                authError = parseErrorMessage(ownedResponse.errorBody()?.string() ?: "", ownedResponse.code())
            }

            if (allShockers.isEmpty()) {
                allShockers.addAll(fetchOwnedShockersViaDevices(api))
            }

            val sharedResponse = api.getSharedShockers1()
            if (sharedResponse.isSuccessful) {
                val bodyString = sharedResponse.body()?.string() ?: ""
                val sharedParsed = parseShockerList(bodyString).map { it.copy(isShared = true) }
                allShockers.addAll(sharedParsed)
            } else if ((authError == null) && ((sharedResponse.code() == 401) || (sharedResponse.code() == 403))) {
                authError = parseErrorMessage(sharedResponse.errorBody()?.string() ?: "", sharedResponse.code())
            }

            if (allShockers.isEmpty() && (authError != null)) {
                return Result.failure(Exception(authError))
            }
            val customIds = tokenManager.getCustomShockerIds()
            for (customId in customIds) {
                if (allShockers.none { it.id == customId }) {
                    val resolved = resolveShockerHub(customId)
                    if (resolved != null) {
                        allShockers.add(resolved)
                    } else {
                        allShockers.add(
                            ShockerItem(
                                id = customId,
                                name = "Shocker ${customId.take(8)}",
                                isShared = true,
                                device = ShockerDevice(id = customId, name = "Hub ${customId.take(8)}"),
                            ),
                        )
                    }
                }
            }

            val withHubIds = allShockers.map { shocker ->
                if (shocker.device?.id.isNullOrBlank()) {
                    val resolved = resolveShockerHub(shocker.id)
                    if (resolved != null) shocker.copy(device = resolved.device, name = resolved.name.ifBlank { shocker.name }) else shocker
                } else {
                    shocker
                }
            }

            Result.success(withHubIds.distinctBy { it.id })
        } catch (e: Exception) {
            val msg = if (e is UnknownHostException) {
                "Unable to resolve host ${e.message}. Check URL in Settings."
            } else {
                e.message ?: "Error loading shockers"
            }
            Result.failure(Exception(msg))
        }
    }

    @Suppress("unused")
    suspend fun triggerShocker(
        shockerId: String,
        shockerName: String,
        type: String, // "Shock", "Vibrate", "Sound", "Stop"
        intensity: Int, // 1 - 100
        durationMs: Int,
    ): Result<String> {
        val normalizedType = when (type.lowercase()) {
            "sound" -> "Sound"
            "vibrate" -> "Vibrate"
            "shock" -> "Shock"
            "stop" -> "Stop"
            else -> "Shock"
        }

        val request = ControlRequest(
            shocks = listOf(
                ControlShockerPayload(
                    id = shockerId,
                    type = normalizedType,
                    intensity = if (normalizedType == "Stop") 0 else intensity.coerceIn(1, 100),
                    duration = durationMs.coerceAtLeast(100),
                    exclusive = true,
                ),
            ),
            customName = "ShockYourPet",
        )

        return try {
            val api = apiClient.getApiService()
            val response = api.controlShockers2(request)

            val success = response.isSuccessful
            val rawError = if (!success) response.errorBody()?.string() ?: "" else ""
            val msg = if (success) "Command sent successfully" else parseErrorMessage(rawError, response.code())

            addLogEntry(
                shockerName = shockerName,
                actionType = type,
                intensity = intensity,
                durationMs = durationMs,
                success = success,
                statusText = msg,
            )

            if (success) {
                Result.success("Sent $type ($intensity% for ${durationMs}ms) to $shockerName")
            } else {
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = if (e is UnknownHostException) {
                "Unable to resolve host ${e.message}. Check URL in Settings."
            } else {
                e.message ?: "Connection error"
            }

            addLogEntry(
                shockerName = shockerName,
                actionType = type,
                intensity = intensity,
                durationMs = durationMs,
                success = false,
                statusText = msg,
            )
            Result.failure(Exception(msg))
        }
    }

    fun addCustomShockerId(id: String) {
        tokenManager.addCustomShockerId(id)
    }

    fun removeCustomShockerId(id: String) {
        tokenManager.removeCustomShockerId(id)
    }

    @Suppress("unused")
    fun addAccountConnection(name: String, token: String): AccountConnection {
        val conn = tokenManager.addAccountConnection(name, token)
        apiClient.invalidateCache()
        return conn
    }

    @Suppress("unused")
    fun removeAccountConnection(id: String) {
        tokenManager.removeAccountConnection(id)
        apiClient.invalidateCache()
    }

    suspend fun getLcgGateway(hubId: String): Result<LcgGatewayInfo> {
        return try {
            val api = apiClient.getApiService()
            var response = api.getDeviceLcg2(hubId)
            if (!response.isSuccessful && (response.code() == 404)) {
                response = api.getDeviceLcg1(hubId)
            }
            if (response.isSuccessful) {
                val bodyString = response.body()?.string() ?: ""
                val gateway = parseLcgGateway(bodyString)
                if (gateway?.host.isNullOrBlank()) {
                    Result.failure(Exception("LCG lookup returned empty host"))
                } else {
                    Result.success(gateway)
                }
            } else {
                val rawError = response.errorBody()?.string() ?: ""
                Result.failure(Exception(parseErrorMessage(rawError, response.code())))
            }
        } catch (e: Exception) {
            val msg = if (e is UnknownHostException) {
                "Unable to resolve host ${e.message}. Check URL in Settings."
            } else {
                e.message ?: "LCG lookup error"
            }
            Result.failure(Exception(msg))
        }
    }

    fun createLiveControlClient(
        scope: CoroutineScope,
        onMissingPermission: (actionType: String, statusText: String) -> Unit = { _, _ -> },
    ): LiveControlClient {
        return LiveControlClient(
            scope = scope,
            tokenProvider = { tokenManager.apiToken },
            resolveLcg = { hubId -> getLcgGateway(hubId) },
        ) { shockerName, actionType, intensity, success, statusText ->
            if ((actionType == "ShockerMissingPermission") || (actionType == "ShockerMissingLivePermission")) {
                onMissingPermission(actionType, statusText)
            }
            addLogEntry(
                shockerName = shockerName,
                actionType = actionType,
                intensity = intensity,
                durationMs = 0,
                success = success,
                statusText = statusText,
            )
        }
    }

    private suspend fun resolveShockerHub(shockerId: String): ShockerItem? {
        return try {
            val api = apiClient.getApiService()
            val response = api.getShocker1(shockerId)
            if (response.isSuccessful) {
                val bodyString = response.body()?.string() ?: ""
                val parsed = ShockerListParser.parse(bodyString).firstOrNull()
                if (parsed != null) {
                    val validHubId = parsed.device?.id?.ifBlank { shockerId } ?: shockerId
                    val validHubName = parsed.device?.name?.ifBlank { "Hub ${validHubId.take(8)}" } ?: "Hub ${validHubId.take(8)}"
                    return parsed.copy(device = ShockerDevice(id = validHubId, name = validHubName))
                }
            }

            val deviceResponse = api.getDeviceShockers1(shockerId)
            if (deviceResponse.isSuccessful) {
                val bodyString = deviceResponse.body()?.string() ?: ""
                val parsedList = ShockerListParser.parseForHub(bodyString, fallbackHubId = shockerId, fallbackHubName = "Hub ${shockerId.take(8)}")
                if (parsedList.isNotEmpty()) {
                    return parsedList.first()
                }
            }

            ShockerItem(
                id = shockerId,
                name = "Shocker ${shockerId.take(8)}",
                isShared = true,
                device = ShockerDevice(id = shockerId, name = "Hub ${shockerId.take(8)}"),
            )
        } catch (_: Exception) {
            ShockerItem(
                id = shockerId,
                name = "Shocker ${shockerId.take(8)}",
                isShared = true,
                device = ShockerDevice(id = shockerId, name = "Hub ${shockerId.take(8)}"),
            )
        }
    }

    private fun parseLcgGateway(json: String): LcgGatewayInfo? {
        if (json.isBlank()) return null
        return try {
            val root = JsonParser.parseString(json)
            val obj = when {
                root.isJsonObject && root.asJsonObject.has("data") && root.asJsonObject["data"].isJsonObject ->
                    root.asJsonObject.getAsJsonObject("data")
                root.isJsonObject -> root.asJsonObject
                else -> return null
            }
            val host = when {
                obj.has("host") -> obj["host"].asString
                obj.has("gateway") -> obj["gateway"].asString
                else -> return null
            }
            LcgGatewayInfo(
                host = host,
                port = if (obj.has("port")) obj["port"].asInt else 443,
                pathPrefix = if (obj.has("pathPrefix")) obj["pathPrefix"].asString else "",
                country = if (obj.has("country")) obj["country"].asString else "",
            )
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchOwnedShockersViaDevices(api: OpenShockApiService): List<ShockerItem> {
        return try {
            val devicesResponse = api.getDevices1()
            if (!devicesResponse.isSuccessful) return emptyList()
            val devicesBody = devicesResponse.body()?.string() ?: return emptyList()
            val devices = parseDeviceList(devicesBody)
            val shockers = mutableListOf<ShockerItem>()
            for (device in devices) {
                val shockerResponse = api.getDeviceShockers1(device.first)
                if (!shockerResponse.isSuccessful) continue
                val shockerBody = shockerResponse.body()?.string() ?: continue
                shockers.addAll(ShockerListParser.parseForHub(shockerBody, device.first, device.second))
            }
            shockers
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseDeviceList(json: String): List<Pair<String, String>> {
        if (json.isBlank()) return emptyList()
        return try {
            val root = JsonParser.parseString(json)
            val array = when {
                root.isJsonArray -> root.asJsonArray
                root.isJsonObject && root.asJsonObject.has("data") && root.asJsonObject["data"].isJsonArray ->
                    root.asJsonObject.getAsJsonArray("data")
                else -> return emptyList()
            }
            array.mapNotNull { element ->
                if (!element.isJsonObject) return@mapNotNull null
                val obj = element.asJsonObject
                val id = obj["id"]?.asString ?: return@mapNotNull null
                val name = obj["name"]?.asString ?: "Hub ${id.take(8)}"
                id to name
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseTokenSelfData(json: String): UserData {
        if (json.isBlank()) return UserData(username = "Connected")
        return try {
            val root = JsonParser.parseString(json)
            val obj = when {
                root.isJsonObject && root.asJsonObject.has("data") && root.asJsonObject["data"].isJsonObject ->
                    root.asJsonObject.getAsJsonObject("data")
                root.isJsonObject -> root.asJsonObject
                else -> return UserData(username = "Connected")
            }
            val tokenId = obj["id"]?.takeUnless { it.isJsonNull }?.asString.orEmpty()
            val tokenName = obj["name"]?.takeUnless { it.isJsonNull }?.asString.orEmpty()
            val permissions = if (obj.has("permissions") && obj.get("permissions").isJsonArray) {
                obj.getAsJsonArray("permissions").map { it.asString }
            } else {
                emptyList()
            }
            val hasShockersUse = permissions.any {
                it.equals("shockers.use", ignoreCase = true) || it.equals("Shockers_Use", ignoreCase = true)
            }
            UserData(
                username = "Connected account",
                tokenId = tokenId,
                tokenName = tokenName,
                permissions = permissions,
                hasShockersUse = hasShockersUse,
            )
        } catch (_: Exception) {
            UserData(username = "Connected")
        }
    }

    private fun parseUserSelfData(json: String, tokenData: UserData): UserData {
        if (json.isBlank()) return tokenData
        return try {
            val root = JsonParser.parseString(json)
            val obj = when {
                root.isJsonObject && root.asJsonObject.has("data") && root.asJsonObject["data"].isJsonObject ->
                    root.asJsonObject.getAsJsonObject("data")
                root.isJsonObject -> root.asJsonObject
                else -> return tokenData
            }
            val roles = if (obj.has("roles") && obj.get("roles").isJsonArray) {
                obj.getAsJsonArray("roles").mapNotNull { role ->
                    runCatching { role.asString }.getOrNull()
                }
            } else {
                emptyList()
            }
            tokenData.copy(
                id = obj.get("id")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
                username = obj.get("name")?.takeUnless { it.isJsonNull }?.asString
                    ?: obj.get("username")?.takeUnless { it.isJsonNull }?.asString
                    ?: tokenData.username,
                email = obj.get("email")?.takeUnless { it.isJsonNull }?.asString,
                imageUrl = obj.get("image")?.takeUnless { it.isJsonNull }?.asString,
                rank = obj.get("rank")?.takeUnless { it.isJsonNull }?.asString ?: "User",
                roles = roles,
                hasPassword = obj.get("hasPassword")?.takeUnless { it.isJsonNull }?.asBoolean ?: false,
            )
        } catch (_: Exception) {
            tokenData
        }
    }

    private fun parseShockerList(json: String): List<ShockerItem> = ShockerListParser.parse(json)

    private fun parseToken(json: String): String? {
        if (json.isBlank()) return null
        return try {
            val root = JsonParser.parseString(json)
            if (root.isJsonObject) {
                val obj = root.asJsonObject
                when {
                    obj.has("token") -> obj.get("token").asString
                    obj.has("data") && obj["data"].isJsonObject && obj.getAsJsonObject("data").has("token") ->
                        obj.getAsJsonObject("data").get("token").asString
                    else -> null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun addLogEntry(
        shockerName: String,
        actionType: String,
        intensity: Int,
        durationMs: Int,
        success: Boolean,
        statusText: String,
    ) {
        val entry = CommandLogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            shockerName = shockerName,
            actionType = actionType,
            intensity = intensity,
            durationMs = durationMs,
            success = success,
            statusText = statusText,
        )
        val current = _commandLogs.value.toMutableList()
        current.add(0, entry)
        _commandLogs.value = current.take(100)
    }

    private fun parseErrorMessage(errorBody: String, httpCode: Int): String {
        if (errorBody.isNotBlank()) {
            try {
                val root = JsonParser.parseString(errorBody)
                if (root.isJsonObject) {
                    val obj = root.asJsonObject
                    if (obj.has("message") && !obj.get("message").isJsonNull) {
                        return obj.get("message").asString
                    }
                    if (obj.has("error") && !obj["error"].isJsonNull) {
                        return obj["error"].asString
                    }
                }
            } catch (_: Exception) {
            }
        }
        return when (httpCode) {
            401 -> "Invalid or expired API token"
            403 -> "Permission denied for this operation"
            404 -> "Requested resource not found"
            429 -> "Rate limit exceeded. Please wait a moment."
            else -> "HTTP $httpCode request failed"
        }
    }
}
