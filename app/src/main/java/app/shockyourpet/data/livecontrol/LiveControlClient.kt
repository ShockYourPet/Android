package app.shockyourpet.data.livecontrol

import app.shockyourpet.data.api.models.LcgGatewayInfo
import app.shockyourpet.data.api.models.LiveControlFrameData
import app.shockyourpet.data.api.models.LiveControlPongData
import app.shockyourpet.data.api.models.LiveControlRequest
import app.shockyourpet.data.api.withOpenShockAuth
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class LiveControlClient(
    private val scope: CoroutineScope,
    private val tokenProvider: () -> String,
    private val resolveLcg: suspend (hubId: String) -> Result<LcgGatewayInfo>,
    private val onEvent: (shockerName: String, actionType: String, intensity: Int, success: Boolean, statusText: String) -> Unit,
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var frameJob: Job? = null
    private var connectJob: Job? = null

    private var hubId: String? = null
    private var shockerId: String? = null
    private var shockerName: String = ""
    private var controlType: String = "Shock"
    private var intensity: Int = 0
    private var tps: Int = 10
    private val isHolding = AtomicBoolean(false)
    private val isReady = AtomicBoolean(false)
    private val isConnecting = AtomicBoolean(false)

    fun connect(hubId: String, shockerId: String, shockerName: String) {
        if ((this.hubId == hubId) && (isReady.get() || isConnecting.get())) {
            this.shockerId = shockerId
            this.shockerName = shockerName
            return
        }
        disconnect()
        this.hubId = hubId
        this.shockerId = shockerId
        this.shockerName = shockerName
        isConnecting.set(true)
        connectJob = scope.launch {
            val gatewayResult = resolveLcg(hubId)
            gatewayResult.fold(
                onSuccess = { gateway ->
                    val url = LiveControlUrlBuilder.buildWebSocketUrl(gateway, hubId)
                    openWebSocket(url)
                },
            ) { err ->
                isConnecting.set(false)
                onEvent(shockerName, "LiveConnect", 0, false, err.message ?: "LCG lookup failed")
            }
        }
    }

    fun disconnect() {
        connectJob?.cancel()
        connectJob = null
        stopFrameTicker()
        isHolding.set(false)
        isReady.set(false)
        isConnecting.set(false)
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        hubId = null
        shockerId = null
    }

    @Suppress("unused")
    fun startHolding(type: String, targetIntensity: Int) {
        controlType = normalizeType(type)
        intensity = targetIntensity.coerceIn(0, 100)
        isHolding.set(true)
        if (isReady.get()) startFrameTicker()
    }

    fun updateHolding(type: String, targetIntensity: Int) {
        controlType = normalizeType(type)
        intensity = targetIntensity.coerceIn(0, 100)
        isHolding.set(true)
        if (isReady.get() && (frameJob == null)) startFrameTicker()
    }

    fun stopHolding() {
        isHolding.set(false)
        intensity = 0
        stopFrameTicker()
    }

    private fun openWebSocket(url: String) {
        val token = tokenProvider().trim()
        val request = Request.Builder()
            .url(url)
            .withOpenShockAuth(token)
            .build()

        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    isConnecting.set(false)
                    onEvent(shockerName, "LiveConnect", 0, true, "WebSocket connected")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleServerMessage(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    isReady.set(false)
                    isConnecting.set(false)
                    stopFrameTicker()
                    onEvent(shockerName, "LiveDisconnect", 0, code == 1000, "Closed: $reason ($code)")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    isReady.set(false)
                    isConnecting.set(false)
                    stopFrameTicker()
                    val detail = response?.message?.let { "$it - " } ?: ""
                    onEvent(shockerName, "LiveError", 0, false, "$detail${t.message ?: "WebSocket failure"}")
                }
            },
        )
    }

    private fun handleServerMessage(text: String) {
        try {
            val root = LiveControlMessageParser.parseRoot(text) ?: return
            val responseType = LiveControlMessageParser.parseResponseType(root) ?: return
            val data = LiveControlMessageParser.parseData(root)

            when (responseType) {
                "TPS" -> {
                    tps = LiveControlMessageParser.parseClientTps(data)
                }
                "DeviceConnected" -> {
                    isReady.set(true)
                    onEvent(shockerName, "LiveReady", 0, true, "Hub online on gateway (TPS $tps)")
                    if (isHolding.get()) startFrameTicker()
                }
                "Ping" -> {
                    val timestamp = LiveControlMessageParser.parsePingTimestamp(data)
                    sendMessage(LiveControlRequest("Pong", LiveControlPongData(timestamp)))
                }
                "LatencyAnnounce" -> Unit
                "DeviceNotConnected" -> {
                    isReady.set(false)
                    stopFrameTicker()
                    onEvent(shockerName, "LiveError", 0, false, "Hub disconnected from gateway")
                }
                "ShockerNotFound",
                "ShockerMissingPermission",
                "ShockerMissingLivePermission",
                "ShockerPaused",
                "ShockerExclusive",
                "TokenPaused",
                "InvalidData",
                -> {
                    val extra = data?.get("Until")?.asString?.let { " until $it" } ?: ""
                    onEvent(shockerName, responseType, intensity, false, "$responseType$extra")
                }
            }
        } catch (e: Exception) {
            onEvent(shockerName, "LiveError", 0, false, e.message ?: "Failed to parse server message")
        }
    }

    private fun startFrameTicker() {
        if (frameJob?.isActive == true) return
        val sid = shockerId ?: return
        frameJob = scope.launch {
            val intervalMs = (1000.0 / tps).toLong().coerceAtLeast(100L)
            sendFrame(sid, controlType, intensity)
            while (isActive && isHolding.get()) {
                delay(intervalMs.milliseconds)
                if (!isHolding.get()) break
                sendFrame(sid, controlType, intensity)
            }
        }
    }

    private fun stopFrameTicker() {
        frameJob?.cancel()
        frameJob = null
    }

    private fun sendFrame(shockerId: String, type: String, frameIntensity: Int) {
        val frame = LiveControlFrameData(
            shocker = shockerId,
            intensity = frameIntensity.coerceIn(0, 100),
            type = normalizeType(type),
        )
        sendMessage(LiveControlRequest("Frame", frame))
    }

    private fun sendMessage(request: LiveControlRequest) {
        webSocket?.send(gson.toJson(request))
    }

    private fun normalizeType(type: String): String = when (type.lowercase()) {
        "sound" -> "Sound"
        "vibrate" -> "Vibrate"
        "shock" -> "Shock"
        "stop" -> "Stop"
        else -> "Shock"
    }
}
