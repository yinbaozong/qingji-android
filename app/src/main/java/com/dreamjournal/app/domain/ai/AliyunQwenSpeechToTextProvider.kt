package com.dreamjournal.app.domain.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Base64
import java.util.concurrent.TimeUnit

class AliyunQwenSpeechToTextProvider(
    private val baseUrl: String,
    private val apiPath: String,
    private val apiKey: String,
    private val model: String,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SpeechToTextProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    override suspend fun transcribe(audioFile: File): Result<String> = runCatching {
        require(audioFile.exists()) { "录音文件不存在" }
        val normalizedApiKey = normalizeApiKey(apiKey)
        require(normalizedApiKey.isNotBlank()) { "阿里云 API Key 不能为空" }
        require(audioFile.length() <= 10L * 1024L * 1024L) {
            "当前阿里云方案适合 10MB 内、约 5 分钟内录音。更长文件建议后续接入 filetrans。"
        }

        execute(buildRequestBody(audioDataUri(audioFile)))
    }

    suspend fun validateConfig(): Result<String> = runCatching {
        val normalizedApiKey = normalizeApiKey(apiKey)
        require(normalizedApiKey.isNotBlank()) { "阿里云 API Key 不能为空" }
        execute(buildRequestBody("https://dashscope.oss-cn-beijing.aliyuncs.com/audios/welcome.mp3"))
        "阿里云语音识别配置可用"
    }

    private fun buildRequestBody(audioInput: String): String {
        return json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("model", JsonPrimitive(model))
                put(
                    "input",
                    buildJsonObject {
                        put(
                            "messages",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("role", JsonPrimitive("system"))
                                        put(
                                            "content",
                                            buildJsonArray {
                                                add(buildJsonObject { put("text", JsonPrimitive("")) })
                                            }
                                        )
                                    }
                                )
                                add(
                                    buildJsonObject {
                                        put("role", JsonPrimitive("user"))
                                        put(
                                            "content",
                                            buildJsonArray {
                                                add(buildJsonObject { put("audio", JsonPrimitive(audioInput)) })
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                )
                put(
                    "parameters",
                    buildJsonObject {
                        put(
                            "asr_options",
                            buildJsonObject {
                                put("enable_itn", JsonPrimitive(false))
                            }
                        )
                    }
                )
            }
        )
    }

    private fun execute(jsonBody: String): String {
        val request = Request.Builder()
            .url(normalizeUrl(baseUrl, apiPath))
            .addHeader("Authorization", "Bearer ${normalizeApiKey(apiKey)}")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error(buildAliyunErrorMessage(response.code, raw))
            }
            return extractTranscript(raw)
        }
    }

    private fun buildAliyunErrorMessage(httpCode: Int, raw: String): String {
        val lower = raw.lowercase()
        return when {
            "arrearage" in lower || "overdue-payment" in lower || "good standing" in lower ->
                "阿里云账号当前欠费或账户状态异常，请先到百炼控制台完成充值或恢复后再试。原始信息：$raw"
            "model_not_supported" in lower || "unsupported model" in lower ->
                "当前模型不支持该调用方式。请改用 qwen3-asr-flash。原始信息：$raw"
            "invalid_parameter_error" in lower || "required body invalid" in lower ->
                "阿里云请求体格式校验失败，我已经按官方同步接口重构；如果仍报这个错，请把最新错误再发我。原始信息：$raw"
            else -> "阿里云语音识别失败：$httpCode $raw"
        }
    }

    private fun extractTranscript(raw: String): String {
        val root = json.parseToJsonElement(raw).jsonObject
        val output = root["output"]?.jsonObject ?: error("阿里云返回为空")
        val choices = output["choices"]?.jsonArray.orEmpty()
        val first = choices.firstOrNull()?.jsonObject ?: error("阿里云返回为空")
        val message = first["message"]?.jsonObject ?: error("阿里云返回内容为空")
        val content = message["content"] ?: error("阿里云返回内容为空")

        return when (content) {
            is JsonPrimitive -> content.contentOrNull.orEmpty()
            is JsonArray -> content.firstNotNullOfOrNull { item ->
                item.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                    ?: item.jsonObject["transcript"]?.jsonPrimitive?.contentOrNull
            }.orEmpty()
            else -> ""
        }.trim().ifBlank { error("阿里云没有返回可用转写结果") }
    }

    private fun audioDataUri(audioFile: File): String {
        val mime = when (audioFile.extension.lowercase()) {
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            "pcm" -> "audio/pcm"
            else -> "audio/amr"
        }
        val base64 = Base64.getEncoder().encodeToString(audioFile.readBytes())
        return "data:$mime;base64,$base64"
    }

    private fun normalizeUrl(baseUrl: String, apiPath: String): String {
        val trimmedBase = baseUrl.trimEnd('/')
        val trimmedPath = if (apiPath.startsWith("/")) apiPath else "/$apiPath"
        return "$trimmedBase$trimmedPath"
    }

    private fun normalizeApiKey(value: String): String {
        return value.trim()
            .removePrefix("Bearer ")
            .removePrefix("bearer ")
            .removePrefix("\"")
            .removeSuffix("\"")
    }
}
