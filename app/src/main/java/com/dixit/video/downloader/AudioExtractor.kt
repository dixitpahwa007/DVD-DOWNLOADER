package com.dixit.video.downloader

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

/** Extracts an existing AAC audio track to M4A without re-encoding. */
object AudioExtractor {
    fun extractAacToM4a(context: Context, inputPath: String): Result<String> = runCatching {
        val source = File(inputPath)
        require(source.exists()) { "Source file not found" }
        val out = File(source.parentFile ?: context.filesDir, source.nameWithoutExtension + "_audio.m4a")
        val extractor = MediaExtractor()
        extractor.setDataSource(source.absolutePath)
        var audioTrack = -1
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) { audioTrack = i; break }
        }
        require(audioTrack >= 0) { "No audio track found" }
        val format = extractor.getTrackFormat(audioTrack)
        require(format.getString(MediaFormat.KEY_MIME) == "audio/mp4a-latm") { "Only AAC audio can be extracted to M4A without re-encoding" }
        extractor.selectTrack(audioTrack)
        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val outTrack = muxer.addTrack(format)
        muxer.start()
        val buffer = java.nio.ByteBuffer.allocate(1024 * 1024)
        val info = android.media.MediaCodec.BufferInfo()
        while (true) {
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            info.offset = 0; info.size = size; info.presentationTimeUs = extractor.sampleTime
            info.flags = extractor.sampleFlags
            muxer.writeSampleData(outTrack, buffer, info)
            extractor.advance()
        }
        muxer.stop(); muxer.release(); extractor.release()
        out.absolutePath
    }
}
