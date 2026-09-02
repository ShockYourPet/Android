package app.shockyourpet.data.api.models

import androidx.annotation.Keep

@Keep
data class CommandLogEntry(
    val id: String,
    val timestamp: Long,
    val shockerName: String,
    val actionType: String,
    val intensity: Int,
    val durationMs: Int,
    val success: Boolean,
    val statusText: String,
)
