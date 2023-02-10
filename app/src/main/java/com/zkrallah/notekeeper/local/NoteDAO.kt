package com.zkrallah.notekeeper.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.zkrallah.notekeeper.local.entities.Note

@Dao
interface NoteDAO {

    @Query("select * from notes_table")
    fun getNotes(): LiveData<List<Note>>

    @Query("select * from notes_table WHERE author = :authorId")
    fun getUserNotes(authorId: String): List<Note>

    @Insert
    suspend fun insert(note: Note?)

    @Query("DELETE FROM notes_table WHERE id = :noteId")
    suspend fun deleteNote(noteId: Long): Int

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateNote(note: Note)

    @Query("SELECT * FROM notes_table WHERE id=:noteId")
    suspend fun getNoteById(noteId: Long): Note?
}