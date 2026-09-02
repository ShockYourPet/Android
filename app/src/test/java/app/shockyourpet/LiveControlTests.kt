package app.shockyourpet

import app.shockyourpet.data.api.models.LcgGatewayInfo
import app.shockyourpet.data.api.models.LiveControlFrameData
import app.shockyourpet.data.api.models.LiveControlPongData
import app.shockyourpet.data.api.models.LiveControlRequest
import app.shockyourpet.data.livecontrol.LiveControlMessageParser
import app.shockyourpet.data.livecontrol.LiveControlUrlBuilder
import app.shockyourpet.data.livecontrol.ShockerListParser
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveControlTests {
    private val gson = Gson()

    @Test
    fun testOwnedHubJsonFlattensWithDeviceId() {
        val json = """
            {
              "data": [
                {
                  "id": "hub-1111-1111-1111-111111111111",
                  "name": "My Hub",
                  "shockers": [
                    {
                      "id": "shock-2222-2222-2222-222222222222",
                      "name": "jaz",
                      "isPaused": false
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val shockers = ShockerListParser.parse(json)
        assertEquals(1, shockers.size)
        assertEquals("shock-2222-2222-2222-222222222222", shockers[0].id)
        assertEquals("jaz", shockers[0].name)
        assertEquals("hub-1111-1111-1111-111111111111", shockers[0].device?.id)
        assertEquals("My Hub", shockers[0].device?.name)
    }

    @Test
    fun testSharedOwnerJsonFlattensWithDeviceId() {
        val json = """
            {
              "data": [
                {
                  "id": "owner-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                  "name": "friend",
                  "devices": [
                    {
                      "id": "hub-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                      "name": "Shared Hub",
                      "shockers": [
                        {
                          "id": "shock-cccc-cccc-cccc-cccccccccccc",
                          "name": "collar",
                          "isPaused": false
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val shockers = ShockerListParser.parse(json)
        assertEquals(1, shockers.size)
        assertTrue(shockers[0].isShared)
        assertEquals("hub-bbbb-bbbb-bbbb-bbbbbbbbbbbb", shockers[0].device?.id)
    }

    @Test
    fun testLcgUrlBuilderDefaultPortAndPrefix() {
        val gateway = LcgGatewayInfo(host = "de1-gateway.openshock.app", port = 443, pathPrefix = "")
        val url = LiveControlUrlBuilder.buildWebSocketUrl(gateway, "hub-id", tps = 10)
        assertEquals("wss://de1-gateway.openshock.app/1/ws/live/hub-id?tps=10", url)
    }

    @Test
    fun testLcgUrlBuilderCustomPortAndPrefix() {
        val gateway = LcgGatewayInfo(host = "gw.example.com", port = 8443, pathPrefix = "/gateway")
        val url = LiveControlUrlBuilder.buildWebSocketUrl(gateway, "hub-id", tps = 5)
        assertEquals("wss://gw.example.com:8443/gateway/1/ws/live/hub-id?tps=5", url)
    }

    @Test
    fun testFrameAndPongSerialization() {
        val frameJson = gson.toJson(
            LiveControlRequest(
                requestType = "Frame",
                data = LiveControlFrameData(
                    shocker = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    intensity = 40,
                    type = "Shock",
                ),
            ),
        )
        assertTrue(frameJson.contains("\"RequestType\":\"Frame\""))
        assertTrue(frameJson.contains("\"Shocker\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\""))
        assertTrue(frameJson.contains("\"Intensity\":40"))
        assertTrue(frameJson.contains("\"Type\":\"Shock\""))

        val pongJson = gson.toJson(
            LiveControlRequest(
                requestType = "Pong",
                data = LiveControlPongData(timestamp = 1710000000000L),
            ),
        )
        assertTrue(pongJson.contains("\"RequestType\":\"Pong\""))
        assertTrue(pongJson.contains("\"Timestamp\":1710000000000"))
    }

    @Test
    fun testParseTpsMessageData() {
        val root = LiveControlMessageParser.parseRoot("""{"ResponseType":"TPS","Data":{"Client":10}}""")!!
        val data = LiveControlMessageParser.parseData(root)
        assertEquals(10, LiveControlMessageParser.parseClientTps(data))
    }

    @Test
    fun testParseDeviceConnectedWithoutData() {
        val root = LiveControlMessageParser.parseRoot("""{"ResponseType":"DeviceConnected"}""")!!
        assertEquals("DeviceConnected", LiveControlMessageParser.parseResponseType(root))
        assertNull(LiveControlMessageParser.parseData(root))
    }

    @Test
    fun testParseDeviceConnectedWithNullData() {
        val root = LiveControlMessageParser.parseRoot("""{"ResponseType":"DeviceConnected","Data":null}""")!!
        assertEquals("DeviceConnected", LiveControlMessageParser.parseResponseType(root))
        assertNull(LiveControlMessageParser.parseData(root))
    }

    @Test
    fun testParsePingTimestamp() {
        val root = LiveControlMessageParser.parseRoot("""{"ResponseType":"Ping","Data":{"Timestamp":123}}""")!!
        val data = LiveControlMessageParser.parseData(root)
        assertEquals(123L, LiveControlMessageParser.parsePingTimestamp(data, fallback = 0L))
    }
}
