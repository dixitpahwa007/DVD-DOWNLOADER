package com.dixit.video.downloader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.URLUtil
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MainActivity : AppCompatActivity(), DownloadAdapter.Actions {
    private val items=mutableListOf<DownloadItem>(); private lateinit var adapter:DownloadAdapter; private lateinit var input:EditText; private val store by lazy{DownloadStore(this)}
    companion object{private const val PICK_MEDIA=210;private const val PICK_FOLDER=211}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContentView(R.layout.activity_main);if(android.os.Build.VERSION.SDK_INT>=33&&ActivityCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.POST_NOTIFICATIONS),100)
        input=findViewById(R.id.urlInput);items.addAll(store.load());val list=findViewById<RecyclerView>(R.id.downloadList);list.layoutManager=LinearLayoutManager(this);adapter=DownloadAdapter(items,this);list.adapter=adapter
        handleIncomingIntent(intent);;findViewById<View>(R.id.splashOverlay).postDelayed({findViewById<View>(R.id.splashOverlay).animate().alpha(0f).setDuration(220).withEndAction{findViewById<View>(R.id.splashOverlay).visibility=View.GONE}.start()},900);findViewById<Button>(R.id.analyzeButton).setOnClickListener{analyzeUrl()};findViewById<Button>(R.id.discoverButton).setOnClickListener{discoverBatch()};findViewById<Button>(R.id.downloadButton).setOnClickListener{chooseDownloadProfile()};findViewById<Button>(R.id.playerButton).setOnClickListener{input.text.toString().trim().lineSequence().firstOrNull()?.takeIf{it.isNotBlank()}?.let{openPlayer(it)}}
        findViewById<Button>(R.id.libraryButton).setOnClickListener{startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="video/*";addCategory(Intent.CATEGORY_OPENABLE);putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)},PICK_MEDIA)}
        findViewById<Button>(R.id.folderButton).setOnClickListener{startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),PICK_FOLDER)};findViewById<Button>(R.id.playlistButton).setOnClickListener{startActivity(Intent(this,PlaylistActivity::class.java))};findViewById<Button>(R.id.libraryOpenButton).setOnClickListener{startActivity(Intent(this,LibraryActivity::class.java))};findViewById<Button>(R.id.settingsButton).setOnClickListener{startActivity(Intent(this,SettingsActivity::class.java))};findViewById<Button>(R.id.historyButton).setOnClickListener{startActivity(Intent(this,HistoryActivity::class.java))};findViewById<Button>(R.id.downloadsNavButton).setOnClickListener{findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.downloadList).smoothScrollToPosition(0)};findViewById<Button>(R.id.homeNavButton).setOnClickListener{findViewById<android.widget.ScrollView>(R.id.homeScroll).smoothScrollTo(0,0)};findViewById<Button>(R.id.libraryNavButton).setOnClickListener{startActivity(Intent(this,LibraryActivity::class.java))};findViewById<Button>(R.id.settingsNavButton).setOnClickListener{startActivity(Intent(this,SettingsActivity::class.java))};findViewById<View>(R.id.topSettingsButton).setOnClickListener{startActivity(Intent(this,SettingsActivity::class.java))};findViewById<View>(R.id.youtubeButton).setOnClickListener{input.setText("https://www.youtube.com/");input.requestFocus()};findViewById<View>(R.id.facebookButton).setOnClickListener{input.setText("https://www.facebook.com/");input.requestFocus()};findViewById<View>(R.id.instagramButton).setOnClickListener{input.setText("https://www.instagram.com/");input.requestFocus()};findViewById<View>(R.id.tiktokButton).setOnClickListener{input.setText("https://www.tiktok.com/");input.requestFocus()}
    }

    private fun analyzeUrl(){
        val url=input.text.toString().trim().lineSequence().firstOrNull{it.startsWith("http://")||it.startsWith("https://")}.orEmpty()
        if(url.isBlank()){Toast.makeText(this,"Enter a permitted media URL first",Toast.LENGTH_SHORT).show();return}
        chooseMediaTypeAndAnalyze(url, false)
    }

    /** Share-to-DVD flow: choose Video or Music first, then choose exposed quality/format. */
    private fun chooseMediaTypeAndAnalyze(url:String, fromShare:Boolean){
        val types=arrayOf("Video","Music / Audio")
        val title=if(fromShare) "Shared media detected" else "What do you want to download?"
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(if(fromShare) "Choose whether you want the video or music/audio from this public link." else "Choose Video for picture + sound, or Music / Audio for audio only.")
            .setSingleChoiceItems(types,0,null)
            .setPositiveButton("Continue"){dlg,_ ->
                val selected=(dlg as AlertDialog).listView.checkedItemPosition.coerceIn(0,1)
                analyzeUrlForType(url, selected==1)
            }
            .setNegativeButton("Cancel",null)
            .show()
    }

    private fun analyzeUrlForType(url:String, music:Boolean){
        val site=PublicMediaDiscovery.inspectUrl(url)
        Toast.makeText(this,"${site.name}: analyzing public streams…",Toast.LENGTH_SHORT).show()
        Thread {
            val result=StreamCatalog.inspect(this,url)
            runOnUiThread {
                result.onSuccess { allVariants ->
                    if(allVariants.isEmpty()){
                        Toast.makeText(this,"No downloadable public media variants found",Toast.LENGTH_LONG).show();return@runOnUiThread
                    }
                    val variants=if(music) allVariants.filter{it.kind=="audio"}.ifEmpty{allVariants} else allVariants.filter{it.kind=="video"}.ifEmpty{allVariants}
                    val labels=variants.map { v ->
                        buildString {
                            append(v.quality).append(" • ").append(v.format).append(" • ").append(if(v.kind=="audio")"Music" else "Video")
                            if(v.width>0&&v.height>0)append(" • ${v.width}x${v.height}")
                            if(v.fps>0)append(" • ${v.fps}fps")
                            if(v.bitrate>0)append(" • ${v.bitrate/1000}kbps")
                            if(v.videoCodec!="Auto")append(" • V:${v.videoCodec}")
                            if(v.audioCodec!="Auto")append(" • A:${v.audioCodec}")
                        }
                    }.toTypedArray()
                    val outputFormats=if(music) arrayOf("MP3","M4A","Opus","Auto") else arrayOf("MP4","WebM","MKV","Auto")
                    val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,0,24,0)}
                    root.addView(TextView(this).apply{text=if(music)"Music / Audio format" else "Video format";setPadding(0,8,0,2)})
                    val formatSpinner=Spinner(this).also{it.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,outputFormats);root.addView(it)}
                    root.addView(TextView(this).apply{text="Available public streams";setPadding(0,14,0,4)})
                    val selectedIndex=variants.indexOfFirst{if(music)it.kind=="audio" else it.kind=="video"}.coerceAtLeast(0)
                    val list=ListView(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_list_item_single_choice,labels);choiceMode=ListView.CHOICE_MODE_SINGLE;setItemChecked(selectedIndex,true);layoutParams=LinearLayout.LayoutParams(-1,0,1f)}
                    root.addView(list)
                    val dialog=AlertDialog.Builder(this)
                        .setTitle("${site.name} • ${if(music)"Music / Audio" else "Video"}")
                        .setView(root)
                        .setPositiveButton("Download",null)
                        .setNeutralButton("Best available",null)
                        .setNegativeButton("Cancel",null)
                        .create()
                    dialog.setOnShowListener {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val index=list.checkedItemPosition.coerceIn(0,variants.lastIndex)
                            queueSelectedVariant(url,variants[index],music,formatSpinner.selectedItem.toString())
                            dialog.dismiss()
                        }
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                            val profile=DownloadProfile(
                                mode=if(music)DownloadProfile.AUDIO else DownloadProfile.VIDEO,
                                format=if(music)"MP3" else "MP4",
                                audioOnly=music, videoOnly=!music
                            )
                            val best=StreamSelector.choose(allVariants,profile)?.variant
                            if(best==null){Toast.makeText(this,"No suitable ${if(music)"audio" else "video"} stream exposed",Toast.LENGTH_LONG).show();return@setOnShowListener}
                            queueSelectedVariant(url,best,music,if(music)"MP3" else "MP4")
                            dialog.dismiss()
                        }
                    }
                    dialog.show()
                }.onFailure { Toast.makeText(this,"Analysis failed: ${it.message ?: "source not supported"}",Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun queueSelectedVariant(url:String, variant:StreamCatalog.Variant, music:Boolean, requestedFormat:String){
        val profile=DownloadProfile(
            mode=if(music)DownloadProfile.AUDIO else DownloadProfile.VIDEO,
            quality=variant.quality,
            format=requestedFormat,
            fps=if(variant.fps>0)variant.fps.toString() else "Auto",
            videoCodec=variant.videoCodec,
            audioCodec=variant.audioCodec,
            videoBitrate=if(variant.bitrate>0)"${variant.bitrate/1000} kbps" else "Auto",
            audioBitrate=if(music&&variant.bitrate>0)"${variant.bitrate/1000} kbps" else "Auto",
            audioOnly=music,
            videoOnly=!music
        )
        val title=URLUtil.guessFileName(variant.url,null,null).ifBlank{if(music)"DVD_Audio" else "DVD_Video"}
        DownloadService.command(this,DownloadService.ACTION_ADD,url=url,title=title,profile=profile)
        refresh()
        Toast.makeText(this,"${if(music)"Music" else "Video"} added to download queue",Toast.LENGTH_SHORT).show()
    }

    private fun discoverBatch(){
        val url=input.text.toString().trim().lineSequence().firstOrNull{it.startsWith("http://")||it.startsWith("https://")}.orEmpty()
        if(url.isBlank()){Toast.makeText(this,"Enter a public page or playlist URL first",Toast.LENGTH_SHORT).show();return}
        Toast.makeText(this,"Discovering publicly exposed media…",Toast.LENGTH_SHORT).show()
        Thread {
            val site=PublicMediaDiscovery.inspectUrl(url)
            val result=PublicMediaDiscovery.discoverMany(this,url)
            runOnUiThread {
                result.onSuccess { entries ->
                    if(entries.isEmpty()){
                        val msg=if(site.publicProfile) "${site.name} profile detected, but this page does not expose downloadable public media URLs." else "No publicly exposed media items were found on this page."
                        Toast.makeText(this,msg,Toast.LENGTH_LONG).show();return@onSuccess
                    }
                    val labels=entries.mapIndexed { i,e -> "${i+1}. ${e.title} • ${e.format} • ${e.kind}" }.toTypedArray()
                    AlertDialog.Builder(this).setTitle("${site.name}: ${entries.size} public media item(s)")
                        .setMultiChoiceItems(labels, BooleanArray(entries.size){true}, null)
                        .setPositiveButton("Select quality / format") { dlg,_ ->
                            val list=dlg as AlertDialog
                            val checked=mutableListOf<Int>()
                            for(i in 0 until entries.size) if(list.listView.checkedItemPositions.get(i)) checked += i
                            if(checked.isEmpty()){Toast.makeText(this,"Select at least one item",Toast.LENGTH_SHORT).show();return@setPositiveButton}
                            chooseBatchProfile(entries, checked)
                        }.setNegativeButton("Cancel",null).show()
                }.onFailure { Toast.makeText(this,"Discovery failed: ${it.message ?: "page not accessible"}",Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun chooseBatchProfile(entries: List<PublicMediaDiscovery.MediaEntry>, indices: List<Int>){
        val qualities=arrayOf("Best available","8K","4K","2K","1080p","720p","480p","360p")
        val formats=arrayOf("Auto","MP4","WebM","M4A","MP3","Opus")
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,0,24,0)}
        root.addView(TextView(this).apply{text="Quality";setPadding(0,8,0,2)})
        val q=Spinner(this).also{it.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,qualities);root.addView(it)}
        root.addView(TextView(this).apply{text="Format";setPadding(0,8,0,2)})
        val f=Spinner(this).also{it.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,formats);root.addView(it)}
        AlertDialog.Builder(this).setTitle("Batch download (${indices.size} items)").setView(root)
            .setPositiveButton("Add to queue"){_,_->
                val profile=DownloadProfile(quality=q.selectedItem.toString(),format=f.selectedItem.toString())
                indices.forEach { i -> val e=entries[i]; DownloadService.command(this,DownloadService.ACTION_ADD,url=e.url,title=e.title.ifBlank{URLUtil.guessFileName(e.url,null,null)},profile=profile) }
                refresh();Toast.makeText(this,"${indices.size} public media item(s) added",Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel",null).show()
    }

    private fun chooseDownloadProfile(){
        val modes=arrayOf("Best available","Best compatibility","Data saver","Audio only","Video only");val qualities=arrayOf("Best available","8K","4K","2K","1080p","720p","480p","360p");val formats=arrayOf("Auto","MP4","WebM","MKV","M4A","MP3","Opus");val fps=arrayOf("Auto","24","30","60","120");val codecs=arrayOf("Auto","H.264","H.265/HEVC","VP9","AV1");val audioCodecs=arrayOf("Auto","AAC","Opus","MP3")
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,0,24,0)}
        fun spin(label:String,values:Array<String>):Spinner{root.addView(TextView(this).apply{text=label;setPadding(0,8,0,2)});return Spinner(this).also{it.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,values);root.addView(it)}}
        val mode=spin("Smart download mode",modes);val q=spin("Quality",qualities);val f=spin("Format",formats);val fp=spin("FPS",fps);val vc=spin("Video codec",codecs);val ac=spin("Audio codec",audioCodecs);val vb=spin("Video bitrate",arrayOf("Auto","500 kbps","1 Mbps","2 Mbps","4 Mbps","8 Mbps","12 Mbps"));val ab=spin("Audio bitrate",arrayOf("Auto","64 kbps","96 kbps","128 kbps","192 kbps","256 kbps","320 kbps"))
        AlertDialog.Builder(this).setTitle("Advanced stream selection").setView(root).setPositiveButton("Add to queue"){_,_->val m=when(mode.selectedItemPosition){1->DownloadProfile.COMPATIBILITY;2->DownloadProfile.DATA_SAVER;3->DownloadProfile.AUDIO;4->DownloadProfile.VIDEO;else->DownloadProfile.BEST};addBatch(input.text.toString(),DownloadProfile(m,q.selectedItem.toString(),f.selectedItem.toString(),fp.selectedItem.toString(),vc.selectedItem.toString(),ac.selectedItem.toString(),vb.selectedItem.toString(),ab.selectedItem.toString(),m==DownloadProfile.AUDIO,m==DownloadProfile.VIDEO))}.setNegativeButton("Cancel",null).show()
    }
    private fun addBatch(raw:String,profile:DownloadProfile){val urls=raw.lines().flatMap{it.split(',')}.map{it.trim()}.filter{it.startsWith("http://")||it.startsWith("https://")}.distinct();if(urls.isEmpty()){Toast.makeText(this,"Enter one or more permitted media URLs",Toast.LENGTH_SHORT).show();return};urls.forEach{url->DownloadService.command(this,DownloadService.ACTION_ADD,url=url,title=URLUtil.guessFileName(url,null,null),profile=profile)};input.text.clear();Toast.makeText(this,"${urls.size} download(s) added",Toast.LENGTH_SHORT).show();input.postDelayed({refresh()},500)}
    private fun handleIncomingIntent(i:Intent?){
        val shared=i?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val view=i?.data?.toString().orEmpty()
        val value=if(shared.isNotBlank())shared.trim() else view.trim()
        if(value.startsWith("http://")||value.startsWith("https://")){
            if(::input.isInitialized){
                input.setText(value)
                if(i?.action==Intent.ACTION_SEND){input.post{chooseMediaTypeAndAnalyze(value,true)}}
            }
        }
    }
    override fun onNewIntent(i:Intent){super.onNewIntent(i);setIntent(i);handleIncomingIntent(i)}
    override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null)return;if(r==PICK_MEDIA){val u=mutableListOf<Uri>();d.clipData?.let{for(i in 0 until it.itemCount)u+=it.getItemAt(i).uri};d.data?.let{if(u.isEmpty())u+=it};u.forEach{openPlayer(it.toString())}}else if(r==PICK_FOLDER){d.data?.let{uri->contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION);getSharedPreferences("folder",MODE_PRIVATE).edit().putString("download_tree",uri.toString()).apply();Toast.makeText(this,"Download folder selected",Toast.LENGTH_SHORT).show()}}}
    private fun refresh(){items.clear();items.addAll(store.load());adapter.notifyDataSetChanged()};override fun onResume(){super.onResume();if(::adapter.isInitialized)refresh()}
    override fun pause(id:Long)=DownloadService.command(this,DownloadService.ACTION_PAUSE,id);override fun resume(id:Long)=DownloadService.command(this,DownloadService.ACTION_RESUME,id);override fun cancel(id:Long)=DownloadService.command(this,DownloadService.ACTION_CANCEL,id);override fun retry(id:Long)=DownloadService.command(this,DownloadService.ACTION_RETRY,id)
    override fun play(item:DownloadItem){item.filePath?.let{openPlayer(it)}};override fun delete(item:DownloadItem){item.filePath?.let{if(it.startsWith("/"))File(it).delete()};store.remove(item.id);refresh()}
    override fun extractAudio(item:DownloadItem){val path=item.filePath?:return;AudioExtractor.extractAacToM4a(this,path).onSuccess{Toast.makeText(this,"M4A audio created: ${File(it).name}",Toast.LENGTH_LONG).show()}.onFailure{Toast.makeText(this,it.message?:"Audio extraction unavailable",Toast.LENGTH_LONG).show()}}
    override fun convert(item:DownloadItem){val source=item.filePath?:return;val formats=arrayOf("MP4 (H.264/AAC)","M4A (AAC audio)","MP3 (true conversion)");val heights=arrayOf("Original","1080p","720p","480p");AlertDialog.Builder(this).setTitle("Conversion format").setSingleChoiceItems(formats,0,null).setPositiveButton("Next"){dlg,_->val selected=(dlg as AlertDialog).listView.checkedItemPosition;AlertDialog.Builder(this).setTitle("Output resolution").setSingleChoiceItems(heights,0,null).setPositiveButton("Start"){d,_->val height=when((d as AlertDialog).listView.checkedItemPosition){1->1080;2->720;3->480;else->0};val ext=when(selected){1->"m4a";2->"mp3";else->"mp4"};val dir=MediaOrganizer.directory(this,"audio");val out=MediaOrganizer.unique(File(dir,MediaOrganizer.safeName(item.title.substringBeforeLast('.',item.title),ext)));ConversionService.start(this,source,out.absolutePath,when(selected){1->"M4A";2->"MP3";else->"MP4"},height,out.name, getSharedPreferences("dvd_settings",MODE_PRIVATE).getString("audio_bitrate","128 kbps") ?: "128 kbps")}.setNegativeButton("Cancel",null).show()}.setNegativeButton("Cancel",null).show()}
    override fun rename(item:DownloadItem){val edit=EditText(this).apply{setText(item.title);selectAll()};AlertDialog.Builder(this).setTitle("Rename media").setView(edit).setPositiveButton("Save"){_,_->val old=item.filePath?.takeIf{it.startsWith("/")}?.let{File(it)};val name=edit.text.toString().trim().ifBlank{item.title};if(old!=null&&old.exists()){val target=File(old.parentFile,name.replace(Regex("[\\\\/:*?\"<>|]"),"_"));if(old.renameTo(target))item.filePath=target.absolutePath};store.update(item);refresh()}.setNegativeButton("Delete"){_,_->delete(item)}.show()}
    private fun openPlayer(v:String)=startActivity(Intent(this,PlayerActivity::class.java).putExtra("url",v))
}
