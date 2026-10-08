package com.dixit.video.downloader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DownloadAdapter(private val items: List<DownloadItem>, private val actions: Actions) : RecyclerView.Adapter<DownloadAdapter.VH>() {
    interface Actions { fun pause(id:Long); fun resume(id:Long); fun cancel(id:Long); fun retry(id:Long); fun play(item:DownloadItem); fun delete(item:DownloadItem); fun rename(item:DownloadItem); fun extractAudio(item:DownloadItem); fun convert(item:DownloadItem) }
    class VH(v:View):RecyclerView.ViewHolder(v){
        val title:TextView=v.findViewById(R.id.title); val details:TextView=v.findViewById(R.id.details); val status:TextView=v.findViewById(R.id.status)
        val pause:Button=v.findViewById(R.id.pause); val resume:Button=v.findViewById(R.id.resume); val cancel:Button=v.findViewById(R.id.cancel); val retry:Button=v.findViewById(R.id.retry); val play:Button=v.findViewById(R.id.play); val manage:Button=v.findViewById(R.id.manage); val audio:Button=v.findViewById(R.id.audio); val convert:Button=v.findViewById(R.id.convert)
    }
    override fun onCreateViewHolder(p:ViewGroup,t:Int)=VH(LayoutInflater.from(p.context).inflate(R.layout.item_download,p,false))
    override fun onBindViewHolder(h:VH,pos:Int){val x=items[pos];h.title.text=x.title;h.details.text="${x.mode} • ${x.requestedQuality} • ${x.requestedFormat} • ${if(x.category=="audio")"Music" else "Movies"}";h.status.text=if(x.totalBytes>0)"${x.status} • ${x.progress}%" else x.status
        h.pause.visibility=if(x.status=="Downloading")View.VISIBLE else View.GONE;h.resume.visibility=if(x.status=="Paused")View.VISIBLE else View.GONE;h.retry.visibility=if(x.status.startsWith("Failed"))View.VISIBLE else View.GONE;h.cancel.visibility=if(x.status in listOf("Downloading","Queued","Paused"))View.VISIBLE else View.GONE;h.play.visibility=if(x.status=="Completed")View.VISIBLE else View.GONE;h.manage.visibility=if(x.status=="Completed")View.VISIBLE else View.GONE;h.audio.visibility=if(x.status=="Completed")View.VISIBLE else View.GONE;h.convert.visibility=if(x.status=="Completed")View.VISIBLE else View.GONE
        h.pause.setOnClickListener{actions.pause(x.id)};h.resume.setOnClickListener{actions.resume(x.id)};h.retry.setOnClickListener{actions.retry(x.id)};h.cancel.setOnClickListener{actions.cancel(x.id)};h.play.setOnClickListener{actions.play(x)};h.manage.setOnClickListener{actions.rename(x)};h.audio.setOnClickListener{actions.extractAudio(x)};h.convert.setOnClickListener{actions.convert(x)}
    }
    override fun getItemCount()=items.size
}
