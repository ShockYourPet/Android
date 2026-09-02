package app.shockyourpet.data.livecontrol

import app.shockyourpet.data.api.models.LcgGatewayInfo

object LiveControlUrlBuilder {
    fun buildWebSocketUrl(gateway: LcgGatewayInfo, hubId: String, tps: Int = 10): String {
        val portPart = if (gateway.port == 443) "" else ":${gateway.port}"
        val prefix = gateway.pathPrefix.trimEnd('/')
        val path = if (prefix.isEmpty()) {
            "/1/ws/live/$hubId"
        } else {
            "$prefix/1/ws/live/$hubId"
        }
        return "wss://${gateway.host}$portPart$path?tps=$tps"
    }
}
