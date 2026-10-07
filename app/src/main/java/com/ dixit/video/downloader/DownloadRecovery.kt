package com.dixit.video.downloader

import android.content.Context
import java.io.File

object DownloadRecovery {
    fun reconcile(context: Context) {
        val store = DownloadStore(context)
        val items = store.load()
        var changed = false
        items.forEach { item ->
            if (item.status == "Downloading" || item.status == "Pausing…") {
                item.status = if (item.downloadedBytes > 0) "Paused" else "Queued"
                changed = true
            }
            item.filePath?.let { path ->
                if (path.startsWith("/")) {
                    val f = File(path)
                    if (item.status == "Completed" && !f.exists()) { item.status = "Failed: file missing"; changed = true }
                }
            }
        }
        if (changed) store.save(items)
    }
}
