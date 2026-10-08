package com.dixit.video.downloader

import android.content.Context
import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Discovers media variants actually exposed by a public URL. No DRM/auth bypass. */
object StreamCatalog {
    data class Variant(
        val url: String,
        val format: String,
        val quality: String,
        val width: Int = 0,
        val height: Int = 0,
        val fps: Int = 0,
        val videoCodec: String = "Auto",
        val audioCodec: String = "Auto",
        val bitrate: Long = 0L,
        val kind: String = "video",
        val audioGroup: String = ""
    )

    fun inspect(context: Context, rawUrl: String): Result<List<Variant>> = runCatching {
        val url = rawUrl.trim()
        require(url.startsWith("http://") || url.startsWith("https://")) { "Only HTTP/HTTPS URLs are supported" }
        val path = Uri.parse(url).path.orEmpty().lowercase(Locale.US)
        when {
            path.endsWith(".m3u8") -> parseHls(url, fetch(url))
            isDirectMedia(path) -> listOf(direct(url))
            else -> parseHtml(url, fetch(url))
        }.distinctBy { it.url }
    }

    private fun parseHtml(base: String, html: String): List<Variant> {
        val found = linkedSetOf<String>()
        val patterns = listOf(
            "(?i)(?:src|href|content|data-src)\\s*=\\s*[\\\"']([^\\\"']+\\.(?:mp4|m4v|webm|mkv|mov|mp3|m4a|aac|opus|ogg|wav|m3u8)(?:\\?[^\\\"']*)?)[\\\"']",
            "(?i)(https?://[^\\\"'\\s<>]+\\.(?:mp4|m4v|webm|mkv|mov|mp3|m4a|aac|opus|ogg|wav|m3u8)(?:\\?[^\\\"'\\s<>]*)?)"
        )
        patterns.forEach { Regex(it).findAll(html).forEach { m -> found += resolve(base, m.groupValues[1]) } }
        return found.flatMap { u ->
            if (u.substringBefore('?').lowercase(Locale.US).endsWith(".m3u8")) runCatching { parseHls(u, fetch(u)) }.getOrDefault(emptyList()) else listOf(direct(u))
        }
    }

    private fun parseHls(url: String, body: String): List<Variant> {
        val lines = body.lines().map { it.trim() }
        val out = mutableListOf<Variant>()
        val audioGroups = mutableMapOf<String, String>()
        for (line in lines) {
            if (line.startsWith("#EXT-X-MEDIA", true)) {
                val a = attrs(line.substringAfter(':'))
                if (a["TYPE"].equals("AUDIO", true) && !a["GROUP-ID"].isNullOrBlank() && !a["URI"].isNullOrBlank()) {
                    audioGroups[a["GROUP-ID"]!!] = resolve(url, a["URI"]!!.trim('"'))
                }
            }
        }
        var pending: Map<String, String>? = null
        for (line in lines) {
            if (line.startsWith("#EXT-X-STREAM-INF", true)) {
                pending = attrs(line.substringAfter(':'))
            } else if (pending != null && line.isNotBlank() && !line.startsWith('#')) {
                val p = pending!!
                val absolute = resolve(url, line)
                val res = p["RESOLUTION"]?.split('x', 'X')
                val width = res?.getOrNull(0)?.toIntOrNull() ?: 0
                val height = res?.getOrNull(1)?.toIntOrNull() ?: 0
                val fps = p["FRAME-RATE"]?.toDoubleOrNull()?.toInt() ?: 0
                val bandwidth = p["AVERAGE-BANDWIDTH"]?.toLongOrNull() ?: p["BANDWIDTH"]?.toLongOrNull() ?: 0L
                val codecs = p["CODECS"].orEmpty().split(',').map { it.trim() }
                val vc = codecs.firstOrNull { !it.startsWith("mp4a", true) && !it.startsWith("opus", true) && !it.startsWith("ac-3", true) && !it.startsWith("ec-3", true) } ?: "Auto"
                val ac = codecs.firstOrNull { it.startsWith("mp4a", true) || it.startsWith("opus", true) || it.startsWith("ac-3", true) || it.startsWith("ec-3", true) } ?: "Auto"
                out += Variant(absolute, "HLS", quality(height), width, height, fps, vc, ac, bandwidth, "video", p["AUDIO"].orEmpty())
                pending = null
            }
        }
        audioGroups.forEach { (group, audioUrl) ->
            out += Variant(audioUrl, "HLS", "Audio", audioCodec = "HLS-Audio", kind = "audio", audioGroup = group)
        }
        return if (out.isNotEmpty()) out else listOf(direct(url).copy(format = "HLS"))
    }

    private fun attrs(s: String): Map<String, String> = Regex("([A-Z0-9-]+)=((?:\"[^\"]*\")|(?:[^,]*))", RegexOption.IGNORE_CASE)
        .findAll(s).associate { it.groupValues[1].uppercase(Locale.US) to it.groupValues[2].trim() }

    private fun direct(url: String): Variant {
        val p = url.substringBefore('?').substringBefore('#').lowercase(Locale.US)
        val audio = p.endsWith(".mp3") || p.endsWith(".m4a") || p.endsWith(".aac") || p.endsWith(".opus") || p.endsWith(".ogg") || p.endsWith(".wav")
        val format = when {
            p.endsWith(".mp4") || p.endsWith(".m4v") -> "MP4"; p.endsWith(".webm") -> "WebM"; p.endsWith(".mkv") -> "MKV"; p.endsWith(".mov") -> "MOV"
            p.endsWith(".m3u8") -> "HLS"; p.endsWith(".m4a") -> "M4A"; p.endsWith(".mp3") -> "MP3"; p.endsWith(".opus") -> "Opus" else -> "Media"
        }
        return Variant(url, format, if (audio) "Audio" else "Auto", audioCodec = if (audio) format else "Auto", kind = if (audio) "audio" else "video")
    }

    private fun fetch(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 12000; c.readTimeout = 20000; c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "Dixit-Media-Downloader/5.2")
        return c.inputStream.bufferedReader().use { it.readText() }.also { c.disconnect() }
    }

    private fun resolve(base: String, ref: String): String = runCatching { URL(URL(base), ref).toString() }.getOrDefault(ref)
    private fun isDirectMedia(path: String) = listOf(".mp4", ".m4v", ".webm", ".mkv", ".mov", ".mp3", ".m4a", ".aac", ".opus", ".ogg", ".wav", ".m3u8").any { path.endsWith(it) }
    private fun quality(h: Int) = when { h >= 4320 -> "8K"; h >= 2160 -> "4K"; h >= 1440 -> "2K"; h >= 1080 -> "1080p"; h >= 720 -> "720p"; h >= 480 -> "480p"; h >= 360 -> "360p"; h > 0 -> "${h}p"; else -> "Auto" }
}
