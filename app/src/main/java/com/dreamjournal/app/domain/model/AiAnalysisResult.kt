package com.dreamjournal.app.domain.model

data class AiAnalysisResult(
    val summary: String,
    val keywords: List<String>,
    val emotion: String,
    val suggestions: List<String>
)
