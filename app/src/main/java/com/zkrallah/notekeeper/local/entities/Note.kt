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

    constructor() : this("", "", "","",null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Note

        if (title == other.title && body == other.body)return true

        return false
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + body.hashCode()
        return result
    }

}
