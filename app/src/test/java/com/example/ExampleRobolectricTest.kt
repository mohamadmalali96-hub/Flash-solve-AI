package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppLanguage
import com.example.model.PillMode
import com.example.network.GeminiFlashClient
import com.example.service.FlashSolveController
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FlashSolve AI", appName)
    }

    @Test
    fun `gemini flash payload enforces ultra-fast minimal output config`() {
        val payloadStr = GeminiFlashClient.buildUltraFastPayloadJson("dGVzdA==")
        val json = JSONObject(payloadStr)
        val genConfig = json.getJSONObject("generationConfig")
        assertEquals(12, genConfig.getInt("maxOutputTokens"))
        assertEquals(0.0, genConfig.getDouble("temperature"), 0.001)
        assertEquals(1, genConfig.getInt("topK"))

        val fakeResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "[B]" }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()
        assertEquals("[B]", GeminiFlashClient.parseMinimalBracketedAnswer(fakeResponse))
        assertEquals("[C]", GeminiFlashClient.formatAsBracketedToken("C"))
    }

    @Test
    fun `controller switches language pill mode and stores api key`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FlashSolveController.initialize(context)

        FlashSolveController.setLanguage(context, AppLanguage.AR)
        assertEquals(AppLanguage.AR, FlashSolveController.language.value)

        FlashSolveController.setLanguage(context, AppLanguage.EN)
        assertEquals(AppLanguage.EN, FlashSolveController.language.value)

        FlashSolveController.setPillMode(PillMode.STANDBY)
        assertEquals(PillMode.STANDBY, FlashSolveController.pillMode.value)

        FlashSolveController.togglePillMode()
        assertEquals(PillMode.ACTIVE, FlashSolveController.pillMode.value)

        FlashSolveController.saveCustomApiKey(context, "AIzaSyTestKey12345")
        assertEquals("AIzaSyTestKey12345", FlashSolveController.getEffectiveApiKey())

        FlashSolveController.setMasterServiceEnabled(context, true)
        assertTrue(FlashSolveController.isServiceRunning.value)
        FlashSolveController.setMasterServiceEnabled(context, false)
    }
}
