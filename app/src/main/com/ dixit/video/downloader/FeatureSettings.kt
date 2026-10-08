package com.dixit.video.downloader

import android.content.Context

object FeatureSettings {
    private const val P = "dvd_complete_features"
    fun prefs(c: Context) = c.getSharedPreferences(P, Context.MODE_PRIVATE)
    fun decoderMode(c: Context): String = when (prefs(c).getInt("decoder", 0)) { 1 -> "hardware"; 2 -> "software"; else -> "auto" }
    fun seekSeconds(c: Context) = intArrayOf(5,10,15,30,60)[prefs(c).getInt("seek_seconds", 1).coerceIn(0,4)]
    fun volumeBoost(c: Context) = intArrayOf(100,125,150,175,200)[prefs(c).getInt("volume_boost", 0).coerceIn(0,4)]
    fun orientation(c: Context) = prefs(c).getString("orientation", "sensor") ?: "sensor"
    fun loopMode(c: Context): String = when (prefs(c).getInt("loop", 0)) { 1 -> "one"; 2 -> "all"; else -> "off" }
    fun eqPreset(c: Context) = prefs(c).getString("eq", "normal") ?: "normal"
    fun concurrency(c: Context): Int = (prefs(c).getInt("concurrency", 0) + 1).coerceIn(1, 4)
    fun maxRetries(c: Context): Int = (prefs(c).getInt("max_retries", 2) + 1).coerceIn(1, 5)
    fun speedLimitKbps(c: Context): Int = when (prefs(c).getInt("speed_limit_kbps", 0).coerceIn(0,5)) {
        1 -> 256; 2 -> 512; 3 -> 1024; 4 -> 2048; 5 -> 5120; else -> 0
    }
}
