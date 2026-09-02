package app.shockyourpet.data.api.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Keep
data class AccountConnection(
    val id: String,
    val name: String,
    val token: String,
    val serverUrl: String = "https://api.openshock.app",
)

@Keep
data class SharedApiKey(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val token: String,
    val serverUrl: String = "https://api.openshock.app",
)

@Keep
data class UserData(
    @SerializedName("id") val id: String = "",
    @SerializedName("username") val username: String = "",
    @SerializedName("email") val email: String? = null,
    val imageUrl: String? = null,
    val rank: String = "User",
    val roles: List<String> = emptyList(),
    val hasPassword: Boolean = false,
    val tokenId: String = "",
    val tokenName: String = "",
    val permissions: List<String> = emptyList(),
    val hasShockersUse: Boolean = true,
)

@Keep
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
)

@Keep
@Suppress("unused")
data class LoginResponse(
    @SerializedName("token") val token: String? = null,
    @SerializedName("message") val message: String? = null,
)
