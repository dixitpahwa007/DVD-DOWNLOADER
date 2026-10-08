package com.dixit.video.downloader

import android.content.Context
import java.io.File

object MediaOrganizer {
    fun directory(context: Context, category: String): File {
        val root = context.getExternalFilesDir("Media") ?: File(context.filesDir,"Media")
        val dir = File(root, if (category == "audio") "Music" else "Movies")
        dir.mkdirs(); return dir
    }
    fun safeName(title: String, extension: String): String {
        val base=title.trim().ifBlank { "media_${System.currentTimeMillis()}" }.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim('.')
        return if (base.lowercase().endsWith(".$extension")) base else "$base.$extension"
    }
    fun unique(file: File): File { if (!file.exists()) return file; var i=1; var f=file; while(f.exists()){f=File(file.parentFile,"${file.nameWithoutExtension} ($i).${file.extension}");i++};return f }
}
