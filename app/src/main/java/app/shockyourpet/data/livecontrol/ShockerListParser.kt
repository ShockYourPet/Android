package app.shockyourpet.data.livecontrol

import app.shockyourpet.data.api.models.ShockerDevice
import app.shockyourpet.data.api.models.ShockerItem
import app.shockyourpet.data.api.models.ShockerLimits
import app.shockyourpet.data.api.models.ShockerOwner
import app.shockyourpet.data.api.models.ShockerPermissions
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object ShockerListParser {
    fun parse(json: String): List<ShockerItem> {
        if (json.isBlank()) return emptyList()
        return try {
            val root = JsonParser.parseString(json)
            val dataArray = when {
                root.isJsonArray -> root.asJsonArray
                root.isJsonObject && root.asJsonObject.has("data") && root.asJsonObject["data"].isJsonArray ->
                    root.asJsonObject.getAsJsonArray("data")
                root.isJsonObject && root.asJsonObject.has("shockers") && root.asJsonObject["shockers"].isJsonArray ->
                    root.asJsonObject.getAsJsonArray("shockers")
                root.isJsonObject -> {
                    val single = parseShockerItemFromJson(root.asJsonObject)
                    return single?.let { listOf(it) } ?: emptyList()
                }
                else -> return emptyList()
            }

            val flattened = mutableListOf<ShockerItem>()
            for (element in dataArray) {
                if (!element.isJsonObject) continue
                val obj = element.asJsonObject
                when {
                    obj.has("devices") && obj["devices"].isJsonArray ->
                        flattened.addAll(parseSharedOwner(obj))
                    obj.has("shockers") && obj["shockers"].isJsonArray ->
                        flattened.addAll(parseOwnedHub(obj))
                    else -> {
                        parseShockerItemFromJson(obj)?.let { flattened.add(it) }
                    }
                }
            }
            flattened
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseForHub(json: String, fallbackHubId: String, fallbackHubName: String): List<ShockerItem> {
        if (json.isBlank()) return emptyList()
        return try {
            val root = JsonParser.parseString(json)
            val array = when {
                root.isJsonArray -> root.asJsonArray
                root.isJsonObject && root.asJsonObject.has("data") && root.asJsonObject["data"].isJsonArray ->
                    root.asJsonObject.getAsJsonArray("data")
                root.isJsonObject && root.asJsonObject.has("shockers") && root.asJsonObject["shockers"].isJsonArray ->
                    root.asJsonObject.getAsJsonArray("shockers")
                root.isJsonObject -> {
                    val item = parseShockerItemFromJson(root.asJsonObject, fallbackHubId, fallbackHubName)
                    return item?.let { listOf(it) } ?: emptyList()
                }
                else -> return emptyList()
            }
            array.mapNotNull { element ->
                if (!element.isJsonObject) return@mapNotNull null
                parseShockerItemFromJson(element.asJsonObject, fallbackHubId, fallbackHubName)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseOwnedHub(hubObj: JsonObject): List<ShockerItem> {
        val hubId = hubObj["id"]?.takeIf { it.isJsonPrimitive }?.asString ?: ""
        val hubName = hubObj["name"]?.takeIf { it.isJsonPrimitive }?.asString ?: "Hub ${hubId.take(8)}"
        val shockers = hubObj.getAsJsonArray("shockers") ?: return emptyList()
        return shockers.mapNotNull { shockerEl ->
            if (!shockerEl.isJsonObject) return@mapNotNull null
            parseShockerItemFromJson(shockerEl.asJsonObject, hubId, hubName)
        }
    }

    private fun parseSharedOwner(ownerObj: JsonObject): List<ShockerItem> {
        val devices = ownerObj.getAsJsonArray("devices") ?: return emptyList()
        val ownerName = ownerObj["name"]?.takeIf { it.isJsonPrimitive }?.asString
            ?: ownerObj["username"]?.takeIf { it.isJsonPrimitive }?.asString ?: "Shared"
        val ownerId = ownerObj["id"]?.takeIf { it.isJsonPrimitive }?.asString ?: ""

        val result = mutableListOf<ShockerItem>()
        for (deviceEl in devices) {
            if (!deviceEl.isJsonObject) continue
            val deviceObj = deviceEl.asJsonObject
            val hubId = deviceObj["id"]?.takeIf { it.isJsonPrimitive }?.asString ?: continue
            val hubName = deviceObj["name"]?.takeIf { it.isJsonPrimitive }?.asString ?: "Hub ${hubId.take(8)}"
            val shockers = deviceObj.getAsJsonArray("shockers") ?: continue
            for (shockerEl in shockers) {
                if (!shockerEl.isJsonObject) continue
                val item = parseShockerItemFromJson(shockerEl.asJsonObject, hubId, hubName, isSharedOverride = true)
                item?.let {
                    result.add(
                        it.copy(
                            isShared = true,
                            owner = ShockerOwner(id = ownerId, username = ownerName),
                        ),
                    )
                }
            }
        }
        return result
    }

    fun parseShockerItemFromJson(
        obj: JsonObject,
        fallbackHubId: String = "",
        fallbackHubName: String = "",
        isSharedOverride: Boolean = false,
    ): ShockerItem? {
        val id = obj["id"]?.takeIf { it.isJsonPrimitive }?.asString ?: return null
        val name = obj["name"]?.takeIf { it.isJsonPrimitive }?.asString ?: "Shocker ${id.take(8)}"
        val paused = when {
            obj.has("isPaused") && obj["isPaused"].isJsonPrimitive -> obj["isPaused"].asBoolean
            obj.has("paused") && obj["paused"].isJsonPrimitive -> obj["paused"].asBoolean
            else -> false
        }
        val isShared = isSharedOverride || (obj.has("isShared") && obj["isShared"].isJsonPrimitive && obj["isShared"].asBoolean)

        // Flexible Hub / Device ID extraction
        var hubId = fallbackHubId
        var hubName = fallbackHubName

        if (obj.has("device") && !obj["device"].isJsonNull) {
            val devEl = obj["device"]
            if (devEl.isJsonObject) {
                val devObj = devEl.asJsonObject
                val parsedId = devObj["id"]?.takeIf { it.isJsonPrimitive }?.asString
                val parsedName = devObj["name"]?.takeIf { it.isJsonPrimitive }?.asString
                if (!parsedId.isNullOrBlank()) hubId = parsedId
                if (!parsedName.isNullOrBlank()) hubName = parsedName
            } else if (devEl.isJsonPrimitive && devEl.asString.isNotBlank()) {
                hubId = devEl.asString
            }
        }

        if (hubId.isBlank()) {
            if (obj.has("deviceId") && obj["deviceId"].isJsonPrimitive) {
                hubId = obj["deviceId"].asString
            } else if (obj.has("hubId") && obj["hubId"].isJsonPrimitive) {
                hubId = obj["hubId"].asString
            } else if (obj.has("hub") && !obj["hub"].isJsonNull) {
                val hubEl = obj["hub"]
                if (hubEl.isJsonObject) {
                    val parsedId = hubEl.asJsonObject["id"]?.takeIf { it.isJsonPrimitive }?.asString
                    val parsedName = hubEl.asJsonObject["name"]?.takeIf { it.isJsonPrimitive }?.asString
                    if (!parsedId.isNullOrBlank()) hubId = parsedId
                    if (!parsedName.isNullOrBlank()) hubName = parsedName
                } else if (hubEl.isJsonPrimitive && hubEl.asString.isNotBlank()) {
                    hubId = hubEl.asString
                }
            }
        }

        // Guaranteed Fallback: If no hub ID found, default hubId to the shocker's own ID
        if (hubId.isBlank()) {
            hubId = id
        }
        if (hubName.isBlank()) {
            hubName = "Hub ${hubId.take(8)}"
        }

        val limits = parseLimits(obj["limits"])
        val permissions = parsePermissions(obj["permissions"] ?: obj["limits"])
        val owner = parseOwner(obj["owner"])

        return ShockerItem(
            id = id,
            name = name,
            paused = paused,
            isPaused = paused,
            isShared = isShared,
            owner = owner,
            device = ShockerDevice(id = hubId, name = hubName),
            limits = limits,
            permissions = permissions,
        )
    }

    private fun parseOwner(element: JsonElement?): ShockerOwner? {
        if ((element == null) || !element.isJsonObject) return null
        val obj = element.asJsonObject
        val id = obj["id"]?.takeIf { it.isJsonPrimitive }?.asString ?: ""
        val username = when {
            obj.has("username") && obj["username"].isJsonPrimitive -> obj["username"].asString
            obj.has("name") && obj["name"].isJsonPrimitive -> obj["name"].asString
            else -> "Shared"
        }
        return ShockerOwner(id = id, username = username)
    }

    private fun parseLimits(element: JsonElement?): ShockerLimits? {
        if ((element == null) || !element.isJsonObject) return null
        val obj = element.asJsonObject
        val intensity = when {
            obj.has("maxIntensity") && obj["maxIntensity"].isJsonPrimitive -> obj["maxIntensity"].asInt
            obj.has("intensity") && obj["intensity"].isJsonPrimitive -> obj["intensity"].asInt
            else -> 100
        }
        val duration = when {
            obj.has("maxDuration") && obj["maxDuration"].isJsonPrimitive -> obj["maxDuration"].asInt
            obj.has("duration") && obj["duration"].isJsonPrimitive -> obj["duration"].asInt
            else -> 10000
        }
        val perms = parsePermissions(element)
        return ShockerLimits(
            maxIntensity = intensity,
            maxDuration = duration,
            allowShock = perms.allowShock,
            allowVibrate = perms.allowVibrate,
            allowSound = perms.allowSound,
        )
    }

    fun parsePermissions(element: JsonElement?): ShockerPermissions {
        if ((element == null) || element.isJsonNull) return ShockerPermissions()

        var allowShock = true
        var allowVibrate = true
        var allowSound = true
        var maxIntensity = 100
        var maxDuration = 10000

        if (element.isJsonObject) {
            val obj = element.asJsonObject

            if (obj.has("maxIntensity") && obj["maxIntensity"].isJsonPrimitive) {
                maxIntensity = obj["maxIntensity"].asInt
            } else if (obj.has("intensity") && obj["intensity"].isJsonPrimitive) {
                maxIntensity = obj["intensity"].asInt
            }

            if (obj.has("maxDuration") && obj["maxDuration"].isJsonPrimitive) {
                maxDuration = obj["maxDuration"].asInt
            } else if (obj.has("duration") && obj["duration"].isJsonPrimitive) {
                maxDuration = obj["duration"].asInt
            }

            // Shock
            if (obj.has("allowShock") && obj["allowShock"].isJsonPrimitive) {
                allowShock = obj["allowShock"].asBoolean
            } else if (obj.has("canShock") && obj["canShock"].isJsonPrimitive) {
                allowShock = obj["canShock"].asBoolean
            } else if (obj.has("shock") && obj["shock"].isJsonPrimitive) {
                allowShock = obj["shock"].asBoolean
            } else if (obj.has("maxShockIntensity") && obj["maxShockIntensity"].isJsonPrimitive) {
                allowShock = obj["maxShockIntensity"].asInt > 0
            }

            // Vibrate
            if (obj.has("allowVibrate") && obj["allowVibrate"].isJsonPrimitive) {
                allowVibrate = obj["allowVibrate"].asBoolean
            } else if (obj.has("canVibrate") && obj["canVibrate"].isJsonPrimitive) {
                allowVibrate = obj["canVibrate"].asBoolean
            } else if (obj.has("vibrate") && obj["vibrate"].isJsonPrimitive) {
                allowVibrate = obj["vibrate"].asBoolean
            } else if (obj.has("maxVibrateIntensity") && obj["maxVibrateIntensity"].isJsonPrimitive) {
                allowVibrate = obj["maxVibrateIntensity"].asInt > 0
            }

            // Sound
            if (obj.has("allowSound") && obj["allowSound"].isJsonPrimitive) {
                allowSound = obj["allowSound"].asBoolean
            } else if (obj.has("canSound") && obj["canSound"].isJsonPrimitive) {
                allowSound = obj["canSound"].asBoolean
            } else if (obj.has("sound") && obj["sound"].isJsonPrimitive) {
                allowSound = obj["sound"].asBoolean
            } else if (obj.has("maxSoundIntensity") && obj["maxSoundIntensity"].isJsonPrimitive) {
                allowSound = obj["maxSoundIntensity"].asInt > 0
            }
        } else if (element.isJsonArray) {
            val array = element.asJsonArray
            val modeStrings = array.mapNotNull { if (it.isJsonPrimitive) it.asString.lowercase() else null }
            if (modeStrings.isNotEmpty()) {
                val hasExplicitModes = modeStrings.any { (it == "shock") || (it == "vibrate") || (it == "sound") }
                if (hasExplicitModes) {
                    allowShock = modeStrings.contains("shock")
                    allowVibrate = modeStrings.contains("vibrate")
                    allowSound = modeStrings.contains("sound")
                }
            }
        }

        return ShockerPermissions(
            allowShock = allowShock,
            allowVibrate = allowVibrate,
            allowSound = allowSound,
            maxIntensity = maxIntensity,
            maxDuration = maxDuration,
        )
    }
}
