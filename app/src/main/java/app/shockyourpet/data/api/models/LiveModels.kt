package app.shockyourpet.data.api.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class LcgGatewayInfo(
    @SerializedName("host") val host: String,
    @SerializedName("port") val port: Int = 443,
    @SerializedName("pathPrefix") val pathPrefix: String = "",
    @SerializedName("country") val country: String = "",
)

@Keep
data class LiveControlFrameData(
    @SerializedName("Shocker") val shocker: String,
    @SerializedName("Intensity") val intensity: Int,
    @SerializedName("Type") val type: String,
)

@Keep
data class LiveControlRequest(
    @SerializedName("RequestType") val requestType: String,
    @SerializedName("Data") val data: Any,
)

@Keep
data class LiveControlPongData(
    @SerializedName("Timestamp") val timestamp: Long = System.currentTimeMillis(),
)
