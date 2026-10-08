package com.dixit.video.downloader

import android.content.Context
import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Public-page discovery only. It extracts media URLs already exposed by the
 * page (HTML5 source tags, OpenGraph video/audio, JSON-LD and direct links).
 * It deliberately does not sign requests, bypass DRM, bypass login/paywalls,
 * or call private/internal APIs.
 */
object PublicMediaDiscovery {
    data class SiteInfo(
        val id: String,
        val name: String,
        val publicProfile: Boolean,
        val profileType: String = ""
    )

    data class MediaEntry(
        val url: String,
        val title: String,
        val site: SiteInfo,
        val kind: String,
        val format: String
    )

    private data class Adapter(
        val id: String,
        val name: String,
        val hosts: Set<String>,
        val profilePatterns: List<Regex>
    )

    private val adapters = listOf(
        Adapter("youtube", "YouTube", setOf("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be"), listOf(Regex("/(?:@[^/]+|channel/[^/]+|c/[^/]+|user/[^/]+)(?:/.*)?$", RegexOption.IGNORE_CASE))),
        Adapter("facebook", "Facebook", setOf("facebook.com", "www.facebook.com", "m.facebook.com", "fb.watch"), listOf(Regex("/(?:[^/?]+)(?:/videos|/reels)?/?$", RegexOption.IGNORE_CASE))),
        Adapter("instagram", "Instagram", setOf("instagram.com", "www.instagram.com"), listOf(Regex("/(?:@)?[^/]+/?$", RegexOption.IGNORE_CASE))),
        Adapter("tiktok", "TikTok", setOf("tiktok.com", "www.tiktok.com", "m.tiktok.com"), listOf(Regex("/@[^/]+/?$", RegexOption.IGNORE_CASE))),
        Adapter("x", "X", setOf("x.com", "twitter.com", "www.x.com", "www.twitter.com"), listOf(Regex("/@[^/]+/?$", RegexOption.IGNORE_CASE))),
        Adapter("vimeo", "Vimeo", setOf("vimeo.com", "www.vimeo.com", "player.vimeo.com"), listOf(Regex("/(?:user|channels|showcase|ondemand)/[^/?]+", RegexOption.IGNORE_CASE))),
        Adapter("dailymotion", "Dailymotion", setOf("dailymotion.com", "www.dailymotion.com", "geo.dailymotion.com"), listOf(Regex("/(?:user|video|playlist)/[^/?]+", RegexOption.IGNORE_CASE))),
        Adapter("reddit", "Reddit", setOf("reddit.com", "www.reddit.com", "old.reddit.com", "v.redd.it"), listOf(Regex("/r/[^/]+/?$", RegexOption.IGNORE_CASE))),
        Adapter("twitch", "Twitch", setOf("twitch.tv", "www.twitch.tv"), listOf(Regex("/(?:[^/?]+)$", RegexOption.IGNORE_CASE))),
        Adapter("soundcloud", "SoundCloud", setOf("soundcloud.com", "www.soundcloud.com"), listOf(Regex("/[^/]+/[^/?]+$", RegexOption.IGNORE_CASE))),
        Adapter("generic", "Generic public media page", emptySet(), emptyList())
    )

    fun inspectUrl(url: String): SiteInfo {
        val uri = Uri.parse(url)
        val host = uri.host.orEmpty().lowercase(Locale.US)
        val adapter = adapters.firstOrNull { it.hosts.any { h -> host == h || host.endsWith(".$h") } } ?: adapters.last()
        val profile = adapter.profilePatterns.any { it.containsMatchIn(uri.path.orEmpty()) }
        return SiteInfo(adapter.id, adapter.name, profile, if (profile) "Public creator/profile/channel URL" else "")
    }

    fun discover(context: Context, rawUrl: String): Result<List<MediaEntry>> = runCatching {
        val url = rawUrl.trim()
        val site = inspectUrl(url)
        val body = fetch(url)
        val title = titleFromHtml(body).ifBlank { site.name }
        val candidates = linkedSetOf<String>()
        val base = url

        // HTML5 media and common public source attributes.
        val attrPattern = Regex("(?is)(?:src|href|content|data-src|data-video|data-audio)\\s*=\\s*[\\\"']([^\\\"']+?\\.(?:mp4|m4v|webm|mkv|mov|mp3|m4a|aac|opus|ogg|wav|m3u8)(?:\\?[^\\\"']*)?)[\\\"']")
        attrPattern.findAll(body).forEach { candidates += resolve(base, it.groupValues[1]) }

        // OpenGraph media, Twitter player/media and JSON-LD content URLs.
        val metaPattern = Regex("(?is)<meta[^>]+(?:property|name)\\s*=\\s*[\\\"'](?:og:(?:video|audio)(?::url)?|twitter:player:stream)[\\\"'][^>]+content\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']")
        metaPattern.findAll(body).forEach { candidates += resolve(base, it.groupValues[1]) }
        val jsonUrlPattern = Regex("(?i)\\\"(?:contentUrl|embedUrl|content_url|videoUrl|audioUrl)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
        jsonUrlPattern.findAll(body).forEach { candidates += resolve(base, unescape(it.groupValues[1])) }

        // Absolute media URLs that are already public in the document.
        val absolutePattern = Regex("(?i)https?://[^\\\"'\\s<>]+\\.(?:mp4|m4v|webm|mkv|mov|mp3|m4a|aac|opus|ogg|wav|m3u8)(?:\\?[^\\\"'\\s<>]*)?")
        absolutePattern.findAll(body).forEach { candidates += it.value }

        candidates.map { mediaUrl ->
            val clean = mediaUrl.trim().replace("\\u0026", "&")
            val path = Uri.parse(clean).path.orEmpty().lowercase(Locale.US)
            val kind = if (path.endsWith(".mp3") || path.endsWith(".m4a") || path.endsWith(".aac") || path.endsWith(".opus") || path.endsWith(".ogg") || path.endsWith(".wav")) "audio" else "video"
            MediaEntry(clean, title, site, kind, StreamCatalog.inspect(context, clean).getOrNull()?.firstOrNull()?.format ?: "Media")
        }.distinctBy { it.url }
    }

    fun discoverMany(context: Context, rawUrl: String): Result<List<MediaEntry>> = discover(context, rawUrl).map { entries ->
        entries.sortedWith(compareBy<MediaEntry>({ it.kind != "video" }, { it.title.lowercase(Locale.US) }, { it.url }))
    }

    private fun titleFromHtml(html: String): String {
        val og = Regex("(?is)<meta[^>]+(?:property|name)\\s*=\\s*[\\\"'](?:og:title|twitter:title)[\\\"'][^>]+content\\s*=\\s*[\\\"']([^\\\"']+)").find(html)?.groupValues?.get(1)
        if (!og.isNullOrBlank()) return htmlDecode(og).trim()
        val t = Regex("(?is)<title[^>]*>(.*?)</title>").find(html)?.groupValues?.get(1)
        return htmlDecode(t.orEmpty()).replace(Regex("\\s+"), " ").trim()
    }

    private fun htmlDecode(s: String): String = s.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")
    private fun unescape(s: String): String = s.replace("\\/", "/").replace("\\u0026", "&")
    private fun fetch(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 12000; c.readTimeout = 20000; c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) Dixit-Media-Downloader/6.0")
        c.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8")
        return c.inputStream.bufferedReader().use { it.readText() }.also { c.disconnect() }
    }
    private fun resolve(base: String, ref: String): String = runCatching { URL(URL(base), ref).toString() }.getOrDefault(ref)
}
