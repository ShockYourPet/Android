package app.shockyourpet

import app.shockyourpet.data.api.models.ControlRequest
import app.shockyourpet.data.api.models.ControlShockerPayload
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class OpenShockModelsTest {

    private val gson = Gson()

    @Test
    fun testControlRequestSerialization() {
        val testId = UUID.randomUUID().toString()
        val payload = ControlShockerPayload(
            id = testId,
            type = "Shock",
            intensity = 25,
            duration = 1500,
            exclusive = true,
        )
        val request = ControlRequest(
            shocks = listOf(payload),
            customName = "ShockYourPet",
        )

        val json = gson.toJson(request)
        assertTrue(json.contains(testId))
        assertTrue(json.contains("\"intensity\":25"))
        assertTrue(json.contains("\"duration\":1500"))
        assertTrue(json.contains("\"type\":\"Shock\""))
        assertTrue(json.contains("\"customName\":\"ShockYourPet\""))

        val deserialized = gson.fromJson(json, ControlRequest::class.java)
        assertEquals(1, deserialized.shocks.size)
        assertEquals(testId, deserialized.shocks[0].id)
        assertEquals("Shock", deserialized.shocks[0].type)
        assertEquals(25, deserialized.shocks[0].intensity)
        assertEquals(1500, deserialized.shocks[0].duration)
    }
}
