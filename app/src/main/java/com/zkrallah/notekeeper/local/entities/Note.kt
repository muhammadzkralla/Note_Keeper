package com.zkrallah.notekeeper.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_table")
data class Note(
    var title: String,
    var body: String,
    val date: String,
    val author: String,
    var images: List<String>?
    ){
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L
}
