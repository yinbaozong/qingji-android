package com.dreamjournal.app.domain.settings

enum class ThemeMode {
    LIGHT,
    DARK
}

enum class SpeechProviderType {
    MOCK,
    SYSTEM,
    BAIDU_ASR,
    ALIYUN_QWEN_ASR,
    OPENAI_COMPATIBLE
}

enum class AnalysisProviderType {
    MOCK,
    OPENAI_COMPATIBLE
}

enum class AnalysisServiceType {
    MINIMAX,
    QWEN,
    DEEPSEEK,
    ZHIPU,
    CUSTOM
}

data class AnalysisServicePreset(
    val displayName: String,
    val baseUrl: String,
    val apiPath: String = "/chat/completions",
    val models: List<String>
) {
    val defaultModel: String get() = models.first()
}

fun AnalysisServiceType.preset(): AnalysisServicePreset = when (this) {
    AnalysisServiceType.MINIMAX -> AnalysisServicePreset(
        displayName = "MiniMax",
        baseUrl = "https://api.minimaxi.com/v1",
        models = listOf("MiniMax-M2.7", "MiniMax-M2.7-highspeed", "MiniMax-M2.5")
    )
    AnalysisServiceType.QWEN -> AnalysisServicePreset(
        displayName = "通义千问",
        baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        models = listOf("qwen3.7-plus", "qwen-plus", "qwen-turbo")
    )
    AnalysisServiceType.DEEPSEEK -> AnalysisServicePreset(
        displayName = "DeepSeek",
        baseUrl = "https://api.deepseek.com",
        models = listOf("deepseek-v4-flash", "deepseek-v4-pro")
    )
    AnalysisServiceType.ZHIPU -> AnalysisServicePreset(
        displayName = "智谱 GLM",
        baseUrl = "https://open.bigmodel.cn/api/paas/v4",
        models = listOf("glm-5.1", "glm-4.7")
    )
    AnalysisServiceType.CUSTOM -> AnalysisServicePreset(
        displayName = "自定义服务",
        baseUrl = "",
        models = listOf("model-name")
    )
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val speechProviderType: SpeechProviderType = SpeechProviderType.MOCK,
    val analysisProviderType: AnalysisProviderType = AnalysisProviderType.MOCK,
    val analysisServiceType: AnalysisServiceType = AnalysisServiceType.MINIMAX,
    val speechBaseUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1",
    val speechApiPath: String = "/audio/transcriptions",
    val speechModel: String = "whisper-1",
    val speechApiKey: String = "",
    val baiduSpeechUrl: String = "http://vop.baidu.com/server_api",
    val baiduTokenUrl: String = "https://aip.baidubce.com/oauth/2.0/token",
    val baiduApiKey: String = "",
    val baiduSecretKey: String = "",
    val baiduAppId: String = "",
    val baiduDevPid: String = "1537",
    val aliyunSpeechBaseUrl: String = "https://dashscope.aliyuncs.com/api/v1",
    val aliyunSpeechApiPath: String = "/services/aigc/multimodal-generation/generation",
    val aliyunSpeechApiKey: String = "",
    val aliyunSpeechModel: String = "qwen3-asr-flash",
    val analysisBaseUrl: String = "https://api.minimaxi.com/v1",
    val analysisApiPath: String = "/chat/completions",
    val analysisModel: String = "MiniMax-M2.5",
    val analysisApiKey: String = "",
    val minimaxAnalysisApiKey: String = "",
    val qwenAnalysisApiKey: String = "",
    val deepSeekAnalysisApiKey: String = "",
    val zhipuAnalysisApiKey: String = "",
    val customAnalysisApiKey: String = "",
    val analysisPromptTemplate: String = "",
    val customTags: String = ""
)
