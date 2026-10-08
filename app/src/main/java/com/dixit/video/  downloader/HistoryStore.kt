package com.dixit.video.downloader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(val id: Long, val title: String, val path: String?, val url: String, val status: String, val time: Long, val category: String)

class HistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("dvd_history", Context.MODE_PRIVATE)
    fun load(): List<HistoryEntry> {
        val a = JSONArray(prefs.getString("items", "[]") ?: "[]")
        return (0 until a.length()).map { i -> val o=a.getJSONObject(i); HistoryEntry(o.getLong("id"),o.optString("title"),o.optString("path",null),o.optString("url"),o.optString("status"),o.optLong("time"),o.optString("category","video")) }
    }
    fun record(item: DownloadItem) {
        val all = load().filterNot { it.id == item.id }.toMutableList()
        all.add(0, HistoryEntry(item.id,item.title,item.filePath,item.url,item.status,System.currentTimeMillis(),item.category))
        val a=JSONArray(); all.take(500).forEach { e -> a.put(JSONObject().apply { put("id",e.id);put("title",e.title);put("path",e.path);put("url",e.url);put("status",e.status);put("time",e.time);put("category",e.category) }) }
        prefs.edit().putString("items",a.toString()).apply()
    }
    fun clear() = prefs.edit().remove("items").apply()
}
