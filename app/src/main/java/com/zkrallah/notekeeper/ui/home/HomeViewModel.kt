package com.zkrallah.notekeeper.ui.home

import androidx.lifecycle.ViewModel
import com.zkrallah.notekeeper.local.NoteDatabase

class HomeViewModel : ViewModel() {

    private val database = NoteDatabase.getInstance()

    val allNotes = database.noteDAO().getNotes()
}