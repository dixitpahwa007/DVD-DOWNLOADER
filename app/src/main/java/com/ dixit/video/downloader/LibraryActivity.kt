package com.dixit.video.downloader

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class LibraryActivity : AppCompatActivity() {
    data class Media(val id:Long,val uri:String,val name:String,val duration:Long,val dateAdded:Long,val folder:String,val isVideo:Boolean)
    private val all=mutableListOf<Media>()
    private lateinit var adapter:ArrayAdapter<String>
    private lateinit var list:ListView
    private var query=""
    private var filter=0
    private val requestCode=701
    private val favorites by lazy { getSharedPreferences("favorites",MODE_PRIVATE) }
    private val watched by lazy { getSharedPreferences("watched",MODE_PRIVATE) }
    private val positions by lazy { getSharedPreferences("playback_positions",MODE_PRIVATE) }

    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);buildUi(); if(hasPermission())scan() else requestPermission()}

    private fun hasPermission():Boolean = if(Build.VERSION.SDK_INT>=33) {
        ContextCompat.checkSelfPermission(this,Manifest.permission.READ_MEDIA_VIDEO)==PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this,Manifest.permission.READ_MEDIA_AUDIO)==PackageManager.PERMISSION_GRANTED
    } else ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED

    private fun requestPermission(){val p=if(Build.VERSION.SDK_INT>=33)arrayOf(Manifest.permission.READ_MEDIA_VIDEO,Manifest.permission.READ_MEDIA_AUDIO) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE);ActivityCompat.requestPermissions(this,p,requestCode)}
    override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==requestCode)scan()}

    private fun buildUi(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,16,16,16)}
        root.addView(TextView(this).apply{text="Media Library";textSize=22f})
        val search=EditText(this).apply{hint="Search media, folder or file name"};root.addView(search)
        val controls=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        val sort=Spinner(this);sort.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("Newest","Name","Longest","Folder"));controls.addView(sort,LinearLayout.LayoutParams(0,-2,1f))
        val filterSpin=Spinner(this);filterSpin.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("All","Favorites","Unwatched","Watched","Continue watching"));controls.addView(filterSpin,LinearLayout.LayoutParams(0,-2,1f));root.addView(controls)
        list=ListView(this);root.addView(list,LinearLayout.LayoutParams(-1,0,1f));adapter=ArrayAdapter(this,android.R.layout.simple_list_item_1,mutableListOf());list.adapter=adapter
        search.addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){};override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){query=s?.toString().orEmpty();render(sort.selectedItemPosition)};override fun afterTextChanged(e:android.text.Editable?) {}})
        sort.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,pos:Int,id:Long){render(pos)}}
        filterSpin.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,pos:Int,id:Long){filter=pos;render(sort.selectedItemPosition)}}
        list.setOnItemClickListener{_,_,position,_->val item=filtered()[position];startActivity(Intent(this,PlayerActivity::class.java).putExtra("url",item.uri))}
        list.setOnItemLongClickListener{_,_,position,_->showItemMenu(filtered()[position],sort.selectedItemPosition);true}
        setContentView(root)
    }

    private fun showItemMenu(item:Media,sort:Int){
        val fav=favorites.getBoolean(item.uri,false);val isWatched=watched.getBoolean(item.uri,false)
        val choices= mutableListOf("Play","Share",if(fav)"Remove favorite" else "Favorite",if(isWatched)"Mark unwatched" else "Mark watched","Rename","Delete")
        AlertDialog.Builder(this).setTitle(item.name).setItems(choices.toTypedArray()){_,w->when(w){0->startActivity(Intent(this,PlayerActivity::class.java).putExtra("url",item.uri));1->startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,item.uri),"Share media"));2->favorites.edit().putBoolean(item.uri,!fav).apply();3->watched.edit().putBoolean(item.uri,!isWatched).apply();4->renameItem(item);5->deleteItem(item)};render(sort)}.show()
    }

    private fun renameItem(item:Media){
        val input=EditText(this).apply{setText(item.name);selectAll()}
        AlertDialog.Builder(this).setTitle("Rename media").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Rename"){_,_->
            val newName=input.text.toString().trim();if(newName.isBlank())return@setPositiveButton
            runCatching{
                val values=android.content.ContentValues().apply{put(MediaStore.MediaColumns.DISPLAY_NAME,newName)}
                contentResolver.update(android.net.Uri.parse(item.uri),values,null,null)
                scan()
            }.onFailure{Toast.makeText(this,"Rename failed: ${it.message}",Toast.LENGTH_LONG).show()}
        }.show()
    }

    private fun deleteItem(item:Media){
        AlertDialog.Builder(this).setTitle("Delete media?").setMessage(item.name).setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->
            runCatching{contentResolver.delete(android.net.Uri.parse(item.uri),null,null);favorites.edit().remove(item.uri).apply();watched.edit().remove(item.uri).apply();positions.edit().remove(item.uri.hashCode().toString()).apply();scan()}.onFailure{Toast.makeText(this,"Delete failed: ${it.message}",Toast.LENGTH_LONG).show()}
        }.show()
    }

    private fun scan(){all.clear();queryCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,true);queryCollection(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,false);render(0)}

    private fun queryCollection(uri:android.net.Uri,video:Boolean){
        val projection=if(video)arrayOf(MediaStore.Video.Media._ID,MediaStore.Video.Media.DISPLAY_NAME,MediaStore.Video.Media.DURATION,MediaStore.Video.Media.DATE_ADDED,MediaStore.Video.Media.RELATIVE_PATH) else arrayOf(MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DISPLAY_NAME,MediaStore.Audio.Media.DURATION,MediaStore.Audio.Media.DATE_ADDED,MediaStore.Audio.Media.RELATIVE_PATH)
        runCatching{contentResolver.query(uri,projection,null,null,"${if(video)MediaStore.Video.Media.DATE_ADDED else MediaStore.Audio.Media.DATE_ADDED} DESC")?.use{c->while(c.moveToNext()){val id=c.getLong(0);val name=c.getString(1).orEmpty();val dur=c.getLong(2);val added=c.getLong(3);val folder=runCatching{c.getString(4).orEmpty().trimEnd('/')}.getOrDefault("");all.add(Media(id,ContentUris.withAppendedId(uri,id).toString(),name,dur,added,folder,video))}}}
    }

    private fun filtered():List<Media>{
        val q=query.trim().lowercase(Locale.getDefault())
        return all.filter{m->
            val matches=q.isBlank()||m.name.lowercase(Locale.getDefault()).contains(q)||m.folder.lowercase(Locale.getDefault()).contains(q)
            val f=when(filter){1->favorites.getBoolean(m.uri,false);2->!watched.getBoolean(m.uri,false);3->watched.getBoolean(m.uri,false);4->positions.getLong(m.uri.hashCode().toString(),0L)>5000L;else->true}
            matches&&f
        }
    }

    private fun render(sort:Int){
        val x=filtered().sortedWith(when(sort){1->compareBy{it.name.lowercase(Locale.getDefault())};2->compareByDescending{it.duration};3->compareBy{it.folder.lowercase(Locale.getDefault())}.thenBy{it.name.lowercase(Locale.getDefault())};else->compareByDescending{it.dateAdded}})
        adapter.clear();adapter.addAll(x.map{m->val fav=favorites.getBoolean(m.uri,false);val w=watched.getBoolean(m.uri,false);val pos=positions.getLong(m.uri.hashCode().toString(),0L);val progress=if(m.duration>0&&pos>5000)" • ${(pos*100/m.duration).coerceIn(0,100)}%" else "";"${if(m.isVideo)"VIDEO" else "AUDIO"} • ${m.name}${if(m.folder.isNotBlank())" • ${m.folder}" else ""}${if(fav)" • ★" else ""}${if(w)" • Watched" else ""}$progress"});adapter.notifyDataSetChanged()
    }
}
