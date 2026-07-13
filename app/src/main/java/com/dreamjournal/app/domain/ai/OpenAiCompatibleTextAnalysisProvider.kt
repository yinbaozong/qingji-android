package com.dreamjournal.app.domain.ai

import com.dreamjournal.app.domain.model.AiAnalysisResult
import com.dreamjournal.app.domain.model.AiChatMessage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OpenAiCompatibleTextAnalysisProvider(
    private val baseUrl: String,
    private val apiPath: String,
    private val apiKey: String,
    private val model: String,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : TextAnalysisProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override suspend fun analyzeDream(content: String): Result<AiAnalysisResult> = runCatching {
        val prompt = buildString {
            append("你是一位温和、克制、善于提炼重点的个人记录整理助手。")
            append("请基于用户提供的记录进行整理，不要过度心理诊断，不要编造不存在的细节。")
            append("只返回严格 JSON，字段固定为：summary, keywords, emotion, suggestions。")
            append("其中 keywords 和 suggestions 必须是数组；summary 和 emotion 为字符串；不要输出 markdown，不要解释。")
            append("\n记录文本：\n")
            append(content)
        }
        val reply = request(prompt)
        val parsed = json.decodeFromString(AnalysisJson.serializer(), extractJson(reply))
        AiAnalysisResult(
            summary = parsed.summary,
            keywords = parsed.keywords,
            emotion = parsed.emotion,
            suggestions = parsed.suggestions
        )
    }

    override suspend fun chat(
        dreamContent: String,
        history: List<AiChatMessage>,
        userMessage: String
    ): Result<String> = runCatching {
        val messages = buildList {
            val historyBlock = history.takeLast(8).joinToString("\n") { item ->
                "${item.role}: ${item.content}"
            }
            val prompt = buildString {
                append("你是一位温和、可靠的个人记录助手。回答要简洁、具体、自然，避免神秘化表达。")
                append("只返回严格 JSON：{\\\"reply\\\":\\\"...\\\"}。")
                append("\n\n记录内容：\n")
                append(dreamContent)
                if (historyBlock.isNotBlank()) {
                    append("\n\n最近对话：\n")
                    append(historyBlock)
                }
                append("\n\n用户问题：\n")
                append(userMessage)
            }
            add(ChatMessage(role = "user", content = prompt))
        }
        val responseText = requestWithMessages(messages)
        val parsed = json.decodeFromString(ChatJson.serializer(), extractJson(responseText))
        parsed.reply
    }

    private fun request(prompt: String): String {
        return requestWithMessages(
            listOf(
                ChatMessage(role = "user", content = prompt)
            )
        )
    }

    private fun requestWithMessages(messages: List<ChatMessage>): String {
        val normalizedApiKey = apiKey
            .trim()
            .removePrefix("Bearer ")
            .removePrefix("bearer ")
            .removePrefix("\"")
            .removeSuffix("\"")
        require(normalizedApiKey.isNotBlank()) { "API Key 为空，请先在设置中填写。" }

        val body = ChatCompletionRequest(
            model = model,
            messages = messages
        )
        val jsonBody = json.encodeToString(body)

        val request = Request.Builder()
            .url(normalizeUrl(baseUrl, apiPath))
            .addHeader("Authorization", "Bearer $normalizedApiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                error("AI 请求失败：${response.code} $errorBody")
            }
            val responseBody = response.body?.string().orEmpty()
            val parsed = json.decodeFromString(ChatCompletionResponse.serializer(), responseBody)
            return parsed.choices.firstOrNull()?.message?.content
                ?: error("AI 返回内容为空")
        }
    }

    private fun extractJson(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return raw
        return raw.substring(start, end + 1)
    }

    private fun normalizeUrl(baseUrl: String, apiPath: String): String {
        val trimmedBase = baseUrl.trimEnd('/')
        val trimmedPath = if (apiPath.startsWith("/")) apiPath else "/$apiPath"
        return "$trimmedBase$trimmedPath"
    }
}
