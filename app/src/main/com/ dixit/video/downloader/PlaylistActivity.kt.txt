package com.dixit.video.downloader

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class PlaylistActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("dvd_playlist", MODE_PRIVATE) }
    private lateinit var files: List<File>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,16,16,16)}
        val play=Button(this).apply{text="Play playlist"}
        val list=ListView(this)
        val dir=getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
        files=dir?.listFiles()?.filter{it.isFile&&!it.name.endsWith(".part")}.orEmpty().sortedByDescending{it.lastModified()}
        val selected=prefs.getStringSet("items", emptySet())!!.toMutableSet()
        list.choiceMode=ListView.CHOICE_MODE_MULTIPLE
        list.adapter=ArrayAdapter(this,android.R.layout.simple_list_item_multiple_choice,files.map{it.name})
        files.forEachIndexed{i,f->if(selected.contains(f.absolutePath))list.setItemChecked(i,true)}
        play.setOnClickListener{
            val chosen=files.filterIndexed{i,_->list.isItemChecked(i)}
            if(chosen.isEmpty()){Toast.makeText(this,"Select media first",Toast.LENGTH_SHORT).show();return@setOnClickListener}
            prefs.edit().putStringSet("items",chosen.map{it.absolutePath}.toSet()).apply()
            startActivity(Intent(this,PlayerActivity::class.java).putExtra("playlist",chosen.map{it.absolutePath}.toTypedArray()).putExtra("url",chosen.first().absolutePath))
        }
        root.addView(play);root.addView(list,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
    }
}
