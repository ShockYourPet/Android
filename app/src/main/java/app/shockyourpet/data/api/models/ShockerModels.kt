package app.shockyourpet.data.api.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class ShockerOwner(
    @SerializedName("id") val id: String = "",
    @SerializedName("username") val username: String = "",
)

@Keep
data class ShockerDevice(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
)

@Keep
data class ShockerPermissions(
    val allowShock: Boolean = true,
    val allowVibrate: Boolean = true,
    val allowSound: Boolean = true,
    val maxIntensity: Int = 100,
    val maxDuration: Int = 10000,
)

@Keep
data class ShockerLimits(
    @SerializedName("maxIntensity") val maxIntensity: Int = 100,
    @SerializedName("maxDuration") val maxDuration: Int = 10000,
    val allowShock: Boolean = true,
    val allowVibrate: Boolean = true,
    val allowSound: Boolean = true,
)

@Keep
data class ShockerItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("paused") val paused: Boolean = false,
    @SerializedName("isPaused") val isPaused: Boolean = false,
    @SerializedName("type") val type: String = "Shock",
    @SerializedName("isShared") val isShared: Boolean = false,
    @SerializedName("owner") val owner: ShockerOwner? = null,
    @SerializedName("device") val device: ShockerDevice? = null,
    @SerializedName("limits") val limits: ShockerLimits? = null,
    @SerializedName("permissions") val permissions: ShockerPermissions? = ShockerPermissions(),
) {
    fun isModeAllowed(mode: String): Boolean {
        if (paused || isPaused) return false
        val modeLower = mode.lowercase()
        val perm = permissions ?: ShockerPermissions()
        val lim = limits ?: ShockerLimits()

        return when (modeLower) {
            "shock" -> (perm.allowShock && lim.allowShock) && (perm.maxIntensity > 0)
            "vibrate" -> perm.allowVibrate && lim.allowVibrate
            "sound" -> perm.allowSound && lim.allowSound
            else -> true
        }
    }
}
