package com.zkrallah.notekeeper.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zkrallah.notekeeper.R
import com.zkrallah.notekeeper.local.entities.Note

class SyncNoteAdapter(private val list: List<Note>) : RecyclerView.Adapter<SyncNoteAdapter.ViewHolder>() {

    var toBeSynced = mutableSetOf<Note>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
            .inflate(R.layout.sync_note_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.titleTV.text = list[position].title
        holder.bodyTV.text = list[position].body
        holder.dateTV.text = list[position].date
        holder.itemView.setOnClickListener {
            if (toBeSynced.contains(list[position])){
                toBeSynced.remove(list[position])
                holder.check.visibility = View.INVISIBLE
            } else{
                toBeSynced.add(list[position])
                holder.check.visibility = View.VISIBLE
            }
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val titleTV: TextView = itemView.findViewById(R.id.title)
        val bodyTV: TextView = itemView.findViewById(R.id.body)
        val dateTV: TextView = itemView.findViewById(R.id.date)
        val check: ImageView = itemView.findViewById(R.id.check)
    }
}