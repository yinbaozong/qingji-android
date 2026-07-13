package com.dreamjournal.app.data.repository

import android.content.Context
import android.net.Uri
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EntryAssetRepository(private val context: Context) {
    fun importPhoto(uri: Uri): Result<String> = runCatching {
        val photosDir = File(context.getExternalFilesDir(null), "entry_photos")
        if (!photosDir.exists()) photosDir.mkdirs()

        val ext = context.contentResolver.getType(uri)
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
            ?: "jpg"

        val file = File(
            photosDir,
            "photo_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"))}.$ext"
        )

        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: error("无法读取选中的图片")

        file.absolutePath
    }

    fun deletePhoto(path: String) {
        runCatching { File(path).takeIf(File::exists)?.delete() }
    }
}
