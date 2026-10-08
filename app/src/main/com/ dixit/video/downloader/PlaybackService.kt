package com.dixit.video.downloader

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val mode = FeatureSettings.decoderMode(this)
        val rf = DefaultRenderersFactory(this).setEnableDecoderFallback(true).apply {
            when (mode) {
                "software" -> setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                "hardware" -> setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
                else -> setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            }
        }
        player = ExoPlayer.Builder(this, rf).build()
        session = MediaSession.Builder(this, player!!).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("url")?.takeIf { it.isNotBlank() }?.let {
            player?.setMediaItem(MediaItem.fromUri(it)); player?.prepare(); player?.playWhenReady = true
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onTaskRemoved(rootIntent: Intent?) { player?.playWhenReady = true; super.onTaskRemoved(rootIntent) }
    override fun onDestroy() { session?.release(); player?.release(); session = null; player = null; super.onDestroy() }
}
