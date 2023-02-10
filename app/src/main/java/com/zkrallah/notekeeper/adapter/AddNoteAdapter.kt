package com.zkrallah.notekeeper.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.zkrallah.notekeeper.R

class AddNoteAdapter(private val list: List<String>?) : RecyclerView.Adapter<AddNoteAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
            .inflate(R.layout.image_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.imageHolder.setImageURI(Uri.parse((list?.get(position) ?: R.drawable.ic_baseline_add_photo_alternate_24) as String?))
    }

    override fun getItemCount(): Int {
        return list?.size ?: 0
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageHolder: ImageView = itemView.findViewById(R.id.image_holder)
    }
}