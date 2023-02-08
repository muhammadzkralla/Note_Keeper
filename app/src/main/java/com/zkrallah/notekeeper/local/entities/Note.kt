package com.zkrallah.notekeeper.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_table")
data class Note(
    val title: String,
    val body: String,
    val date: String,
    val author: String,
    val images: List<String>?
    ){
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L
}
