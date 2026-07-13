package com.dreamjournal.app.domain.ai

import com.dreamjournal.app.domain.model.AiAnalysisResult
import com.dreamjournal.app.domain.model.AiChatMessage
import java.io.File

interface SpeechToTextProvider {
    suspend fun transcribe(audioFile: File): Result<String>
}

interface TextAnalysisProvider {
    suspend fun analyzeDream(content: String): Result<AiAnalysisResult>
    suspend fun chat(
        dreamContent: String,
        history: List<AiChatMessage>,
        userMessage: String
    ): Result<String>
}
