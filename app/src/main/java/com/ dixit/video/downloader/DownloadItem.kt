package com.dixit.video.downloader

data class DownloadItem(
    val id: Long,
    val url: String,
    val title: String,
    var status: String = "Queued",
    var progress: Int = 0,
    var downloadedBytes: Long = 0L,
    var totalBytes: Long = -1L,
    var filePath: String? = null,
    var attempts: Int = 0,
    var priority: Int = 0,
    var mode: String = "BEST",
    var requestedQuality: String = "Best available",
    var requestedFormat: String = "Auto",
    var requestedFps: String = "Auto",
    var requestedVideoCodec: String = "Auto",
    var requestedAudioCodec: String = "Auto",
    var requestedVideoBitrate: String = "Auto",
    var requestedAudioBitrate: String = "Auto",
    var audioOnly: Boolean = false,
    var videoOnly: Boolean = false,
    var createdAt: Long = System.currentTimeMillis(),
    var completedAt: Long = 0L,
    var category: String = "video"
)
