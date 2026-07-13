package com.dreamjournal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TodoItem(
    val id: Long,
    val text: String,
    val isDone: Boolean = false
)
