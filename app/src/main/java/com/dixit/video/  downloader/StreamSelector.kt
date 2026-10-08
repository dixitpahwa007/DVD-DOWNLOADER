package com.dixit.video.downloader

import java.util.Locale

/**
 * Chooses an actually exposed variant from StreamCatalog. It never fabricates a
 * codec, bitrate, FPS or resolution. If no exact match exists, the closest
 * exposed variant is selected according to the requested smart mode.
 */
object StreamSelector {
    data class Selection(val variant: StreamCatalog.Variant, val reason: String)

    fun choose(variants: List<StreamCatalog.Variant>, profile: DownloadProfile): Selection? {
        if (variants.isEmpty()) return null
        val filtered = variants.filter { v ->
            when {
                profile.audioOnly -> v.kind == "audio"
                profile.videoOnly -> v.kind == "video"
                else -> v.kind == "video"
            }
        }.ifEmpty { variants }

        val scored = filtered.map { it to score(it, profile) }.sortedWith(compareByDescending<Pair<StreamCatalog.Variant, Double>> { it.second }.thenByDescending { it.first.height }.thenByDescending { it.first.bitrate })
        val best = scored.firstOrNull()?.first ?: return null
        return Selection(best, describe(best, profile))
    }

    private fun score(v: StreamCatalog.Variant, p: DownloadProfile): Double {
        var s = 0.0
        val h = v.height.toDouble()
        val br = v.bitrate.toDouble()
        val mode = p.mode
        val desiredH = qualityHeight(p.quality)
        val desiredFps = p.fps.toIntOrNull()
        val desiredVbr = bitrate(p.videoBitrate)
        val desiredAbr = bitrate(p.audioBitrate)

        if (p.format != "Auto" && p.format.equals(v.format, true)) s += 5000.0
        if (desiredH > 0) s -= kotlin.math.abs(h - desiredH) * 3.0
        else s += h * if (mode == DownloadProfile.DATA_SAVER) -0.2 else 0.5
        if (desiredFps != null && v.fps > 0) s -= kotlin.math.abs(v.fps - desiredFps) * 40.0
        if (p.videoCodec != "Auto") s += codecScore(v.videoCodec, p.videoCodec) * 2000.0
        if (p.audioCodec != "Auto") s += codecScore(v.audioCodec, p.audioCodec) * 2000.0
        if (desiredVbr != null && v.bitrate > 0) s -= kotlin.math.abs(v.bitrate - desiredVbr) / 1000.0
        if (mode == DownloadProfile.COMPATIBILITY) {
            if (v.format.equals("MP4", true)) s += 1000
            if (v.videoCodec.contains("avc", true) || v.videoCodec.contains("h264", true)) s += 700
        }
        if (mode == DownloadProfile.DATA_SAVER) s -= h * 1.2
        if (mode == DownloadProfile.BEST) s += h + br / 1_000_000.0
        if (p.audioOnly && desiredAbr != null && v.bitrate > 0) s -= kotlin.math.abs(v.bitrate - desiredAbr) / 1000.0
        return s
    }

    private fun codecScore(actual: String, requested: String): Int {
        val a = actual.lowercase(Locale.US)
        val r = requested.lowercase(Locale.US)
        return when {
            r.contains("h.264") || r.contains("avc") -> if (a.contains("avc") || a.contains("h264")) 1 else 0
            r.contains("h.265") || r.contains("hevc") -> if (a.contains("hev") || a.contains("h265")) 1 else 0
            r == "vp9" -> if (a.contains("vp9")) 1 else 0
            r == "av1" -> if (a.contains("av01") || a.contains("av1")) 1 else 0
            r == "aac" -> if (a.contains("mp4a") || a.contains("aac")) 1 else 0
            r == "opus" -> if (a.contains("opus")) 1 else 0
            r == "mp3" -> if (a.contains("mp3")) 1 else 0
            else -> 0
        }
    }

    private fun qualityHeight(q: String): Int = when (q.lowercase(Locale.US)) {
        "8k" -> 4320; "4k" -> 2160; "2k" -> 1440; "1080p" -> 1080
        "720p" -> 720; "480p" -> 480; "360p" -> 360; else -> 0
    }

    private fun bitrate(v: String): Long? = Regex("(\\d+(?:\\.\\d+)?)\\s*(kbps|mbps)", RegexOption.IGNORE_CASE)
        .find(v)?.let { m ->
            val n = m.groupValues[1].toDouble()
            if (m.groupValues[2].equals("mbps", true)) (n * 1_000_000).toLong() else (n * 1_000).toLong()
        }

    private fun describe(v: StreamCatalog.Variant, p: DownloadProfile): String =
        "${v.quality} ${v.format} ${v.width}x${v.height} ${if (v.fps > 0) "${v.fps}fps" else ""} ${if (v.bitrate > 0) "${v.bitrate / 1000}kbps" else ""}".trim()
}
