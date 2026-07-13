package com.dreamjournal.app.domain.ai

import com.dreamjournal.app.domain.model.AiAnalysisResult
import com.dreamjournal.app.domain.model.AiChatMessage
import java.io.File

class MockSpeechToTextProvider : SpeechToTextProvider {
    override suspend fun transcribe(audioFile: File): Result<String> {
        return Result.success("")
    }
}

class MockTextAnalysisProvider : TextAnalysisProvider {
    override suspend fun analyzeDream(content: String): Result<AiAnalysisResult> {
        return Result.success(
            AiAnalysisResult(
                summary = "这个梦反映了你近期对变化的敏感，以及想掌控节奏的心理需求。",
                keywords = listOf("变化", "追逐", "好奇", "压力"),
                emotion = "紧张中带有期待",
                suggestions = listOf(
                    "起床后先记录一个最清晰的场景细节。",
                    "连续一周观察是否反复出现相同人物或地点。",
                    "对照白天与夜间的记录，看看哪些想法反复出现。"
                )
            )
        )
    }

    override suspend fun chat(
        dreamContent: String,
        history: List<AiChatMessage>,
        userMessage: String
    ): Result<String> {
        return Result.success("这是演示模式回复。若要真实大模型分析，请在设置页切换到 OpenAI Compatible 并填写 API Key。")
    }
}
