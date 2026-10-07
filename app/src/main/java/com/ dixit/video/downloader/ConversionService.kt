package com.dixit.video.downloader

import android.app.*
import android.content.*
import android.media.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.Transformer
import io.auxo.ame.Mp3Encoder
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ConversionService : Service() {
    companion object {
        const val ACTION_CONVERT = "dvd.CONVERT"
        const val EXTRA_INPUT = "input"
        const val EXTRA_OUTPUT = "output"
        const val EXTRA_FORMAT = "format"
        const val EXTRA_HEIGHT = "height"
        const val EXTRA_TITLE = "title"
        const val EXTRA_AUDIO_BITRATE = "audio_bitrate"
        const val EXTRA_VIDEO_BITRATE = "video_bitrate"
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_DELETE_INPUT = "delete_input"
        private const val CHANNEL = "conversion"
        private const val NOTE = 4201
        fun start(context: Context, input: String, output: String, format: String, height: Int = 0, title: String = "Conversion", audioBitrate: String = "128 kbps", videoBitrate: String = "Auto", itemId: Long = -1L, deleteInput: Boolean = false) {
            val i=Intent(context,ConversionService::class.java).setAction(ACTION_CONVERT)
                .putExtra(EXTRA_INPUT,input).putExtra(EXTRA_OUTPUT,output).putExtra(EXTRA_FORMAT,format)
                .putExtra(EXTRA_HEIGHT,height).putExtra(EXTRA_TITLE,title).putExtra(EXTRA_AUDIO_BITRATE,audioBitrate)
                .putExtra(EXTRA_VIDEO_BITRATE,videoBitrate).putExtra(EXTRA_ITEM_ID,itemId).putExtra(EXTRA_DELETE_INPUT,deleteInput)
            androidx.core.content.ContextCompat.startForegroundService(context,i)
        }
    }
    override fun onCreate(){super.onCreate();createChannel();startForeground(NOTE,note("Preparing conversion",0))}
    override fun onStartCommand(i:Intent?,flags:Int,startId:Int):Int{
        if(i?.action==ACTION_CONVERT){
            val input=i.getStringExtra(EXTRA_INPUT) ?: return START_NOT_STICKY
            val output=i.getStringExtra(EXTRA_OUTPUT) ?: return START_NOT_STICKY
            val format=i.getStringExtra(EXTRA_FORMAT) ?: "MP4"
            val height=i.getIntExtra(EXTRA_HEIGHT,0)
            val bitrate=i.getStringExtra(EXTRA_AUDIO_BITRATE) ?: "128 kbps"
            val videoBitrate=i.getStringExtra(EXTRA_VIDEO_BITRATE) ?: "Auto"
            val itemId=i.getLongExtra(EXTRA_ITEM_ID,-1L)
            val deleteInput=i.getBooleanExtra(EXTRA_DELETE_INPUT,false)
            Thread { runConversion(input,output,format,height,bitrate,videoBitrate,itemId,deleteInput) }.start()
        }
        return START_NOT_STICKY
    }
    private fun runConversion(input:String, output:String, format:String, height:Int, bitrate:String, videoBitrate:String, itemId:Long, deleteInput:Boolean){
        val file=File(output); file.parentFile?.mkdirs(); if(file.exists()) file.delete()
        if(format.equals("MP3",true)) { runMp3Conversion(input,file,bitrate); return }
        Handler(Looper.getMainLooper()).post {
            try {
                val builder=Transformer.Builder(this).addListener(object:Transformer.Listener{
                    override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult){
                        finishItem(itemId,file.absolutePath,true,deleteInput,input)
                        postStatus("Conversion complete: ${file.name}",false);stopSelf()
                    }
                    override fun onError(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult, exportException: androidx.media3.transformer.ExportException){
                        finishItem(itemId,null,false,deleteInput,input)
                        postStatus("Conversion failed: ${exportException.message ?: "unsupported media"}",false);stopSelf()
                    }
                })
                when(format.uppercase()){
                    "MP4" -> builder.setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC)
                    "M4A","AAC" -> builder.setAudioMimeType(MimeTypes.AUDIO_AAC)
                    else -> builder.setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC)
                }
                val encoderBuilder = androidx.media3.transformer.DefaultEncoderFactory.Builder(this)
                parseBitrate(bitrate)?.let { encoderBuilder.setRequestedAudioEncoderSettings(androidx.media3.transformer.AudioEncoderSettings.Builder().setBitrate(it).build()) }
                parseBitrate(videoBitrate)?.let { encoderBuilder.setRequestedVideoEncoderSettings(androidx.media3.transformer.VideoEncoderSettings.Builder().setBitrate(it).build()) }
                val transformer=builder.setEncoderFactory(encoderBuilder.build()).build()
                val effects=if(height>0) Effects(emptyList(), listOf(Presentation.createForHeight(height))) else Effects(emptyList(), emptyList())
                val edited=EditedMediaItem.Builder(MediaItem.fromUri(input)).setEffects(effects).build()
                transformer.start(edited,file.absolutePath)
                postStatus("Conversion started: ${file.name}",true)
            } catch(t:Throwable){finishItem(itemId,null,false,deleteInput,input);postStatus("Conversion failed: ${t.message ?: "unsupported format"}",false);stopSelf()}
        }
    }

    /** True MP3 conversion: decode the source audio to PCM WAV, then encode that PCM with AME/LAME. */
    private var currentItemId:Long=-1L
    private var currentDeleteInput=false
    private var currentInput=""
    private var decodedSampleRate=44100
    private var decodedChannels=2
    private fun runMp3Conversion(input:String, output:File, bitrate:String){
        try {
            currentItemId = currentItemId.takeIf { it >= 0 } ?: -1L
            postStatus("Decoding audio for MP3…",true)
            val wav=File(cacheDir,"dvd_mp3_${System.currentTimeMillis()}.wav")
            decodeAudioToWav(input,wav)
            postStatus("Encoding MP3…",true)
            val kbps=Regex("(\\d+)").find(bitrate)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(32,320) ?: 128
            val options=Mp3Encoder.Options().sampleRate(decodedSampleRate).bitrate(kbps).numChannels(decodedChannels).quality(3).mode(if(decodedChannels==1) Mp3Encoder.Options.MONO else Mp3Encoder.Options.STEREO)
            Mp3Encoder.encode(wav.absolutePath,output.absolutePath,options,object:Mp3Encoder.Callback{
                override fun onStart(){postStatus("MP3 encoding started",true)}
                override fun onProgress(total:Int,current:Int){ if(total>0) postStatus("MP3 encoding ${((current.toLong()*100)/total).coerceIn(0,100)}%",true) }
                override fun onComplete(){wav.delete();finishItem(currentItemId,output.absolutePath,true,currentDeleteInput,currentInput);postStatus("MP3 conversion complete: ${output.name}",false);stopSelf()}
                override fun onError(){wav.delete();finishItem(currentItemId,null,false,currentDeleteInput,currentInput);postStatus("MP3 conversion failed",false);stopSelf()}
            })
        } catch(t:Throwable){finishItem(currentItemId,null,false,currentDeleteInput,currentInput);postStatus("MP3 conversion failed: ${t.message ?: "unsupported audio"}",false);stopSelf()}
    }

    private fun decodeAudioToWav(input:String, wav:File){
        val extractor=MediaExtractor(); extractor.setDataSource(input)
        var track=-1
        for(i in 0 until extractor.trackCount){if(extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/")==true){track=i;break}}
        require(track>=0){"No audio track found"}
        val fmt=extractor.getTrackFormat(track); extractor.selectTrack(track)
        val mime=fmt.getString(MediaFormat.KEY_MIME) ?: error("Unknown audio codec")
        val sampleRate=fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE,44100); val channels=fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT,2).coerceIn(1,2)
        decodedSampleRate=sampleRate; decodedChannels=channels
        val decoder=MediaCodec.createDecoderByType(mime); decoder.configure(fmt,null,null,0); decoder.start()
        FileOutputStream(wav).use { out -> writeWavHeader(out,sampleRate,channels,0)
            val info=MediaCodec.BufferInfo(); var inputDone=false; var outputDone=false; var total=0L
            while(!outputDone){
                if(!inputDone){val idx=decoder.dequeueInputBuffer(10000);if(idx>=0){val b=decoder.getInputBuffer(idx)!!;b.clear();val n=extractor.readSampleData(b,0);if(n<0){decoder.queueInputBuffer(idx,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputDone=true}else{decoder.queueInputBuffer(idx,0,n,extractor.sampleTime,0);extractor.advance()}}}
                val outIdx=decoder.dequeueOutputBuffer(info,10000)
                if(outIdx>=0){val b=decoder.getOutputBuffer(outIdx)!!;if(info.size>0){b.position(info.offset);b.limit(info.offset+info.size);val data=ByteArray(info.size);b.get(data);out.write(data);total+=data.size};decoder.releaseOutputBuffer(outIdx,false);if((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0)outputDone=true}
            }
            out.flush(); RandomAccessFile(wav,"rw").use { raf->raf.seek(0);writeWavHeader(raf,sampleRate,channels,total) }
        }
        decoder.stop();decoder.release();extractor.release()
    }
    private fun writeWavHeader(out:OutputStream,sampleRate:Int,channels:Int,dataSize:Long){val h=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);h.put("RIFF".toByteArray());h.putInt((36+dataSize).coerceAtMost(0xffffffffL).toInt());h.put("WAVEfmt ".toByteArray());h.putInt(16);h.putShort(1);h.putShort(channels.toShort());h.putInt(sampleRate);h.putInt(sampleRate*channels*2);h.putShort((channels*2).toShort());h.putShort(16);h.put("data".toByteArray());h.putInt(dataSize.coerceAtMost(0xffffffffL).toInt());out.write(h.array())}
    private fun writeWavHeader(raf:RandomAccessFile,sampleRate:Int,channels:Int,dataSize:Long){writeWavHeader(object:OutputStream(){override fun write(b:Int){raf.write(b)}override fun write(b:ByteArray){raf.write(b)}override fun write(b:ByteArray,o:Int,l:Int){raf.write(b,o,l)}},sampleRate,channels,dataSize)}
    private fun parseBitrate(value:String):Int? = Regex("(\\d+(?:\\.\\d+)?)\\s*(kbps|mbps)",RegexOption.IGNORE_CASE).find(value)?.let{m->
        val n=m.groupValues[1].toDouble();if(m.groupValues[2].equals("mbps",true))(n*1_000_000).toInt() else (n*1_000).toInt()
    }
    private fun finishItem(itemId:Long,output:String?,success:Boolean,deleteInput:Boolean,input:String){
        if(deleteInput) runCatching{File(input).delete()}
        if(itemId<0) return
        runCatching{
            val store=DownloadStore(this);val item=store.load().firstOrNull{it.id==itemId}?:return@runCatching
            if(success&&output!=null){item.filePath=output;item.status="Completed";item.progress=100;item.completedAt=System.currentTimeMillis()}
            else item.status="Failed: conversion failed"
            store.update(item);if(success)HistoryStore(this).record(item)
        }
    }

    private fun postStatus(message:String,ongoing:Boolean){getSystemService(NotificationManager::class.java).notify(NOTE,note(message,0,ongoing))}
    private fun note(text:String,p:Int,ongoing:Boolean=true)=NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle("Media conversion").setContentText(text).setProgress(100,p,p==0).setOngoing(ongoing).setContentIntent(PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)).build()
    private fun createChannel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Conversions",NotificationManager.IMPORTANCE_LOW))}
    override fun onBind(intent:Intent?):IBinder?=null
}
