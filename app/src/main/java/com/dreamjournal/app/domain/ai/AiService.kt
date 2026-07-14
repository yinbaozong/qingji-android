package com.dreamjournal.app.domain.ai

import com.dreamjournal.app.data.repository.SettingsRepository
import com.dreamjournal.app.domain.model.AiAnalysisResult
import com.dreamjournal.app.domain.model.AiChatMessage
import com.dreamjournal.app.domain.model.RecordType
import com.dreamjournal.app.domain.settings.AnalysisProviderType
import com.dreamjournal.app.domain.settings.SpeechProviderType
import com.dreamjournal.app.domain.settings.preset
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiService(
    private val settingsRepository: SettingsRepository
) {
    suspend fun validateSpeechConfiguration(): Result<String> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getCurrentSettings()
        when (settings.speechProviderType) {
            SpeechProviderType.MOCK -> Result.success("演示模式无需额外配置")
            SpeechProviderType.SYSTEM -> Result.success("系统语音识别无需云端配置")
            SpeechProviderType.BAIDU_ASR -> BaiduSpeechToTextProvider(
                speechUrl = settings.baiduSpeechUrl,
                tokenUrl = settings.baiduTokenUrl,
                apiKey = settings.baiduApiKey,
                secretKey = settings.baiduSecretKey,
                appId = settings.baiduAppId,
                devPid = settings.baiduDevPid
            ).validateConfig().map { "百度鉴权成功" }
            SpeechProviderType.ALIYUN_QWEN_ASR -> AliyunQwenSpeechToTextProvider(
                baseUrl = settings.aliyunSpeechBaseUrl,
                apiPath = settings.aliyunSpeechApiPath,
                apiKey = settings.aliyunSpeechApiKey,
                model = settings.aliyunSpeechModel
            ).validateConfig()
            SpeechProviderType.OPENAI_COMPATIBLE -> runCatching {
                require(settings.speechBaseUrl.isNotBlank()) { "语音 Base URL 不能为空" }
                require(settings.speechApiPath.isNotBlank()) { "语音 API Path 不能为空" }
                require(settings.speechModel.isNotBlank()) { "语音模型不能为空" }
                require(settings.speechApiKey.isNotBlank()) { "语音 API Key 不能为空" }
                "OpenAI Compatible 基础配置已填写"
            }
        }
    }

    suspend fun transcribe(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getCurrentSettings()
        val provider: SpeechToTextProvider = when (settings.speechProviderType) {
            SpeechProviderType.MOCK -> MockSpeechToTextProvider()
            SpeechProviderType.SYSTEM -> MockSpeechToTextProvider()
            SpeechProviderType.BAIDU_ASR -> BaiduSpeechToTextProvider(
                speechUrl = settings.baiduSpeechUrl,
                tokenUrl = settings.baiduTokenUrl,
                apiKey = settings.baiduApiKey,
                secretKey = settings.baiduSecretKey,
                appId = settings.baiduAppId,
                devPid = settings.baiduDevPid
            )
            SpeechProviderType.ALIYUN_QWEN_ASR -> AliyunQwenSpeechToTextProvider(
                baseUrl = settings.aliyunSpeechBaseUrl,
                apiPath = settings.aliyunSpeechApiPath,
                apiKey = settings.aliyunSpeechApiKey,
                model = settings.aliyunSpeechModel
            )
            SpeechProviderType.OPENAI_COMPATIBLE -> OpenAiCompatibleSpeechToTextProvider(
                baseUrl = settings.speechBaseUrl,
                apiPath = settings.speechApiPath,
                apiKey = settings.speechApiKey,
                model = settings.speechModel
            )
        }
        provider.transcribe(audioFile)
    }

    suspend fun analyze(content: String, recordType: RecordType = RecordType.DREAM): Result<AiAnalysisResult> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getCurrentSettings()
        val provider: TextAnalysisProvider = when (settings.analysisProviderType) {
            AnalysisProviderType.MOCK -> MockTextAnalysisProvider()
            AnalysisProviderType.OPENAI_COMPATIBLE -> OpenAiCompatibleTextAnalysisProvider(
                baseUrl = settings.analysisBaseUrl,
                apiPath = settings.analysisApiPath,
                apiKey = settings.analysisApiKey,
                model = settings.analysisModel
            )
        }
        val customPromptPrefix = settings.analysisPromptTemplate
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let { "请优先遵循下面这段用户自定义总结提示词，再进行整理：\n$it\n\n" }
            .orEmpty()
        val contextPrefix = if (recordType == RecordType.DAY) {
            "以下内容是一条白天记录，不是梦境，请按日间经历、情绪与线索来整理：\n"
        } else {
            "以下内容是一条梦境记录，请按梦境意象、情绪与反复出现的线索来整理：\n"
        }
        provider.analyzeDream(customPromptPrefix + contextPrefix + content)
    }

    suspend fun validateAnalysisConfiguration(): Result<String> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getCurrentSettings()
        when (settings.analysisProviderType) {
            AnalysisProviderType.MOCK -> Result.success("演示模式无需额外配置")
            AnalysisProviderType.OPENAI_COMPATIBLE -> runCatching {
                val serviceName = settings.analysisServiceType.preset().displayName
                require(settings.analysisApiKey.isNotBlank()) { "$serviceName API Key 不能为空" }
                require(settings.analysisBaseUrl.isNotBlank()) { "$serviceName 服务地址不能为空" }
                require(settings.analysisModel.isNotBlank()) { "$serviceName 模型不能为空" }
                val provider = OpenAiCompatibleTextAnalysisProvider(
                    baseUrl = settings.analysisBaseUrl,
                    apiPath = settings.analysisApiPath,
                    apiKey = settings.analysisApiKey,
                    model = settings.analysisModel
                )
                provider.analyzeDream("这是一段用于测试配置的个人记录：今天完成了计划中的事情，晚上想简单回顾一下。").getOrThrow()
                "$serviceName 配置可用"
            }
        }
    }

    suspend fun chat(
        dreamContent: String,
        history: List<AiChatMessage>,
        userMessage: String,
        recordType: RecordType = RecordType.DREAM
    ): Result<String> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getCurrentSettings()
        val provider: TextAnalysisProvider = when (settings.analysisProviderType) {
            AnalysisProviderType.MOCK -> MockTextAnalysisProvider()
            AnalysisProviderType.OPENAI_COMPATIBLE -> OpenAiCompatibleTextAnalysisProvider(
                baseUrl = settings.analysisBaseUrl,
                apiPath = settings.analysisApiPath,
                apiKey = settings.analysisApiKey,
                model = settings.analysisModel
            )
        }
        val contextPrefix = if (recordType == RecordType.DAY) {
            "这是一条白天记录，请把它当作日间经历来交流，不要当作梦来解读。\n"
        } else {
            ""
        }
        provider.chat(contextPrefix + dreamContent, history, userMessage)
    }
}
