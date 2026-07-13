package com.dreamjournal.app.domain.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

@Serializable
private data class BaiduTokenResponse(
    val access_token: String? = null,
    val error: String? = null,
    val error_description: String? = null
)

@Serializable
private data class BaiduAsrRequest(
    val format: String,
    val rate: Int,
    val channel: Int,
    val token: String,
    val cuid: String,
    val dev_pid: Int,
    val speech: String,
    val len: Int
)

@Serializable
private data class BaiduAsrResponse(
    val result: List<String>? = null,
    val err_no: Int? = null,
    val err_msg: String? = null
)

class BaiduSpeechToTextProvider(
    private val speechUrl: String,
    private val tokenUrl: String,
    private val apiKey: String,
    private val secretKey: String,
    private val appId: String,
    private val devPid: String,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SpeechToTextProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    override suspend fun transcribe(audioFile: File): Result<String> = runCatching {
        require(apiKey.isNotBlank()) { "百度 API Key 不能为空" }
        require(secretKey.isNotBlank()) { "百度 Secret Key 不能为空" }
        require(audioFile.exists()) { "录音文件不存在" }

        val token = fetchAccessToken()
        val format = audioFile.extension.lowercase().ifBlank { "amr" }
        val rate = if (format == "amr") 8000 else 16000
        val payload = BaiduAsrRequest(
            format = format,
            rate = rate,
            channel = 1,
            token = token,
            cuid = appId.ifBlank { UUID.randomUUID().toString() },
            dev_pid = devPid.toIntOrNull() ?: 1537,
            speech = Base64.getEncoder().encodeToString(audioFile.readBytes()),
            len = audioFile.length().toInt()
        )

        val request = Request.Builder()
            .url(speechUrl)
            .addHeader("Content-Type", "application/json")
            .post(json.encodeToString(payload).toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("百度识别请求失败：${response.code} $raw")
            }
            val parsed = json.decodeFromString(BaiduAsrResponse.serializer(), raw)
            if (parsed.err_no != null && parsed.err_no != 0) {
                error("百度识别失败：${parsed.err_msg ?: "未知错误"}")
            }
            parsed.result?.joinToString("")?.trim().orEmpty()
                .ifBlank { error("百度识别结果为空") }
        }
    }

    suspend fun validateConfig(): Result<String> = runCatching {
        fetchAccessToken()
    }

    private fun fetchAccessToken(): String {
        val formBody = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("client_id", apiKey)
            .add("client_secret", secretKey)
            .build()

        val request = Request.Builder()
            .url(tokenUrl)
            .post(formBody)
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("百度 Token 获取失败：${response.code} $raw")
            }
            val parsed = json.decodeFromString(BaiduTokenResponse.serializer(), raw)
            return parsed.access_token
                ?: error(parsed.error_description ?: parsed.error ?: "未拿到百度 access_token")
        }
    }
}
