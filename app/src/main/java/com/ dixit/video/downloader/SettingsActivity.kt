package com.dixit.video.downloader

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("dvd_settings", MODE_PRIVATE) }
    private fun <T> save(key:String, value:T) { prefs.edit().apply { when(value){ is Boolean->putBoolean(key,value); is Int->putInt(key,value); is String->putString(key,value) } }.apply() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,24,24,24)}
        root.addView(TextView(this).apply{text="DVD • Media Downloader — Complete Settings";textSize=22f})
        root.addView(TextView(this).apply{text="Downloads";textSize=17f;setPadding(0,24,0,8)})
        fun sw(label:String,key:String,def:Boolean){ val v=Switch(this).apply{text=label;isChecked=prefs.getBoolean(key,def)};v.setOnCheckedChangeListener{_,x->save(key,x)};root.addView(v) }
        sw("Wi-Fi only","wifi_only",false); sw("Automatic retry","auto_retry",true); sw("Scan completed media into library","scan_media",true)
        fun spinner(label:String,key:String,values:Array<String>,index:Int){root.addView(TextView(this).apply{text=label;setPadding(0,16,0,6)});val s=Spinner(this);s.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,values);s.setSelection(prefs.getInt(key,index).coerceIn(0,values.lastIndex));s.setOnItemSelectedListener(object:android.widget.AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:android.widget.AdapterView<*>?){};override fun onItemSelected(p:android.widget.AdapterView<*>?,v:android.view.View?,pos:Int,id:Long){save(key,pos)}});root.addView(s)}
        spinner("Maximum simultaneous downloads","concurrency",arrayOf("1","2","3","4"),0)
        spinner("Maximum automatic retries","max_retries",arrayOf("1","2","3","4","5"),2)
        spinner("Default quality","default_quality",arrayOf("Best available","8K","4K","2K","1080p","720p","480p","360p","Audio only"),0)
        spinner("Preferred format","preferred_format",arrayOf("Auto","MP4","WebM","MKV","M4A","MP3","Opus"),0)
        spinner("Smart download mode","smart_mode",arrayOf("Best available","Best compatibility","Data saver","Audio only","Video only"),0)
        spinner("Preferred FPS","preferred_fps",arrayOf("Auto","24","30","60","120"),0)
        spinner("Video codec","video_codec",arrayOf("Auto","H.264","H.265/HEVC","VP9","AV1"),0)
        spinner("Audio codec","audio_codec",arrayOf("Auto","AAC","Opus","MP3"),0)
        spinner("Video bitrate","video_bitrate",arrayOf("Auto","500 kbps","1 Mbps","2 Mbps","4 Mbps","8 Mbps","12 Mbps"),0)
        spinner("Audio bitrate","audio_bitrate",arrayOf("Auto","64 kbps","96 kbps","128 kbps","192 kbps","256 kbps","320 kbps"),0)
        spinner("Duplicate files","duplicate_policy",arrayOf("Rename automatically","Skip if already exists","Replace existing"),0)
        spinner("Download speed limit","speed_limit_kbps",arrayOf("Unlimited","256 KB/s","512 KB/s","1 MB/s","2 MB/s","5 MB/s"),0)
        root.addView(TextView(this).apply{text="Player & Library";textSize=17f;setPadding(0,24,0,8)})
        sw("Remember playback position","remember_position",true); sw("Auto picture-in-picture","auto_pip",true); sw("Show hidden/system media","show_hidden",false)
        spinner("Decoder preference","decoder",arrayOf("Auto","Hardware preferred","Software / FFmpeg preferred"),0)
        spinner("Seek step","seek_seconds",arrayOf("5 seconds","10 seconds","15 seconds","30 seconds","60 seconds"),1)
        spinner("Loop mode","loop",arrayOf("Off","Repeat one","Repeat all"),0)
        spinner("Volume boost","volume_boost",arrayOf("100%","125%","150%","175%","200%"),0)
        root.addView(TextView(this).apply{text="Privacy: no login, advertising, subscription or API/token requirement. DRM, authentication and protected-stream bypass is not supported.";setPadding(0,24,0,0)})
        setContentView(ScrollView(this).apply{addView(root)})
    }
}
