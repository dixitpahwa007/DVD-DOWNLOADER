package com.dixit.video.downloader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class DownloadStore(context: Context) {
    private val prefs = context.getSharedPreferences("downloads", Context.MODE_PRIVATE)

    fun load(): MutableList<DownloadItem> {
        val result = mutableListOf<DownloadItem>()
        val raw = prefs.getString("items", "[]") ?: "[]"
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result += DownloadItem(
                    o.getLong("id"), o.getString("url"), o.getString("title"),
                    o.optString("status", "Queued"), o.optInt("progress", 0),
                    o.optLong("downloadedBytes", 0L), o.optLong("totalBytes", -1L),
                    o.optString("filePath", null), o.optInt("attempts", 0), o.optInt("priority", 0),
                    o.optString("mode", "BEST"), o.optString("requestedQuality", "Best available"),
                    o.optString("requestedFormat", "Auto"), o.optString("requestedFps", "Auto"),
                    o.optString("requestedVideoCodec", "Auto"), o.optString("requestedAudioCodec", "Auto"),
                    o.optString("requestedVideoBitrate", "Auto"), o.optString("requestedAudioBitrate", "Auto"),
                    o.optBoolean("audioOnly", false), o.optBoolean("videoOnly", false),
                    o.optLong("createdAt", System.currentTimeMillis()), o.optLong("completedAt", 0L),
                    o.optString("category", "video")
                )
            }
        }
        return result
    }

    fun remove(id: Long) { save(load().filterNot { it.id == id }) }
    fun update(item: DownloadItem) { save(load().map { if (it.id == item.id) item else it }) }

    fun save(items: List<DownloadItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id); put("url", item.url); put("title", item.title)
                put("status", item.status); put("progress", item.progress)
                put("downloadedBytes", item.downloadedBytes); put("totalBytes", item.totalBytes)
                put("filePath", item.filePath); put("attempts", item.attempts); put("priority", item.priority)
                put("mode", item.mode); put("requestedQuality", item.requestedQuality); put("requestedFormat", item.requestedFormat)
                put("requestedFps", item.requestedFps); put("requestedVideoCodec", item.requestedVideoCodec); put("requestedAudioCodec", item.requestedAudioCodec)
                put("requestedVideoBitrate", item.requestedVideoBitrate); put("requestedAudioBitrate", item.requestedAudioBitrate)
                put("audioOnly", item.audioOnly); put("videoOnly", item.videoOnly); put("createdAt", item.createdAt); put("completedAt", item.completedAt); put("category", item.category)
            })
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }
}
