package com.dreamjournal.app.domain.model

enum class ExportFormat(val displayName: String, val extension: String) {
    MARKDOWN("Markdown", "md"),
    TXT("纯文字 TXT", "txt")
}
