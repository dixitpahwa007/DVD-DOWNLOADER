package com.dixit.video.downloader

import android.content.Context
import android.net.Uri
import java.util.Locale

/** Central feature layer for lawful, non-DRM media exposed by a page or direct URL. */
object SnapTubeFeatureEngine {
    data class StreamOption(val url: String, val format: String, val quality: String, val kind: String)
    interface SiteAdapter { fun matches(uri: Uri): Boolean; fun describe(): String; fun isPublicProfile(uri: Uri): Boolean = false }

    private val adapters = listOf(
        "youtube" to "YouTube", "facebook" to "Facebook", "instagram" to "Instagram",
        "tiktok" to "TikTok", "x" to "X", "vimeo" to "Vimeo",
        "dailymotion" to "Dailymotion", "reddit" to "Reddit", "twitch" to "Twitch",
        "soundcloud" to "SoundCloud"
    ).map { (id, name) -> object : SiteAdapter {
        override fun matches(uri: Uri): Boolean = PublicMediaDiscovery.inspectUrl(uri.toString()).id == id
        override fun describe(): String = name + " public-media adapter"
        override fun isPublicProfile(uri: Uri): Boolean = PublicMediaDiscovery.inspectUrl(uri.toString()).publicProfile
    }} + object : SiteAdapter {
        override fun matches(uri: Uri) = uri.scheme in listOf("http", "https")
        override fun describe() = "Generic public HTML5/direct-media adapter"
    }

    fun adapterFor(url: String): SiteAdapter? = runCatching { Uri.parse(url) }.getOrNull()?.let { u -> adapters.firstOrNull { it.matches(u) } }

    fun option(url: String, width: Int = 0, height: Int = 0, mime: String = ""): StreamOption {
        val p = url.substringBefore('?').substringBefore('#').lowercase(Locale.US)
        val format = when {
            mime.contains("mp4") || p.endsWith(".mp4") || p.endsWith(".m4v") -> "MP4"
            mime.contains("webm") || p.endsWith(".webm") -> "WebM"
            mime.contains("m3u8") || p.endsWith(".m3u8") -> "HLS"
            p.endsWith(".m4a") -> "M4A"
            p.endsWith(".mp3") -> "MP3"
            p.endsWith(".opus") -> "Opus"
            else -> "Media"
        }
        val quality = when { height >= 4320 -> "8K"; height >= 2160 -> "4K"; height >= 1440 -> "2K"; height >= 1080 -> "1080p"; height >= 720 -> "720p"; height >= 480 -> "480p"; height >= 360 -> "360p"; height > 0 -> "${height}p"; else -> "Auto" }
        return StreamOption(url, format, quality, if (format in listOf("MP3", "M4A", "Opus")) "Audio" else "Video")
    }

    fun settings(context: Context) = context.getSharedPreferences("dvd_settings", Context.MODE_PRIVATE)
}
