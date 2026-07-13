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
    QWEN
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
    val analysisPromptTemplate: String = "",
    val customTags: String = ""
)
