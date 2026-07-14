package com.dreamjournal.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dreamjournal.app.domain.settings.AnalysisProviderType
import com.dreamjournal.app.domain.settings.AnalysisServiceType
import com.dreamjournal.app.domain.settings.AppSettings
import com.dreamjournal.app.domain.settings.SpeechProviderType
import com.dreamjournal.app.domain.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val speechProviderType = stringPreferencesKey("speech_provider_type")
        val analysisProviderType = stringPreferencesKey("analysis_provider_type")
        val analysisServiceType = stringPreferencesKey("analysis_service_type")
        val speechBaseUrl = stringPreferencesKey("speech_base_url")
        val speechApiPath = stringPreferencesKey("speech_api_path")
        val speechModel = stringPreferencesKey("speech_model")
        val speechApiKey = stringPreferencesKey("speech_api_key")
        val baiduSpeechUrl = stringPreferencesKey("baidu_speech_url")
        val baiduTokenUrl = stringPreferencesKey("baidu_token_url")
        val baiduApiKey = stringPreferencesKey("baidu_api_key")
        val baiduSecretKey = stringPreferencesKey("baidu_secret_key")
        val baiduAppId = stringPreferencesKey("baidu_app_id")
        val baiduDevPid = stringPreferencesKey("baidu_dev_pid")
        val aliyunSpeechBaseUrl = stringPreferencesKey("aliyun_speech_base_url")
        val aliyunSpeechApiPath = stringPreferencesKey("aliyun_speech_api_path")
        val aliyunSpeechApiKey = stringPreferencesKey("aliyun_speech_api_key")
        val aliyunSpeechModel = stringPreferencesKey("aliyun_speech_model")
        val analysisBaseUrl = stringPreferencesKey("analysis_base_url")
        val analysisApiPath = stringPreferencesKey("analysis_api_path")
        val analysisModel = stringPreferencesKey("analysis_model")
        val analysisApiKey = stringPreferencesKey("analysis_api_key")
        val minimaxAnalysisApiKey = stringPreferencesKey("minimax_analysis_api_key")
        val qwenAnalysisApiKey = stringPreferencesKey("qwen_analysis_api_key")
        val deepSeekAnalysisApiKey = stringPreferencesKey("deepseek_analysis_api_key")
        val zhipuAnalysisApiKey = stringPreferencesKey("zhipu_analysis_api_key")
        val customAnalysisApiKey = stringPreferencesKey("custom_analysis_api_key")
        val analysisPromptTemplate = stringPreferencesKey("analysis_prompt_template")
        val customTags = stringPreferencesKey("custom_tags")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        prefs.toSettings()
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.themeMode] = next.themeMode.name
            prefs[Keys.speechProviderType] = next.speechProviderType.name
            prefs[Keys.analysisProviderType] = next.analysisProviderType.name
            prefs[Keys.analysisServiceType] = next.analysisServiceType.name
            prefs[Keys.speechBaseUrl] = next.speechBaseUrl
            prefs[Keys.speechApiPath] = next.speechApiPath
            prefs[Keys.speechModel] = next.speechModel
            prefs[Keys.speechApiKey] = next.speechApiKey
            prefs[Keys.baiduSpeechUrl] = next.baiduSpeechUrl
            prefs[Keys.baiduTokenUrl] = next.baiduTokenUrl
            prefs[Keys.baiduApiKey] = next.baiduApiKey
            prefs[Keys.baiduSecretKey] = next.baiduSecretKey
            prefs[Keys.baiduAppId] = next.baiduAppId
            prefs[Keys.baiduDevPid] = next.baiduDevPid
            prefs[Keys.aliyunSpeechBaseUrl] = next.aliyunSpeechBaseUrl
            prefs[Keys.aliyunSpeechApiPath] = next.aliyunSpeechApiPath
            prefs[Keys.aliyunSpeechApiKey] = next.aliyunSpeechApiKey
            prefs[Keys.aliyunSpeechModel] = next.aliyunSpeechModel
            prefs[Keys.analysisBaseUrl] = next.analysisBaseUrl
            prefs[Keys.analysisApiPath] = next.analysisApiPath
            prefs[Keys.analysisModel] = next.analysisModel
            prefs[Keys.analysisApiKey] = next.analysisApiKey
            prefs[Keys.minimaxAnalysisApiKey] = next.minimaxAnalysisApiKey
            prefs[Keys.qwenAnalysisApiKey] = next.qwenAnalysisApiKey
            prefs[Keys.deepSeekAnalysisApiKey] = next.deepSeekAnalysisApiKey
            prefs[Keys.zhipuAnalysisApiKey] = next.zhipuAnalysisApiKey
            prefs[Keys.customAnalysisApiKey] = next.customAnalysisApiKey
            prefs[Keys.analysisPromptTemplate] = next.analysisPromptTemplate
            prefs[Keys.customTags] = next.customTags
        }
    }

    suspend fun getCurrentSettings(): AppSettings {
        return settingsFlow.first()
    }

    private fun Preferences.toSettings(): AppSettings {
        val themeMode = this[Keys.themeMode]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.LIGHT
        val speechType = this[Keys.speechProviderType]
            ?.let { runCatching { SpeechProviderType.valueOf(it) }.getOrNull() }
            ?: SpeechProviderType.MOCK
        val analysisType = this[Keys.analysisProviderType]
            ?.let { runCatching { AnalysisProviderType.valueOf(it) }.getOrNull() }
            ?: AnalysisProviderType.MOCK
        val analysisService = this[Keys.analysisServiceType]
            ?.let { runCatching { AnalysisServiceType.valueOf(it) }.getOrNull() }
            ?: AppSettings().analysisServiceType
        val legacyAnalysisApiKey = this[Keys.analysisApiKey].orEmpty()
        val minimaxKey = this[Keys.minimaxAnalysisApiKey]
            ?: legacyAnalysisApiKey.takeIf { analysisService == AnalysisServiceType.MINIMAX }.orEmpty()
        val qwenKey = this[Keys.qwenAnalysisApiKey]
            ?: legacyAnalysisApiKey.takeIf { analysisService == AnalysisServiceType.QWEN }.orEmpty()
        val deepSeekKey = this[Keys.deepSeekAnalysisApiKey]
            ?: legacyAnalysisApiKey.takeIf { analysisService == AnalysisServiceType.DEEPSEEK }.orEmpty()
        val zhipuKey = this[Keys.zhipuAnalysisApiKey]
            ?: legacyAnalysisApiKey.takeIf { analysisService == AnalysisServiceType.ZHIPU }.orEmpty()
        val customKey = this[Keys.customAnalysisApiKey]
            ?: legacyAnalysisApiKey.takeIf { analysisService == AnalysisServiceType.CUSTOM }.orEmpty()
        val activeAnalysisKey = when (analysisService) {
            AnalysisServiceType.MINIMAX -> minimaxKey
            AnalysisServiceType.QWEN -> qwenKey
            AnalysisServiceType.DEEPSEEK -> deepSeekKey
            AnalysisServiceType.ZHIPU -> zhipuKey
            AnalysisServiceType.CUSTOM -> customKey
        }

        return AppSettings(
            themeMode = themeMode,
            speechProviderType = speechType,
            analysisProviderType = analysisType,
            analysisServiceType = analysisService,
            speechBaseUrl = this[Keys.speechBaseUrl] ?: AppSettings().speechBaseUrl,
            speechApiPath = this[Keys.speechApiPath] ?: AppSettings().speechApiPath,
            speechModel = this[Keys.speechModel] ?: AppSettings().speechModel,
            speechApiKey = this[Keys.speechApiKey] ?: "",
            baiduSpeechUrl = this[Keys.baiduSpeechUrl] ?: AppSettings().baiduSpeechUrl,
            baiduTokenUrl = this[Keys.baiduTokenUrl] ?: AppSettings().baiduTokenUrl,
            baiduApiKey = this[Keys.baiduApiKey] ?: "",
            baiduSecretKey = this[Keys.baiduSecretKey] ?: "",
            baiduAppId = this[Keys.baiduAppId] ?: "",
            baiduDevPid = this[Keys.baiduDevPid] ?: AppSettings().baiduDevPid,
            aliyunSpeechBaseUrl = this[Keys.aliyunSpeechBaseUrl] ?: AppSettings().aliyunSpeechBaseUrl,
            aliyunSpeechApiPath = this[Keys.aliyunSpeechApiPath] ?: AppSettings().aliyunSpeechApiPath,
            aliyunSpeechApiKey = this[Keys.aliyunSpeechApiKey] ?: "",
            aliyunSpeechModel = this[Keys.aliyunSpeechModel] ?: AppSettings().aliyunSpeechModel,
            analysisBaseUrl = this[Keys.analysisBaseUrl] ?: AppSettings().analysisBaseUrl,
            analysisApiPath = this[Keys.analysisApiPath] ?: AppSettings().analysisApiPath,
            analysisModel = this[Keys.analysisModel] ?: AppSettings().analysisModel,
            analysisApiKey = activeAnalysisKey,
            minimaxAnalysisApiKey = minimaxKey,
            qwenAnalysisApiKey = qwenKey,
            deepSeekAnalysisApiKey = deepSeekKey,
            zhipuAnalysisApiKey = zhipuKey,
            customAnalysisApiKey = customKey,
            analysisPromptTemplate = this[Keys.analysisPromptTemplate] ?: "",
            customTags = this[Keys.customTags] ?: ""
        )
    }
}
