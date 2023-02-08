package com.zkrallah.notekeeper.ui.home

import androidx.lifecycle.*
import com.zkrallah.notekeeper.local.NoteDatabase
import com.zkrallah.notekeeper.local.entities.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val database = NoteDatabase.getInstance()

    private val _userNotes = MutableLiveData<List<Note>>()
    val userNotes = _userNotes

    fun getUserNotes(authorId: String){
        viewModelScope.launch (Dispatchers.IO){
            _userNotes.postValue(database.noteDAO().getUserNotes(authorId))
        }
    }
}