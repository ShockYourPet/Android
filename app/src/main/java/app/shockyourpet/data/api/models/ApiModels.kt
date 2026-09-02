package app.shockyourpet.data.api.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
@Suppress("unused")
data class ApiResponse<T>(
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
    @SerializedName("status") val status: Int? = null,
)

@Keep
data class ControlShockerPayload(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String, // "Shock", "Vibrate", "Sound", "Stop"
    @SerializedName("intensity") val intensity: Int, // 1 - 100
    @SerializedName("duration") val duration: Int, // ms
    @SerializedName("exclusive") val exclusive: Boolean = true,
)

@Keep
data class ControlRequest(
    @SerializedName("shocks") val shocks: List<ControlShockerPayload>,
    @SerializedName("customName") val customName: String = "ShockYourPet",
)

@Keep
@Suppress("unused")
data class ControlResponse(
    @SerializedName("message") val message: String? = null,
)

@Keep
data class ControlPreset(
    val id: String,
    val name: String,
    val type: String, // "Shock", "Vibrate", "Sound"
    val intensity: Int,
    val durationMs: Int,
)
