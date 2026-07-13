package com.dreamjournal.app.domain.model

enum class RecordType {
    DAY,
    DREAM;

    val titleLabel: String
        get() = if (this == DAY) "日常记录" else "夜间记录"

    val shortLabel: String
        get() = if (this == DAY) "日常" else "夜间"

    companion object {
        fun fromStorage(value: String?): RecordType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DREAM
        }
    }
}
