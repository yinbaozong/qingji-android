package com.dreamjournal.app.domain.ai

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.2
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice> = emptyList()
)

@Serializable
data class Choice(
    val message: ChatMessage
)

@Serializable
data class AnalysisJson(
    val summary: String = "",
    val keywords: List<String> = emptyList(),
    val emotion: String = "",
    val suggestions: List<String> = emptyList()
)

@Serializable
data class ChatJson(
    val reply: String = ""
)
