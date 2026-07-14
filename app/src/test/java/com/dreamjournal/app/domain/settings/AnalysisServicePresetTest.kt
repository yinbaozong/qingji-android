package com.dreamjournal.app.domain.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisServicePresetTest {
    @Test
    fun namedServicesExposeReadyToUseChatCompletionPresets() {
        val namedServices = AnalysisServiceType.entries - AnalysisServiceType.CUSTOM

        namedServices.forEach { service ->
            val preset = service.preset()
            assertTrue(preset.baseUrl.startsWith("https://"))
            assertEquals("/chat/completions", preset.apiPath)
            assertTrue(preset.defaultModel.isNotBlank())
        }
    }

    @Test
    fun deepSeekUsesCurrentOfficialOpenAiCompatibleEndpoint() {
        val preset = AnalysisServiceType.DEEPSEEK.preset()

        assertEquals("https://api.deepseek.com", preset.baseUrl)
        assertEquals("deepseek-v4-flash", preset.defaultModel)
    }
}
