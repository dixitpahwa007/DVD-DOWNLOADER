package com.dixit.video.downloader

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity:AppCompatActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState); val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;padding(16)}; root.addView(TextView(this).apply{text="Download History";textSize=22f}); val list=ListView(this); root.addView(list,LinearLayout.LayoutParams(-1,0,1f)); val entries=HistoryStore(this).load(); val names=entries.map{"${it.title}\n${it.status} • ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it.time))}\n${it.category}"}; list.adapter=ArrayAdapter(this,android.R.layout.simple_list_item_1,names); list.setOnItemClickListener{_,_,p,_->entries[p].path?.let{startActivity(Intent(this,PlayerActivity::class.java).putExtra("url",it))}}; root.addView(Button(this).apply{text="Clear history";setOnClickListener{HistoryStore(this@HistoryActivity).clear();recreate()}});setContentView(root)}
    private fun LinearLayout.padding(v:Int){setPadding(v,v,v,v)}
}
