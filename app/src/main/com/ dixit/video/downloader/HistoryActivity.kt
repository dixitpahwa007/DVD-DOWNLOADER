package com.dixit.video.downloader

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity:AppCompatActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(DvdUi.bg);setPadding(DvdUi.dp(this@HistoryActivity,12),0,DvdUi.dp(this@HistoryActivity,12),DvdUi.dp(this@HistoryActivity,12))};val head=DvdUi.row(this);head.addView(DvdUi.text(this,"‹",34f).apply{gravity=Gravity.CENTER;setOnClickListener{finish()}},LinearLayout.LayoutParams(DvdUi.dp(this,42),DvdUi.dp(this,54)));head.addView(LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;addView(DvdUi.text(this@HistoryActivity,"Playback History",20f,DvdUi.text,true));addView(DvdUi.text(this@HistoryActivity,"Continue watching • recently played",11f,DvdUi.secondary))},LinearLayout.LayoutParams(0,-2,1f));root.addView(head);val entries=HistoryStore(this).load();val list=ListView(this).apply{divider=null;dividerHeight=DvdUi.dp(this@HistoryActivity,8);setPadding(0,DvdUi.dp(this@HistoryActivity,8),0,DvdUi.dp(this@HistoryActivity,8))};val names=entries.map{"▶  ${it.title}\n     ${it.status} • ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it.time))}\n     ${it.category}"};list.adapter=ArrayAdapter(this,android.R.layout.simple_list_item_1,names);root.addView(list,LinearLayout.LayoutParams(-1,0,1f));val actions=DvdUi.row(this);val clear=Button(this).apply{text="Clear history";setTextColor(DvdUi.text);backgroundTintList=android.content.res.ColorStateList.valueOf(DvdUi.card2);setOnClickListener{HistoryStore(this@HistoryActivity).clear();recreate()}};actions.addView(clear,LinearLayout.LayoutParams(0,DvdUi.dp(this,50),1f));root.addView(actions);list.setOnItemClickListener{_,_,p,_->entries[p].path?.let{startActivity(Intent(this,PlayerActivity::class.java).putExtra("url",it))}};setContentView(root)}
}
