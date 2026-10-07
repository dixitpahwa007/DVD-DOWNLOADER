package com.dixit.video.downloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.documentfile.provider.DocumentFile
import java.io.FileInputStream
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Transformer
import java.util.concurrent.CountDownLatch
import java.io.BufferedInputStream
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class DownloadService : Service() {
    companion object {
        const val ACTION_ADD = "dvd.ADD"
        const val ACTION_PAUSE = "dvd.PAUSE"
        const val ACTION_RESUME = "dvd.RESUME"
        const val ACTION_CANCEL = "dvd.CANCEL"
        const val ACTION_RETRY = "dvd.RETRY"
        const val EXTRA_ID = "id"
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_MODE = "mode"
        const val EXTRA_QUALITY = "quality"
        const val EXTRA_FORMAT = "format"
        const val EXTRA_FPS = "fps"
        const val EXTRA_VCODEC = "vcodec"
        const val EXTRA_ACODEC = "acodec"
        const val EXTRA_VBITRATE = "vbitrate"
        const val EXTRA_ABITRATE = "abitrate"
        const val EXTRA_AUDIO_ONLY = "audioOnly"
        const val EXTRA_VIDEO_ONLY = "videoOnly"
        private const val CHANNEL = "downloads"
        private const val NOTIFICATION = 4101

        fun command(context: Context, action: String, id: Long = -1L, url: String? = null, title: String? = null, profile: DownloadProfile = DownloadProfile()) {
            val i = Intent(context, DownloadService::class.java).setAction(action).putExtra(EXTRA_ID, id)
            if (url != null) i.putExtra(EXTRA_URL, url)
            if (title != null) i.putExtra(EXTRA_TITLE, title)
            i.putExtra(EXTRA_MODE, profile.mode).putExtra(EXTRA_QUALITY, profile.quality).putExtra(EXTRA_FORMAT, profile.format)
                .putExtra(EXTRA_FPS, profile.fps).putExtra(EXTRA_VCODEC, profile.videoCodec).putExtra(EXTRA_ACODEC, profile.audioCodec)
                .putExtra(EXTRA_VBITRATE, profile.videoBitrate).putExtra(EXTRA_ABITRATE, profile.audioBitrate)
                .putExtra(EXTRA_AUDIO_ONLY, profile.audioOnly).putExtra(EXTRA_VIDEO_ONLY, profile.videoOnly)
            ContextCompat.startForegroundService(context, i)
        }
    }

    private val executor = Executors.newFixedThreadPool(4)
    private val stopRequested = java.util.concurrent.ConcurrentHashMap<Long, AtomicBoolean>()
    private val pauseRequested = java.util.concurrent.ConcurrentHashMap<Long, AtomicBoolean>()
    private val store by lazy { DownloadStore(this) }
    private val items by lazy { store.load() }
    private var currentId = -1L
    private val activeIds = java.util.Collections.synchronizedSet(mutableSetOf<Long>())
    private val adaptiveTransformers = java.util.concurrent.ConcurrentHashMap<Long, Transformer>()
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        DownloadRecovery.reconcile(this)
        createChannel()
        startForeground(NOTIFICATION, notification("Ready", 0, false))
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MediaDownloader:download")
        items.filter { it.status == "Downloading" || it.status == "Pausing…" }.forEach { it.status = "Queued" }
        store.save(items)
        processQueue()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ADD -> add(
                intent.getStringExtra(EXTRA_URL).orEmpty(), intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                DownloadProfile(
                    intent.getStringExtra(EXTRA_MODE) ?: "BEST", intent.getStringExtra(EXTRA_QUALITY) ?: "Best available", intent.getStringExtra(EXTRA_FORMAT) ?: "Auto",
                    intent.getStringExtra(EXTRA_FPS) ?: "Auto", intent.getStringExtra(EXTRA_VCODEC) ?: "Auto", intent.getStringExtra(EXTRA_ACODEC) ?: "Auto",
                    intent.getStringExtra(EXTRA_VBITRATE) ?: "Auto", intent.getStringExtra(EXTRA_ABITRATE) ?: "Auto",
                    intent.getBooleanExtra(EXTRA_AUDIO_ONLY, false), intent.getBooleanExtra(EXTRA_VIDEO_ONLY, false)))
            ACTION_PAUSE -> pause(intent.getLongExtra(EXTRA_ID, -1L))
            ACTION_RESUME -> resume(intent.getLongExtra(EXTRA_ID, -1L))
            ACTION_CANCEL -> cancel(intent.getLongExtra(EXTRA_ID, -1L))
            ACTION_RETRY -> retry(intent.getLongExtra(EXTRA_ID, -1L))
        }
        return START_STICKY
    }

    private fun add(url: String, title: String, profile: DownloadProfile) {
        if (url.isBlank()) return
        val id = System.currentTimeMillis()
        val safeTitle = title.ifBlank { "download_$id" }
        val category = if (profile.audioOnly || profile.format in listOf("MP3", "M4A", "Opus", "AAC")) "audio" else "video"
        items.add(0, DownloadItem(id, url, safeTitle, mode = profile.mode, requestedQuality = profile.quality, requestedFormat = profile.format,
            requestedFps = profile.fps, requestedVideoCodec = profile.videoCodec, requestedAudioCodec = profile.audioCodec,
            requestedVideoBitrate = profile.videoBitrate, requestedAudioBitrate = profile.audioBitrate,
            audioOnly = profile.audioOnly, videoOnly = profile.videoOnly, category = category))
        store.save(items)
        updateNotification()
        processQueue()
    }

    private fun pause(id: Long) {
        items.find { it.id == id }?.let { if (it.status == "Downloading") { pauseRequested.computeIfAbsent(it.id) { AtomicBoolean(false) }.set(true); it.status = "Pausing…"; store.save(items) } }
    }

    private fun resume(id: Long) {
        items.find { it.id == id }?.let { if (it.status == "Paused" || it.status == "Failed") { it.status = "Queued"; store.save(items); processQueue() } }
    }

    private fun retry(id: Long) = resume(id)

    private fun cancel(id: Long) {
        items.find { it.id == id }?.let {
            stopRequested.computeIfAbsent(it.id) { AtomicBoolean(false) }.set(true)
            adaptiveTransformers[it.id]?.cancel()
            it.status = "Cancelled"
            store.save(items)
        }
    }

    private fun processQueue() {
        val limit = FeatureSettings.concurrency(this)
        val pending = items.filter { it.status == "Queued" && !activeIds.contains(it.id) }.sortedByDescending { it.priority }
        pending.take((limit - activeIds.size).coerceAtLeast(0)).forEach { next ->
            activeIds.add(next.id)
            if (currentId == -1L) currentId = next.id
            executor.execute {
                try { download(next) } finally { activeIds.remove(next.id); if (currentId == next.id) currentId = activeIds.firstOrNull() ?: -1L; processQueue() }
            }
        }
        if (pending.isEmpty() && activeIds.isEmpty()) updateNotification()
    }

    private fun download(item: DownloadItem) {
        stopRequested.computeIfAbsent(item.id) { AtomicBoolean(false) }.set(false); pauseRequested.computeIfAbsent(item.id) { AtomicBoolean(false) }.set(false)
        wakeLock?.takeIf { !it.isHeld }?.acquire(30 * 60 * 1000L)
        item.status = "Downloading"; item.attempts += 1; store.save(items); updateNotification()
        var connection: HttpURLConnection? = null
        try {
            if (getSharedPreferences("dvd_settings", MODE_PRIVATE).getBoolean("wifi_only", false)) {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                val caps = cm.getNetworkCapabilities(cm.activeNetwork)
                if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) != true) throw Exception("Wi-Fi only is enabled")
            }
            // Resolve the requested profile against streams actually exposed by the source.
            // For HLS/video variants this can change the effective URL; for ordinary files it remains unchanged.
            var sourceUrl = item.url
            runCatching {
                val variants = StreamCatalog.inspect(this, item.url).getOrThrow()
                StreamSelector.choose(variants, DownloadProfile(item.mode,item.requestedQuality,item.requestedFormat,item.requestedFps,item.requestedVideoCodec,item.requestedAudioCodec,item.requestedVideoBitrate,item.requestedAudioBitrate,item.audioOnly,item.videoOnly))?.let { sel ->
                    // Keep the HLS master for normal video playback so its declared audio group remains available.
                    if (!item.url.substringBefore('?').lowercase().endsWith(".m3u8")) sourceUrl = sel.variant.url
                    else if (item.audioOnly || (sel.variant.kind=="video" && sel.variant.audioGroup.isBlank())) sourceUrl = sel.variant.url
                }
            }
            val targetDir = MediaOrganizer.directory(this, item.category)
            targetDir.mkdirs()
            val extension = if (sourceUrl.substringBefore('?').lowercase().endsWith(".m3u8")) "mp4" else when (item.requestedFormat.lowercase()) {
                "mp4" -> "mp4"; "webm" -> "webm"; "mkv" -> "mkv"; "m4a" -> "m4a"; "mp3" -> "mp3"; "opus" -> "opus"
                else -> sanitize(item.title).substringAfterLast('.', "mp4")
            }
            val baseName = sanitize(item.title).substringBeforeLast('.', sanitize(item.title))
            val target = File(targetDir, MediaOrganizer.safeName(baseName, extension))
            val duplicatePolicy = getSharedPreferences("dvd_settings", MODE_PRIVATE).getString("duplicate_policy", "rename") ?: "rename"
            val resolvedTarget = if (target.exists() && duplicatePolicy == "skip") { item.status = "Completed"; item.progress = 100; store.save(items); return } else if (target.exists() && duplicatePolicy == "rename") uniqueTarget(target) else target
            val temp = File(resolvedTarget.absolutePath + ".part")
            val existing = if (temp.exists()) temp.length() else 0L
            if (sourceUrl.substringBefore('?').lowercase().endsWith(".m3u8")) {
                val out = resolvedTarget.absolutePath
                val latch = CountDownLatch(1)
                var failure: Throwable? = null
                val transformer = Transformer.Builder(this)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult) { item.progress = 100; latch.countDown() }
                        override fun onError(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult, exportException: androidx.media3.transformer.ExportException) { failure = exportException; latch.countDown() }
                    }).build()
                adaptiveTransformers[item.id] = transformer
                item.totalBytes = -1L; item.progress = 5; store.save(items); updateNotification()
                transformer.start(MediaItem.fromUri(item.url), out)
                while (!latch.await(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    if (stopRequested[item.id]?.get() == true) { transformer.cancel(); throw CancelledException() }
                    if (pauseRequested[item.id]?.get() == true) { transformer.cancel(); throw PausedException() }
                    item.progress = (item.progress + 1).coerceAtMost(95); store.save(items); updateNotification()
                }
                adaptiveTransformers.remove(item.id)
                failure?.let { throw it }
                item.filePath = out; item.status = "Completed"; item.progress = 100; item.completedAt = System.currentTimeMillis(); store.save(items); HistoryStore(this).record(item)
                return
            }
            connection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000; readTimeout = 30000; instanceFollowRedirects = true
                if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
                connect()
            }
            val response = connection.responseCode
            if (response !in 200..299 && response != 206) error("HTTP $response")
            val contentLength = connection.contentLengthLong
            item.totalBytes = if (contentLength > 0) existing + contentLength else -1L
            item.downloadedBytes = existing
            val append = response == 206 && existing > 0
            if (!append && existing > 0) temp.delete()
            BufferedInputStream(connection.inputStream).use { input ->
                RandomAccessFile(temp, "rw").use { out ->
                    if (append) out.seek(existing) else out.setLength(0)
                    val buffer = ByteArray(64 * 1024)
                    val downloadStartedAt = System.nanoTime()
                    while (true) {
                        if (stopRequested[item.id]?.get() == true) throw CancelledException()
                        if (pauseRequested[item.id]?.get() == true) throw PausedException()
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        item.downloadedBytes += n
                        throttleDownload(item.downloadedBytes, downloadStartedAt)
                        item.progress = if (item.totalBytes > 0) ((item.downloadedBytes * 100) / item.totalBytes).toInt().coerceIn(0, 100) else 0
                        if (item.downloadedBytes % (512 * 1024) < n) { store.save(items); updateNotification() }
                    }
                }
            }
            if (resolvedTarget.exists()) resolvedTarget.delete()
            if (!temp.renameTo(resolvedTarget)) error("Could not finalize file")
            // A requested output format/bitrate must trigger a real conversion, not a filename rename.
            val requested = item.requestedFormat.uppercase()
            val actualExt = resolvedTarget.extension.lowercase()
            val needsMp3 = requested == "MP3" && actualExt != "mp3"
            val needsM4a = requested == "M4A" && actualExt != "m4a"
            val needsMp4 = requested == "MP4" && actualExt != "mp4"
            val needsVideoBitrate = requested == "MP4" && item.requestedVideoBitrate != "Auto"
            val needsAudioBitrate = requested in listOf("MP4","M4A") && item.requestedAudioBitrate != "Auto"
            if (needsMp3 || needsM4a || needsMp4 || needsVideoBitrate || needsAudioBitrate) {
                val sourceForConversion = File(resolvedTarget.absolutePath + ".source")
                if (!resolvedTarget.renameTo(sourceForConversion)) error("Unable to prepare conversion source")
                val outputFile = resolvedTarget
                item.status = "Converting"; item.progress = 0; item.filePath = null; store.save(items); updateNotification()
                val outFormat = when { needsMp3 -> "MP3"; needsM4a -> "M4A"; else -> "MP4" }
                ConversionService.start(this, sourceForConversion.absolutePath, outputFile.absolutePath, outFormat, 0, item.title,
                    item.requestedAudioBitrate, item.requestedVideoBitrate, item.id, true)
                return
            }
            var finalPath = resolvedTarget.absolutePath
            val tree = getSharedPreferences("folder", MODE_PRIVATE).getString("download_tree", null)
            if (!tree.isNullOrBlank()) {
                runCatching {
                    val root = DocumentFile.fromTreeUri(this, android.net.Uri.parse(tree))
                    val mime = guessMime(resolvedTarget.name)
                    val outFile = root?.createFile(mime, resolvedTarget.name)
                    if (outFile != null) {
                        FileInputStream(resolvedTarget).use { input -> contentResolver.openOutputStream(outFile.uri, "wt")!!.use { output -> input.copyTo(output, 64 * 1024) } }
                        resolvedTarget.delete(); finalPath = outFile.uri.toString()
                    }
                }
            }
            item.filePath = finalPath; item.status = "Completed"; item.progress = 100; item.completedAt = System.currentTimeMillis(); store.save(items); HistoryStore(this).record(item)
        } catch (_: PausedException) {
            item.status = "Paused"; store.save(items); HistoryStore(this).record(item)
        } catch (_: CancelledException) {
            item.status = "Cancelled"; store.save(items); HistoryStore(this).record(item)
        } catch (t: Throwable) {
            val autoRetry = getSharedPreferences("dvd_settings", MODE_PRIVATE).getBoolean("auto_retry", true)
            if (autoRetry && item.attempts <= FeatureSettings.maxRetries(this) && stopRequested[item.id]?.get() != true) item.status = "Queued" else item.status = "Failed: ${t.message ?: "download error"}"
            store.save(items); if (item.status.startsWith("Failed")) HistoryStore(this).record(item)
        } finally {
            connection?.disconnect(); wakeLock?.let { if (it.isHeld && activeIds.size <= 1) it.release() }
            stopRequested.remove(item.id); pauseRequested.remove(item.id)
            if (currentId == item.id) currentId = activeIds.firstOrNull { it != item.id } ?: -1L
            updateNotification(); processQueue()
        }
    }

    private fun throttleDownload(bytes: Long, startedAtNanos: Long) {
        val kbps = FeatureSettings.speedLimitKbps(this)
        if (kbps <= 0) return
        val targetNanos = (bytes.toDouble() / (kbps * 1024.0) * 1_000_000_000.0).toLong()
        val elapsed = System.nanoTime() - startedAtNanos
        if (targetNanos > elapsed) {
            val millis = ((targetNanos - elapsed) / 1_000_000L).coerceAtMost(250L)
            if (millis > 0) try { Thread.sleep(millis) } catch (_: InterruptedException) {}
        }
    }

    private fun uniqueTarget(base: File): File {
        var i = 1
        var f = base
        while (f.exists()) {
            val dot = base.name.lastIndexOf('.')
            val stem = if (dot > 0) base.name.substring(0, dot) else base.name
            val ext = if (dot > 0) base.name.substring(dot) else ""
            f = File(base.parentFile, "${stem} (${i++})$ext")
        }
        return f
    }

    private fun sanitize(name: String): String {
        val cleaned = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return if (cleaned.isBlank()) "download_${System.currentTimeMillis()}" else cleaned.take(180)
    }

    private fun updateNotification() {
        val item = items.firstOrNull { it.id == currentId }
        val n = if (item != null) notification(item.title, item.progress, true) else notification("Download queue idle", 0, false)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION, n)
    }

    private fun notification(title: String, progress: Int, active: Boolean): Notification =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Media Downloader")
            .setContentText(title)
            .setOngoing(active)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, active && progress == 0)
            .apply {
                val current = items.firstOrNull { it.id == currentId }
                if (current != null && active) addAction(NotificationCompat.Action.Builder(android.R.drawable.ic_media_pause, "Pause", actionPending(ACTION_PAUSE, current.id)).build())
                if (current != null && active) addAction(NotificationCompat.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", actionPending(ACTION_CANCEL, current.id)).build())
            }
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .build()

    private fun actionPending(action: String, id: Long): PendingIntent = PendingIntent.getService(this, (id % 100000).toInt() + action.hashCode(), Intent(this, DownloadService::class.java).setAction(action).putExtra(EXTRA_ID, id), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "mp4" -> "video/mp4"; "mkv" -> "video/x-matroska"; "webm" -> "video/webm"; "mov" -> "video/quicktime"; "avi" -> "video/x-msvideo"; "mp3" -> "audio/mpeg"; "m4a" -> "audio/mp4"; "aac" -> "audio/aac"; "flac" -> "audio/flac"; "wav" -> "audio/wav"; else -> "application/octet-stream"
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onDestroy() { stopRequested.values.forEach { it.set(true) }; executor.shutdownNow(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    private class PausedException : Exception()
    private class CancelledException : Exception()
}
