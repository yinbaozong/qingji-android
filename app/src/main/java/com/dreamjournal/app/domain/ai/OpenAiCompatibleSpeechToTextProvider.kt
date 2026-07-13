package com.dreamjournal.app.domain.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

@Serializable
data class TranscriptionResponse(
    val text: String = ""
)

class OpenAiCompatibleSpeechToTextProvider(
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
        require(apiKey.isNotBlank()) { "API key is empty. Please set API key in Settings." }
        require(audioFile.exists()) { "Audio file does not exist." }

        val mediaType = when (audioFile.extension.lowercase()) {
            "amr" -> "audio/amr"
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            else -> "audio/m4a"
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", model)
            .addFormDataPart(
                "file",
                audioFile.name,
                audioFile.asRequestBody(mediaType.toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(normalizeUrl(baseUrl, apiPath))
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                error("Speech request failed: ${response.code} $errorBody")
            }
            val raw = response.body?.string().orEmpty()
            val parsed = json.decodeFromString(TranscriptionResponse.serializer(), raw)
            parsed.text.ifBlank { error("Speech response is empty") }
        }
    }

    private fun normalizeUrl(baseUrl: String, apiPath: String): String {
        val trimmedBase = baseUrl.trimEnd('/')
        val trimmedPath = if (apiPath.startsWith("/")) apiPath else "/$apiPath"
        return "$trimmedBase$trimmedPath"
    }
}
