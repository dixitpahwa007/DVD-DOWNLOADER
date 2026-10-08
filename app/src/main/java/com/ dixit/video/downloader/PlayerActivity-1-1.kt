package com.dixit.video.downloader

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.MediaStore
import android.content.ContentValues
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TrackSelectionDialogBuilder
import com.google.common.util.concurrent.ListenableFuture
import kotlin.math.abs

class PlayerActivity : AppCompatActivity() {
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private lateinit var playerView: PlayerView
    private lateinit var lockButton: Button
    private var requestedUrl = ""
    private var locked = false
    private var speed = 1f
    private var aspect = 0
    private var mediaKey = ""
    private var avSyncMs = 0L
    private var repeatA = C.TIME_UNSET
    private var repeatB = C.TIME_UNSET
    private var brightness = 0.5f
    private var volumeBoostPercent = 100
    private var seekStepMs = 10000L
    private var eq: Equalizer? = null
    private var loudness: LoudnessEnhancer? = null
    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    private val prefs by lazy { getSharedPreferences("playback_positions", MODE_PRIVATE) }
    private lateinit var gestureDetector: GestureDetector
    private val PICK_LYRICS = 502

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_player)
        playerView=findViewById(R.id.playerView); lockButton=findViewById(R.id.lockButton)
        requestedUrl=intent.getStringExtra("url").orEmpty(); mediaKey=requestedUrl.hashCode().toString(); seekStepMs=FeatureSettings.seekSeconds(this)*1000L
        avSyncMs=FeatureSettings.prefs(this).getInt("av_sync_ms",0).toLong()
        findViewById<TextView>(R.id.playerInfo).text="DVD • Media Downloader • Advanced Player"
        findViewById<Button>(R.id.audioButton).setOnClickListener{showTracks(C.TRACK_TYPE_AUDIO)}
        findViewById<Button>(R.id.speedButton).setOnClickListener{chooseSpeed()}
        findViewById<Button>(R.id.aspectButton).setOnClickListener{cycleAspect()}
        findViewById<Button>(R.id.pipButton).setOnClickListener{enterPip()}
        findViewById<Button>(R.id.brightnessButton).setOnClickListener{adjustBrightness()}
        findViewById<Button>(R.id.avSyncButton).setOnClickListener{adjustAvSync()}
        findViewById<Button>(R.id.audioFxButton).setOnClickListener{audioEffects()}
        findViewById<Button>(R.id.lyricsButton).setOnClickListener{pickLyrics()}
        findViewById<Button>(R.id.abButton).setOnClickListener{toggleAB()}
        findViewById<Button>(R.id.frameButton).setOnClickListener{frameStep()}
        findViewById<Button>(R.id.screenshotButton).setOnClickListener{captureFrame()}
        findViewById<Button>(R.id.infoButton).setOnClickListener{showPlayerInfo()}
        findViewById<Button>(R.id.decoderButton).setOnClickListener{chooseDecoder()}
        findViewById<Button>(R.id.seekButton).setOnClickListener{chooseSeekStep()}
        findViewById<Button>(R.id.loopButton).setOnClickListener{cycleLoop()}
        volumeBoostPercent = FeatureSettings.volumeBoost(this)
        lockButton.setOnClickListener{setLocked(!locked)}
        gestureDetector=GestureDetector(this,GestureListener()); playerView.setOnTouchListener{_,e->gestureDetector.onTouchEvent(e)}
        ContextCompat.startForegroundService(this,Intent(this,PlaybackService::class.java).putExtra("url",requestedUrl))
        connectController()
    }
    private fun connectController(){
        val token=SessionToken(this,ComponentName(this,PlaybackService::class.java)); controllerFuture=MediaController.Builder(this,token).buildAsync()
        controllerFuture?.addListener({runCatching{
            controller=controllerFuture?.get(); playerView.player=controller
            val playlist=intent.getStringArrayExtra("playlist")?.map{MediaItem.fromUri(it)}
            if(!playlist.isNullOrEmpty()){controller?.setMediaItems(playlist);controller?.prepare();controller?.play()}
            else {val r=prefs.getLong(mediaKey,0L);if(r>5000L)controller?.seekTo(r)}
            controller?.addListener(object:Player.Listener{override fun onAudioSessionIdChanged(audioSessionId:Int){attachAudioEffects(audioSessionId)}; override fun onPositionDiscontinuity(old:Player.PositionInfo, new:Player.PositionInfo, reason:Int){if(repeatA!=C.TIME_UNSET&&repeatB!=C.TIME_UNSET&&new.positionMs>=repeatB){controller?.seekTo(repeatA)}}})
        }.onFailure{Toast.makeText(this,"Player connection failed",Toast.LENGTH_SHORT).show()}},ContextCompat.getMainExecutor(this))
    }
    private fun showTracks(type:Int){if(type!=C.TRACK_TYPE_AUDIO)return;controller?.let{TrackSelectionDialogBuilder(this,"Audio tracks",it,C.TRACK_TYPE_AUDIO).setShowDisableOption(false).build().show()}}
    private fun adjustBrightness(){brightness=(brightness+.1f).let{if(it>1f).1f else it};window.attributes=window.attributes.apply{screenBrightness=brightness};Toast.makeText(this,"Brightness ${(brightness*100).toInt()}%",Toast.LENGTH_SHORT).show()}
    private fun adjustAvSync(){val o=arrayOf("-500 ms","-250 ms","0 ms","+250 ms","+500 ms");val vals=intArrayOf(-500,-250,0,250,500);val cur=vals.indexOf(avSyncMs.toInt()).coerceAtLeast(0);AlertDialog.Builder(this).setTitle("Audio / video sync").setSingleChoiceItems(o,cur){d,w->avSyncMs=vals[w].toLong();FeatureSettings.prefs(this).edit().putInt("av_sync_ms",vals[w]).apply();Toast.makeText(this,"A/V offset $avSyncMs ms saved for this player session",Toast.LENGTH_SHORT).show();d.dismiss()}.show()}
    private fun chooseSpeed(){val v=floatArrayOf(.25f,.5f,.75f,1f,1.25f,1.5f,1.75f,2f);AlertDialog.Builder(this).setTitle("Playback speed").setSingleChoiceItems(v.map{"${it}x"}.toTypedArray(),v.indexOf(speed).coerceAtLeast(0)){d,w->speed=v[w];controller?.setPlaybackParameters(PlaybackParameters(speed));d.dismiss()}.show()}
    private fun cycleAspect(){aspect=(aspect+1)%4;playerView.resizeMode=when(aspect){0->AspectRatioFrameLayout.RESIZE_MODE_FIT;1->AspectRatioFrameLayout.RESIZE_MODE_ZOOM;2->AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH;else->AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT};Toast.makeText(this,listOf("Fit","Zoom","Width","Height")[aspect],Toast.LENGTH_SHORT).show()}
    private fun enterPip(){if(Build.VERSION.SDK_INT>=26)enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16,9)).build())}
    private fun setLocked(v:Boolean){locked=v;lockButton.text=if(v)"Unlock" else "Lock";playerView.useController=!v;findViewById<View>(R.id.topBar).visibility=if(v)View.GONE else View.VISIBLE}

    private fun toggleAB(){
        val p=controller?.currentPosition?:return
        if(repeatA==C.TIME_UNSET){repeatA=p;Toast.makeText(this,"A point set at ${p/1000}s",Toast.LENGTH_SHORT).show()}
        else if(repeatB==C.TIME_UNSET){if(p<=repeatA){repeatA=C.TIME_UNSET;Toast.makeText(this,"B must be after A",Toast.LENGTH_SHORT).show()}else{repeatB=p;Toast.makeText(this,"A/B repeat active",Toast.LENGTH_SHORT).show()}}
        else {repeatA=C.TIME_UNSET;repeatB=C.TIME_UNSET;Toast.makeText(this,"A/B repeat cleared",Toast.LENGTH_SHORT).show()}
    }
    private fun frameStep(){
        controller?.let { player ->
            player.pause()
            val options=arrayOf("Previous frame", "Next frame", "-5 frames", "+5 frames")
            AlertDialog.Builder(this).setTitle("Frame stepping (~30 fps)").setItems(options){_,which->
                val delta=when(which){0->-33;1->33;2->-167;else->167}
                val duration=player.duration.takeIf{it>0}?:Long.MAX_VALUE
                player.seekTo((player.currentPosition+delta).coerceIn(0,duration))
            }.show()
        }
    }
    private fun chooseDecoder(){val a=arrayOf("Auto","Hardware preferred","Software/FFmpeg preferred");val cur=when(FeatureSettings.decoderMode(this)){"hardware"->1;"software"->2;else->0};AlertDialog.Builder(this).setTitle("Decoder preference").setSingleChoiceItems(a,cur){d,w->val v=arrayOf("auto","hardware","software")[w];FeatureSettings.prefs(this).edit().putInt("decoder",w).apply();Toast.makeText(this,"Decoder preference saved; reopen player to apply",Toast.LENGTH_LONG).show();d.dismiss()}.show()}
    private fun chooseSeekStep(){val a=arrayOf("5 seconds","10 seconds","15 seconds","30 seconds","60 seconds");val vals=longArrayOf(5000,10000,15000,30000,60000);val cur=vals.indexOf(FeatureSettings.seekSeconds(this)*1000L).coerceAtLeast(0);AlertDialog.Builder(this).setTitle("Seek step").setSingleChoiceItems(a,cur){d,w->seekStepMs=vals[w];FeatureSettings.prefs(this).edit().putInt("seek_seconds",w).apply();d.dismiss()}.show()}
    private fun cycleLoop(){val p=FeatureSettings.prefs(this);val n=when(FeatureSettings.loopMode(this)){"off"->"one";"one"->"all";else->"off"};p.edit().putInt("loop", when(n){"one"->1;"all"->2;else->0}).apply();controller?.repeatMode=when(n){"one"->Player.REPEAT_MODE_ONE;"all"->Player.REPEAT_MODE_ALL;else->Player.REPEAT_MODE_OFF};Toast.makeText(this,"Loop: $n",Toast.LENGTH_SHORT).show()}
    private fun captureFrame(){if(playerView.width<=0||playerView.height<=0)return;val bitmap=Bitmap.createBitmap(playerView.width,playerView.height,Bitmap.Config.ARGB_8888);playerView.draw(Canvas(bitmap));val values=ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,"DVD_Frame_${System.currentTimeMillis()}.png");put(MediaStore.Images.Media.MIME_TYPE,"image/png");if(Build.VERSION.SDK_INT>=29)put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Media Downloader")}
        runCatching{val uri=contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)?:error("Storage unavailable");contentResolver.openOutputStream(uri).use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it!!)};Toast.makeText(this,"Frame saved to Pictures/Media Downloader",Toast.LENGTH_LONG).show()}.onFailure{Toast.makeText(this,"Screenshot failed: ${it.message}",Toast.LENGTH_LONG).show()}}
    private fun showPlayerInfo(){val c=controller?:return;AlertDialog.Builder(this).setTitle("Playback information").setMessage("Position: ${c.currentPosition/1000}s\nDuration: ${if(c.duration>0)c.duration/1000 else "unknown"}s\nSpeed: ${speed}x\nA/B: ${if(repeatA!=C.TIME_UNSET&&repeatB!=C.TIME_UNSET)"active" else "off"}\nMedia3 decoder: device-dependent hardware/software selection").setPositiveButton("OK",null).show()}
    private fun pickLyrics(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="text/*";addCategory(Intent.CATEGORY_OPENABLE)},PICK_LYRICS)}
    private fun showLyrics(uri:Uri){runCatching{contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}}.onSuccess{txt->
        val clean=txt.orEmpty().lines().filter{it.isNotBlank()}.joinToString("\n")
        AlertDialog.Builder(this).setTitle("Lyrics").setMessage(clean.ifBlank{"No lyric text found"}).setPositiveButton("Close",null).show()
    }.onFailure{Toast.makeText(this,"Unable to read lyrics",Toast.LENGTH_SHORT).show()}}
    override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(r==PICK_LYRICS&&c==RESULT_OK&&d?.data!=null){showLyrics(d.data!!)}}
    private fun attachAudioEffects(session:Int){runCatching{eq?.release();loudness?.release();eq=Equalizer(0,session).apply{enabled=true};loudness=LoudnessEnhancer(session).apply{setTargetGain(((volumeBoostPercent-100)*15).coerceAtLeast(0));enabled=true}}}
    private fun audioEffects(){
        val equalizer=eq
        val boost=loudness
        if(equalizer==null){Toast.makeText(this,"Audio effects are not available for this playback session",Toast.LENGTH_SHORT).show();return}
        runCatching{
            val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,8,24,8)}
            root.addView(TextView(this).apply{text="5-band equalizer (dB)";textSize=16f})
            val range=equalizer.bandLevelRange
            val minLevel=range[0].toInt(); val maxLevel=range[1].toInt()
            val bands=equalizer.numberOfBands.toInt().coerceAtMost(5)
            for(i in 0 until bands){
                val freq=equalizer.getCenterFreq(i.toShort())/1000
                root.addView(TextView(this).apply{text="${freq} Hz"})
                val bar=SeekBar(this).apply{max=maxLevel-minLevel;progress=(equalizer.getBandLevel(i.toShort())-minLevel).coerceIn(0,maxLevel-minLevel)}
                bar.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(v:SeekBar?,p:Int,fromUser:Boolean){if(fromUser)equalizer.setBandLevel(i.toShort(),(minLevel+p).toShort())}
                    override fun onStartTrackingTouch(v:SeekBar?){ }
                    override fun onStopTrackingTouch(v:SeekBar?){ }
                });root.addView(bar)
            }
            val gain=SeekBar(this).apply{max=2000;progress=(boost?.targetGain ?: 0).toInt().coerceIn(0, 2000)}
            root.addView(TextView(this).apply{text="Loudness boost"});root.addView(gain)
            gain.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(v:SeekBar?,p:Int,fromUser:Boolean){if(fromUser)boost?.setTargetGain(p)}
                override fun onStartTrackingTouch(v:SeekBar?){ }
                override fun onStopTrackingTouch(v:SeekBar?){ }
            })
            equalizer.enabled=true; boost?.enabled=true
            AlertDialog.Builder(this).setTitle("Audio effects").setView(root).setPositiveButton("Close",null).show()
        }.onFailure{Toast.makeText(this,"Audio effects unavailable on this device",Toast.LENGTH_SHORT).show()}
    }
    private inner class GestureListener:GestureDetector.SimpleOnGestureListener(){override fun onDown(e:MotionEvent)=true;override fun onDoubleTap(e:MotionEvent):Boolean{controller?.let{if(e.x<playerView.width/2)it.seekTo((it.currentPosition-seekStepMs).coerceAtLeast(0))else it.seekTo((it.currentPosition+seekStepMs).coerceAtMost(it.duration.takeIf{d->d>0}?:Long.MAX_VALUE))};return true};override fun onFling(a:MotionEvent?,b:MotionEvent,vx:Float,vy:Float):Boolean{if(a==null||locked)return false;if(abs(vy)>abs(vx)*1.2f){val dir=if(a.y>b.y)1 else -1;if(a.x<playerView.width/2){brightness=(brightness+dir*.08f).coerceIn(.05f,1f);window.attributes=window.attributes.apply{screenBrightness=brightness}}else{val max=audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);val cur=audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,(cur+dir*(max/15).coerceAtLeast(1)).coerceIn(0,max),0)};return true};return false}}
    override fun onUserLeaveHint(){super.onUserLeaveHint();if(Build.VERSION.SDK_INT>=26&&controller?.isPlaying==true)enterPip()}
    override fun onStop(){controller?.let{if(it.currentPosition>5000)prefs.edit().putLong(mediaKey,it.currentPosition).apply()};eq?.release();loudness?.release();playerView.player=null;controllerFuture?.let{MediaController.releaseFuture(it)};controller=null;controllerFuture=null;super.onStop()}
}
