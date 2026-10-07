package com.dixit.video.downloader

data class DownloadProfile(
    val mode: String = "BEST",
    val quality: String = "Best available",
    val format: String = "Auto",
    val fps: String = "Auto",
    val videoCodec: String = "Auto",
    val audioCodec: String = "Auto",
    val videoBitrate: String = "Auto",
    val audioBitrate: String = "Auto",
    val audioOnly: Boolean = false,
    val videoOnly: Boolean = false
) {
    companion object {
        const val BEST = "BEST"
        const val COMPATIBILITY = "COMPATIBILITY"
        const val DATA_SAVER = "DATA_SAVER"
        const val AUDIO = "AUDIO"
        const val VIDEO = "VIDEO"
    }
}
