package app.shockyourpet.data.livecontrol

import com.google.gson.JsonObject
import com.google.gson.JsonParser

object LiveControlMessageParser {
    fun parseRoot(text: String): JsonObject? {
        return try {
            val root = JsonParser.parseString(text)
            if (root.isJsonObject) root.asJsonObject else null
        } catch (_: Exception) {
            null
        }
    }

    fun parseResponseType(root: JsonObject): String? {
        return root["ResponseType"]?.takeIf { it.isJsonPrimitive }?.asString
    }

    fun parseData(root: JsonObject): JsonObject? {
        val dataElement = root["Data"] ?: return null
        return if (dataElement.isJsonObject) dataElement.asJsonObject else null
    }

    fun parseClientTps(data: JsonObject?): Int {
        val value = data?.get("Client")?.takeIf { it.isJsonPrimitive }?.asInt ?: 10
        return if (value < 1) 1 else value
    }

    fun parsePingTimestamp(data: JsonObject?, fallback: Long = System.currentTimeMillis()): Long {
        val timestamp = data?.get("Timestamp")?.takeIf { it.isJsonPrimitive }?.asLong
        return timestamp ?: fallback
    }
}
